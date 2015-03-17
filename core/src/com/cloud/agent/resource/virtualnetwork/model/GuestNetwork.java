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
        super(ConfigBase.GUEST_NETWORK);
    }

    public GuestNetwork(final boolean add, final String macAddress, final String device, final String routerGuestIp, final String routerGuestNetmask, final String routerGuestGateway,
            final String cidr, final String dns, final String domainName) {
        super(ConfigBase.GUEST_NETWORK);
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

    public void setAdd(final boolean add) {
        this.add = add;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(final String device) {
        this.device = device;
    }

    public String getRouterGuestIp() {
        return routerGuestIp;
    }

    public void setRouterGuestIp(final String routerGuestIp) {
        this.routerGuestIp = routerGuestIp;
    }

    public String getRouterGuestNetmask() {
        return routerGuestNetmask;
    }

    public void setRouterGuestNetmask(final String routerGuestNetmask) {
        this.routerGuestNetmask = routerGuestNetmask;
    }

    public String getRouterGuestGateway() {
        return routerGuestGateway;
    }

    public void setRouterGuestGateway(final String routerGuestGateway) {
        this.routerGuestGateway = routerGuestGateway;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(final String cidr) {
        this.cidr = cidr;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(final String dns) {
        this.dns = dns;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(final String domainName) {
        this.domainName = domainName;
    }
}