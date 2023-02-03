// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;

public class UpdateTungstenLoadBalancerHealthMonitorCommand extends TungstenCommand {
    private final String projectFqn;
    private final String healthMonitorName;
    private final String type;
    private final int retry;
    private final int timeout;
    private final int interval;
    private final String httpMethod;
    private final String expectedCode;
    private final String urlPath;

    public UpdateTungstenLoadBalancerHealthMonitorCommand(final String projectFqn, final String healthMonitorName,
        final String type, final int retry, final int timeout, final int interval, final String httpMethod,
        final String expectedCode, final String urlPath) {
        this.projectFqn = projectFqn;
        this.healthMonitorName = healthMonitorName;
        this.type = type;
        this.retry = retry;
        this.timeout = timeout;
        this.interval = interval;
        this.httpMethod = httpMethod;
        this.expectedCode = expectedCode;
        this.urlPath = urlPath;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getHealthMonitorName() {
        return healthMonitorName;
    }

    public String getType() {
        return type;
    }

    public int getRetry() {
        return retry;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getInterval() {
        return interval;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getExpectedCode() {
        return expectedCode;
    }

    public String getUrlPath() {
        return urlPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateTungstenLoadBalancerHealthMonitorCommand that = (UpdateTungstenLoadBalancerHealthMonitorCommand) o;
        return retry == that.retry && timeout == that.timeout && interval == that.interval && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(healthMonitorName, that.healthMonitorName) && Objects.equals(type, that.type) && Objects.equals(httpMethod, that.httpMethod) && Objects.equals(expectedCode, that.expectedCode) && Objects.equals(urlPath, that.urlPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, healthMonitorName, type, retry, timeout, interval, httpMethod, expectedCode, urlPath);
    }
}
