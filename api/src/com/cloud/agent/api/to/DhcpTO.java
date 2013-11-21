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
package com.cloud.agent.api.to;

public class DhcpTO {
    String routerIp;
    String gateway;
    String netmask;
    String startIpOfSubnet;

    public DhcpTO(String routerIp, String gateway, String netmask, String startIpOfSubnet) {
        this.routerIp = routerIp;
        this.startIpOfSubnet = startIpOfSubnet;
        this.gateway = gateway;
        this.netmask = netmask;

    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setStartIpOfSubnet(String ipOfSubNet) {
        startIpOfSubnet = ipOfSubNet;
    }

    public String getRouterIp() {
        return routerIp;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getStartIpOfSubnet() {
        return startIpOfSubnet;
    }

}
