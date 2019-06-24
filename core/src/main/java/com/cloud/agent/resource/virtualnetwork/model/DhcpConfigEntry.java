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

public class DhcpConfigEntry {
    private String routerIpAddress;
    private String gateway;
    private String netmask;
    private String firstIpOfSubnet;

    public DhcpConfigEntry() {
        // Empty for (de)serialization
    }

    public DhcpConfigEntry(String routerIpAddress, String gateway, String netmask, String firstIpOfSubnet) {
        super();
        this.routerIpAddress = routerIpAddress;
        this.gateway = gateway;
        this.netmask = netmask;
        this.firstIpOfSubnet = firstIpOfSubnet;
    }

    public String getRouterIpAddress() {
        return routerIpAddress;
    }

    public void setRouterIpAddress(String routerIpAddress) {
        this.routerIpAddress = routerIpAddress;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getFirstIpOfSubnet() {
        return firstIpOfSubnet;
    }

    public void setFirstIpOfSubnet(String firstIpOfSubnet) {
        this.firstIpOfSubnet = firstIpOfSubnet;
    }

}
