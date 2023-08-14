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

public class DeleteTungstenLoadBalancerCommand extends TungstenCommand {
    private final String projectFqn;
    private final String publicNetworkUuid;
    private final String loadBalancerName;
    private final String loadBalancerHealthMonitorName;
    private final String loadBalancerVmiName;
    private final String fipName;
    private final String fiName;

    public DeleteTungstenLoadBalancerCommand(final String projectFqn, final String publicNetworkUuid,
        final String loadBalancerName, final String loadBalancerHealthMonitorName, final String loadBalancerVmiName,
        final String fipName, final String fiName) {
        this.projectFqn = projectFqn;
        this.publicNetworkUuid = publicNetworkUuid;
        this.loadBalancerName = loadBalancerName;
        this.loadBalancerHealthMonitorName = loadBalancerHealthMonitorName;
        this.loadBalancerVmiName = loadBalancerVmiName;
        this.fipName = fipName;
        this.fiName = fiName;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getLoadBalancerHealthMonitorName() {
        return loadBalancerHealthMonitorName;
    }

    public String getLoadBalancerVmiName() {
        return loadBalancerVmiName;
    }

    public String getFipName() {
        return fipName;
    }

    public String getFiName() {
        return fiName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DeleteTungstenLoadBalancerCommand that = (DeleteTungstenLoadBalancerCommand) o;
        return Objects.equals(projectFqn, that.projectFqn) && Objects.equals(publicNetworkUuid, that.publicNetworkUuid) && Objects.equals(loadBalancerName, that.loadBalancerName) && Objects.equals(loadBalancerHealthMonitorName, that.loadBalancerHealthMonitorName) && Objects.equals(loadBalancerVmiName, that.loadBalancerVmiName) && Objects.equals(fipName, that.fipName) && Objects.equals(fiName, that.fiName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, publicNetworkUuid, loadBalancerName, loadBalancerHealthMonitorName, loadBalancerVmiName, fipName, fiName);
    }
}
