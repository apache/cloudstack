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

public class AddTungstenNetworkSubnetCommand extends TungstenCommand {
    private final String networkUuid;
    private final String ipPrefix;
    private final int ipPrefixLen;
    private final String gateway;
    private final boolean dhcpEnable;
    private final String dnsServer;
    private final String allocationStart;
    private final String allocationEnd;
    private final boolean ipFromStart;
    private final String subnetName;

    public AddTungstenNetworkSubnetCommand(final String networkUuid, final String ipPrefix, final int ipPrefixLen,
        final String gateway, final boolean dhcpEnable, final String dnsServer, final String allocationStart,
        final String allocationEnd, final boolean ipFromStart, final String subnetName) {
        this.networkUuid = networkUuid;
        this.ipPrefix = ipPrefix;
        this.ipPrefixLen = ipPrefixLen;
        this.gateway = gateway;
        this.dhcpEnable = dhcpEnable;
        this.dnsServer = dnsServer;
        this.allocationStart = allocationStart;
        this.allocationEnd = allocationEnd;
        this.ipFromStart = ipFromStart;
        this.subnetName = subnetName;
    }

    public String getNetworkUuid() {
        return networkUuid;
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

    public String getSubnetName() {
        return subnetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddTungstenNetworkSubnetCommand that = (AddTungstenNetworkSubnetCommand) o;
        return ipPrefixLen == that.ipPrefixLen && dhcpEnable == that.dhcpEnable && ipFromStart == that.ipFromStart && Objects.equals(networkUuid, that.networkUuid) && Objects.equals(ipPrefix, that.ipPrefix) && Objects.equals(gateway, that.gateway) && Objects.equals(dnsServer, that.dnsServer) && Objects.equals(allocationStart, that.allocationStart) && Objects.equals(allocationEnd, that.allocationEnd) && Objects.equals(subnetName, that.subnetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkUuid, ipPrefix, ipPrefixLen, gateway, dhcpEnable, dnsServer, allocationStart, allocationEnd, ipFromStart, subnetName);
    }
}
