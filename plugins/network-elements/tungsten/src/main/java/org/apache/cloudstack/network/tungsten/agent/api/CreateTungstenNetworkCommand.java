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

public class CreateTungstenNetworkCommand extends TungstenCommand {
    private final String uuid;
    private final String name;
    private final String displayName;
    private final String projectFqn;
    private final boolean routerExternal;
    private final boolean shared;
    private final String ipPrefix;
    private final int ipPrefixLen;
    private final String gateway;
    private final boolean dhcpEnable;
    private final String dnsServer;
    private final String allocationStart;
    private final String allocationEnd;
    private final boolean ipFromStart;
    private final boolean isManagementNetwork;
    private final String subnetName;

    public CreateTungstenNetworkCommand(final String uuid, final String name, final String displayName,
        final String projectFqn, final boolean routerExternal, final boolean shared, final String ipPrefix,
        final int ipPrefixLen, final String gateway, final boolean dhcpEnable, final String dnsServer,
        final String allocationStart, final String allocationEnd, final boolean ipFromStart,
        final boolean isManagementNetwork, final String subnetName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.projectFqn = projectFqn;
        this.routerExternal = routerExternal;
        this.shared = shared;
        this.ipPrefix = ipPrefix;
        this.ipPrefixLen = ipPrefixLen;
        this.gateway = gateway;
        this.dhcpEnable = dhcpEnable;
        this.dnsServer = dnsServer;
        this.allocationStart = allocationStart;
        this.allocationEnd = allocationEnd;
        this.ipFromStart = ipFromStart;
        this.isManagementNetwork = isManagementNetwork;
        this.subnetName = subnetName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public boolean isRouterExternal() {
        return routerExternal;
    }

    public boolean isShared() {
        return shared;
    }

    public String getIpPrefix() {
        return ipPrefix;
    }

    public int getIpPrefixLen() {
        return ipPrefixLen;
    }

    public String getGateway() {
        return gateway;
    }

    public boolean isDhcpEnable() {
        return dhcpEnable;
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public String getAllocationStart() {
        return allocationStart;
    }

    public String getAllocationEnd() {
        return allocationEnd;
    }

    public boolean isIpFromStart() {
        return ipFromStart;
    }

    public boolean isManagementNetwork() {
        return isManagementNetwork;
    }

    public String getSubnetName() {
        return subnetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenNetworkCommand that = (CreateTungstenNetworkCommand) o;
        return routerExternal == that.routerExternal && shared == that.shared && ipPrefixLen == that.ipPrefixLen && dhcpEnable == that.dhcpEnable && ipFromStart == that.ipFromStart && isManagementNetwork == that.isManagementNetwork && Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name) && Objects.equals(displayName, that.displayName) && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(ipPrefix, that.ipPrefix) && Objects.equals(gateway, that.gateway) && Objects.equals(dnsServer, that.dnsServer) && Objects.equals(allocationStart, that.allocationStart) && Objects.equals(allocationEnd, that.allocationEnd) && Objects.equals(subnetName, that.subnetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, name, displayName, projectFqn, routerExternal, shared, ipPrefix, ipPrefixLen, gateway, dhcpEnable, dnsServer, allocationStart, allocationEnd, ipFromStart, isManagementNetwork, subnetName);
    }
}
