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

public class UpdateLoadBalancerServiceInstanceCommand extends TungstenCommand {
    private String publicNetworkUuid;
    private String floatingPoolName;
    private String floatingIpName;

    public UpdateLoadBalancerServiceInstanceCommand(final String publicNetworkUuid, final String floatingPoolName,
        final String floatingIpName) {
        this.publicNetworkUuid = publicNetworkUuid;
        this.floatingPoolName = floatingPoolName;
        this.floatingIpName = floatingIpName;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public void setPublicNetworkUuid(final String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }

    public String getFloatingPoolName() {
        return floatingPoolName;
    }

    public void setFloatingPoolName(final String floatingPoolName) {
        this.floatingPoolName = floatingPoolName;
    }

    public String getFloatingIpName() {
        return floatingIpName;
    }

    public void setFloatingIpName(final String floatingIpName) {
        this.floatingIpName = floatingIpName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateLoadBalancerServiceInstanceCommand that = (UpdateLoadBalancerServiceInstanceCommand) o;
        return Objects.equals(publicNetworkUuid, that.publicNetworkUuid) && Objects.equals(floatingPoolName, that.floatingPoolName) && Objects.equals(floatingIpName, that.floatingIpName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), publicNetworkUuid, floatingPoolName, floatingIpName);
    }
}
