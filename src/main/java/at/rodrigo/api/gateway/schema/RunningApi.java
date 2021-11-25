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
package at.rodrigo.api.gateway.schema;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RunningApi implements Serializable {
    private String id;
    private String context;
    private String path;
    private List<String> endpoints;
    private EndpointType endpointType;
    private Verb verb;
    private String jwsEndpoint;
    private List<String> audience;
    private String routeId;
    private String zipkinServiceName;
    private String prometheusMetricName;
    private int failedCalls;
    private int maxAllowedFailedCalls;
    private boolean unblockAfter;
    private int unblockAfterMinutes;
    private boolean disabled;
    private boolean removed;
    private int countBlockChecks;
    private boolean blockIfInError;
    private boolean secured;
    private boolean trafficInspectorEnabled;
    private SuspensionType suspensionType;
    private String suspensionMessage;
    private boolean zipkinTraceIdVisible;
    private boolean internalExceptionMessageVisible;
    private boolean internalExceptionVisible;
    private int connectTimeout;
    private int socketTimeout;
    private String clientID;
}
