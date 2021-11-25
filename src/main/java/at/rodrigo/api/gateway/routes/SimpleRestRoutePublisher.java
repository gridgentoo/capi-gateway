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

package at.rodrigo.api.gateway.routes;

import at.rodrigo.api.gateway.cache.ThrottlingManager;
import at.rodrigo.api.gateway.repository.ApiRepository;
import at.rodrigo.api.gateway.schema.Api;
import at.rodrigo.api.gateway.schema.Path;
import at.rodrigo.api.gateway.utils.CamelUtils;
import at.rodrigo.api.gateway.utils.GrafanaUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.RestOperationParamDefinition;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

//@Component
@Slf4j
public class SimpleRestRoutePublisher extends RouteBuilder {

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private CamelUtils camelUtils;

    @Autowired
    private GrafanaUtils grafanaUtils;

    @Autowired
    private ThrottlingManager throttlingManager;

    @Override
    public void configure() {

        log.info("Starting configuration of Simple Routes");

        List<Api> apiList = apiRepository.findAllBySwagger(false);
        for(Api api : apiList) {
            try {
                addRoutes(api);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }


    }

    void addRoutes(Api api) throws  Exception {
        for(Path path : api.getPaths()) {
            if(!path.getPath().equals("/error")) {
                RestOperationParamDefinition restParamDefinition = new RestOperationParamDefinition();
                List<String> paramList = camelUtils.evaluatePath(path.getPath());

                String routeID = camelUtils.normalizeRouteId(api, path);
                path.setRouteID(routeID);
                RouteDefinition routeDefinition;

                switch(path.getVerb()) {
                    case GET:
                        routeDefinition = rest().get("/" + api.getContext() + path.getPath()).route();
                        break;
                    case POST:
                        routeDefinition = rest().post("/" + api.getContext() + path.getPath()).route();
                        break;
                    case PUT:
                        routeDefinition = rest().put("/" + api.getContext() + path.getPath()).route();
                        break;
                    case DELETE:
                        routeDefinition = rest().delete("/" + api.getContext() + path.getPath()).route();
                        break;
                    default:
                        throw new Exception("No verb available");
                }
                camelUtils.buildOnExceptionDefinition(routeDefinition, api.isZipkinTraceIdVisible(), api.isInternalExceptionMessageVisible(), api.isInternalExceptionVisible(), routeID);
                if(paramList.isEmpty()) {
                    camelUtils.buildRoute(routeDefinition, routeID, api, path, false);
                } else {
                    for(String param : paramList) {
                        restParamDefinition.name(param)
                                .type(RestParamType.path)
                                .dataType("String");
                    }
                    camelUtils.buildRoute(routeDefinition, routeID, api, path, true);
                }
            }
        }
        throttlingManager.applyThrottling(api);
    }
}