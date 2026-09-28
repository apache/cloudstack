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
package org.apache.cloudstack.vm;

import java.util.List;

/**
 * A source VM NIC discovered during VMware CBT preflight, so NIC-to-network
 * mappings can be validated when the migration is started instead of failing
 * at import time, after replication has already run.
 */
public class VmwareCbtPreflightNicInfo {

    private final String sourceNicId;
    private final String adapterType;
    private final String macAddress;
    private final Integer vlan;

    // Guest network facts from VMware Tools (available while the source runs): the IPv4
    // addresses in CIDR form, the default gateway of this NIC's subnet, and the guest's DNS
    // servers. Null when Tools is not running or reports nothing for this NIC. Used to
    // preserve a static source IP across the migration when it fits the target network.
    private List<String> ipv4Cidrs;
    private String ipv4Gateway;
    private List<String> dnsServers;

    public VmwareCbtPreflightNicInfo(String sourceNicId, String adapterType, String macAddress, Integer vlan) {
        this.sourceNicId = sourceNicId;
        this.adapterType = adapterType;
        this.macAddress = macAddress;
        this.vlan = vlan;
    }

    public String getSourceNicId() {
        return sourceNicId;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public Integer getVlan() {
        return vlan;
    }

    public List<String> getIpv4Cidrs() {
        return ipv4Cidrs;
    }

    public void setIpv4Cidrs(List<String> ipv4Cidrs) {
        this.ipv4Cidrs = ipv4Cidrs;
    }

    public String getIpv4Gateway() {
        return ipv4Gateway;
    }

    public void setIpv4Gateway(String ipv4Gateway) {
        this.ipv4Gateway = ipv4Gateway;
    }

    public List<String> getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(List<String> dnsServers) {
        this.dnsServers = dnsServers;
    }
}
