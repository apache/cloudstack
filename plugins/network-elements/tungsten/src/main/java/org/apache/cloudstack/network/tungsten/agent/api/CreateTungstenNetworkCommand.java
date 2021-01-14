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

import java.util.List;

public class CreateTungstenNetworkCommand extends TungstenCommand {
    private final String uuid;
    private final String name;
    private final String parent;
    private final boolean routerExternal;
    private final boolean shared;
    private final String ipPrefix;
    private final int ipPrefixLen;
    private final String gateway;
    private final boolean dhcpEnable;
    private final List<String> dnsServers;
    private final String allocationStart;
    private final String allocationEnd;
    private final boolean ipFromStart;
    private final boolean isManagementNetwork;

    public CreateTungstenNetworkCommand(final String uuid, final String name, final String parent,
        final boolean routerExternal, final boolean shared, final String ipPrefix, final int ipPrefixLen,
        final String gateway, final boolean dhcpEnable, final List<String> dnsServers, final String allocationStart,
        final String allocationEnd, final boolean ipFromStart, final boolean isManagementNetwork) {
        this.uuid = uuid;
        this.name = name;
        this.parent = parent;
        this.routerExternal = routerExternal;
        this.shared = shared;
        this.ipPrefix = ipPrefix;
        this.ipPrefixLen = ipPrefixLen;
        this.gateway = gateway;
        this.dhcpEnable = dhcpEnable;
        this.dnsServers = dnsServers;
        this.allocationStart = allocationStart;
        this.allocationEnd = allocationEnd;
        this.ipFromStart = ipFromStart;
        this.isManagementNetwork = isManagementNetwork;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
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

    public List<String> getDnsServers() {
        return dnsServers;
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
}
