/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *          http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.surisoft.capi.gateway.scheduler;

import io.surisoft.capi.gateway.cache.RunningApiManager;
import io.surisoft.capi.gateway.cache.ThrottlingManager;
import io.surisoft.capi.gateway.schema.RunningApi;
import io.surisoft.capi.gateway.schema.SuspensionType;
import io.surisoft.capi.gateway.schema.ThrottlingPolicy;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class ThrottlingInspector {

    @Autowired
    RunningApiManager runningApiManager;

    @Autowired
    ThrottlingManager throttlingManager;

    @Scheduled(fixedRateString = "${api.gateway.api.throttling.inspector.period}")
    public void count() {

        Date executionTime = Calendar.getInstance().getTime();
        IMap<String, ThrottlingPolicy> throttlingPolicies = throttlingManager.getThrottlingPolicies();
        Iterator<String> throttlingIterator = throttlingPolicies.keySet().iterator();

        try {
            while(throttlingIterator.hasNext()) {
                String policyID = throttlingIterator.next();
                ThrottlingPolicy throttlingPolicy = throttlingPolicies.get(policyID);

                if(throttlingPolicy.getThrottlingExpiration() == null) {
                    Calendar throttlingExpirationTime = Calendar.getInstance();
                    throttlingExpirationTime.add(Calendar.MILLISECOND, throttlingPolicy.getPeriodForMaxCalls());
                    throttlingPolicy.setThrottlingExpiration(throttlingExpirationTime.getTime());
                    throttlingManager.saveThrottlingPolicy(policyID, throttlingPolicy);
                }

                if(throttlingPolicy.isApplyPerPath()) {
                    List<String> routes = throttlingPolicy.getRouteID();
                    for(String routeID : routes) {
                        RunningApi runningApi = runningApiManager.getRunningApi(routeID);
                        if(!runningApi.isRemoved()) {
                            if(throttlingPolicy.getTotalCalls() >= throttlingPolicy.getMaxCallsAllowed() && !runningApi.isRemoved()) {
                                Calendar throttlingExpirationTime = Calendar.getInstance();
                                throttlingExpirationTime.add(Calendar.MILLISECOND, throttlingPolicy.getPeriodForMaxCalls());

                                throttlingPolicy.setThrottlingExpiration(throttlingExpirationTime.getTime());
                                throttlingManager.saveThrottlingPolicy(policyID, throttlingPolicy);
                                runningApi.setRemoved(true);
                                runningApi.setDisabled(true);
                                runningApi.setSuspensionType(SuspensionType.THROTTLING);
                                runningApi.setSuspensionMessage("Your route was suspended because its configured to accept: " + throttlingPolicy.getMaxCallsAllowed() + " calls during a period of " + throttlingPolicy.getPeriodForMaxCalls());
                                runningApiManager.saveRunningApi(runningApi);
                                //camelUtils.suspendRoute(runningApi);
                                //camelUtils.addSuspendedRoute(runningApi);
                            } else {
                                if(throttlingPolicy.getThrottlingExpiration() != null && throttlingPolicy.getThrottlingExpiration().before(executionTime)) {
                                    throttlingPolicy.setTotalCalls(0);

                                    Calendar throttlingExpirationTime = Calendar.getInstance();
                                    throttlingExpirationTime.add(Calendar.MILLISECOND, throttlingPolicy.getPeriodForMaxCalls());
                                    throttlingPolicy.setThrottlingExpiration(throttlingExpirationTime.getTime());
                                    throttlingManager.saveThrottlingPolicy(policyID, throttlingPolicy);
                                }
                            }
                        } else if(throttlingPolicy.getThrottlingExpiration().before(executionTime) && runningApi.getSuspensionType().equals(SuspensionType.THROTTLING)) {
                            throttlingPolicy.setThrottlingExpiration(null);
                            throttlingPolicy.setTotalCalls(0);
                            throttlingManager.saveThrottlingPolicy(policyID, throttlingPolicy);
                            runningApi.setSuspensionMessage(null);
                            runningApi.setSuspensionType(null);
                            runningApi.setDisabled(false);
                            runningApi.setRemoved(false);
                            runningApiManager.saveRunningApi(runningApi);
                            //Remove suspended route
                            //camelUtils.suspendRoute(runningApi);
                            //camelUtils.addActiveRoute(runningApi);
                        }
                    }

                } else {
                    //apply per api
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
