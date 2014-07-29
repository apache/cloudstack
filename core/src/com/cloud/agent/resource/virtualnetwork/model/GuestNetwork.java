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

public class GuestNetwork extends ConfigBase {
    private boolean add;
    private String macAddress;
    private String device;
    private String routerGuestIp;
    private String routerGuestNetmask;
    private String routerGuestGateway;
    private String cidr;
    private String dns;
    private String domainName;

    public GuestNetwork() {
        // Empty constructor for (de)serialization
        setType("guestnetwork");
    }

    public GuestNetwork(boolean add, String macAddress, String device, String routerGuestIp, String routerGuestNetmask, String routerGuestGateway, String cidr, String dns,
            String domainName) {
        setType("guestnetwork");
        this.add = add;
        this.macAddress = macAddress;
        this.device = device;
        this.routerGuestIp = routerGuestIp;
        this.routerGuestNetmask = routerGuestNetmask;
        this.routerGuestGateway = routerGuestGateway;
        this.cidr = cidr;
        this.dns = dns;
        this.domainName = domainName;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getRouterGuestIp() {
        return routerGuestIp;
    }

    public void setRouterGuestIp(String routerGuestIp) {
        this.routerGuestIp = routerGuestIp;
    }

    public String getRouterGuestNetmask() {
        return routerGuestNetmask;
    }

    public void setRouterGuestNetmask(String routerGuestNetmask) {
        this.routerGuestNetmask = routerGuestNetmask;
    }

    public String getRouterGuestGateway() {
        return routerGuestGateway;
    }

    public void setRouterGuestGateway(String routerGuestGateway) {
        this.routerGuestGateway = routerGuestGateway;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

}
