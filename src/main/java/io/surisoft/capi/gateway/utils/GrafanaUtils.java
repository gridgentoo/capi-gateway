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

package io.surisoft.capi.gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

//@Component
//@Slf4j
public class GrafanaUtils {

    /*@Value("${api.gateway.grafana.create.panels}")
    private boolean autoCreatePanels;

    @Value("${api.gateway.grafana.endpoint}")
    private String grafanaEndpoint;

    @Autowired
    private RestTemplate restTemplate;

    public void addToGrafana(Api api) {
        if(autoCreatePanels) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Api> request = new HttpEntity<>(api, headers);
                restTemplate.exchange(grafanaEndpoint, HttpMethod.POST, request, String.class);
            } catch(Exception e) {
                log.info(e.getMessage(), e);
            }
        }
    }*/
}
