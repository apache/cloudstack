//
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
//

package com.cloud.agent.resource.virtualnetwork.model;

public class VmDhcpConfig extends ConfigBase {
    private String hostName;
    private String macAddress;
    private String ipv4Address;
    private String ipv6Address;
    private String ipv6Duid;
    private String dnsAddresses;
    private String defaultGateway;
    private String staticRoutes;
    private boolean defaultEntry;

    // Indicate if the entry should be removed when set to true
    private boolean remove;

    public VmDhcpConfig() {
        super(VM_DHCP);
    }

    public VmDhcpConfig(String hostName, String macAddress, String ipv4Address, String ipv6Address, String ipv6Duid, String dnsAddresses, String defaultGateway,
            String staticRoutes, boolean defaultEntry, boolean remove) {
        super(VM_DHCP);
        this.hostName = hostName;
        this.macAddress = macAddress;
        this.ipv4Address = ipv4Address;
        this.ipv6Address = ipv6Address;
        this.ipv6Duid = ipv6Duid;
        this.dnsAddresses = dnsAddresses;
        this.defaultGateway = defaultGateway;
        this.staticRoutes = staticRoutes;
        this.defaultEntry = defaultEntry;
        this.remove = remove;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getIpv4Address() {
        return ipv4Address;
    }

    public void setIpv4Address(String ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public String getIpv6Duid() {
        return ipv6Duid;
    }

    public void setIpv6Duid(String ipv6Duid) {
        this.ipv6Duid = ipv6Duid;
    }

    public String getDnsAddresses() {
        return dnsAddresses;
    }

    public void setDnsAddresses(String dnsAddresses) {
        this.dnsAddresses = dnsAddresses;
    }

    public String getDefaultGateway() {
        return defaultGateway;
    }

    public void setDefaultGateway(String defaultGateway) {
        this.defaultGateway = defaultGateway;
    }

    public String getStaticRoutes() {
        return staticRoutes;
    }

    public void setStaticRoutes(String staticRoutes) {
        this.staticRoutes = staticRoutes;
    }

    public boolean isDefaultEntry() {
        return defaultEntry;
    }

    public void setDefaultEntry(boolean defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

}
