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

package com.cloud.agent.api.routing;

public class QuaggaConfigCommand extends NetworkElementCommand {

    private String routerIp;
    private String routerName;
    private String vpnCidr;
    private String[] tierCidrs;

    public QuaggaConfigCommand(String routerIp, String routerName, String vpnCidr, String[] tierCidrs) {
        this.routerIp = routerIp;
        this.routerName = routerName;
        this.vpnCidr = vpnCidr;
        this.tierCidrs = tierCidrs;
    }

    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    public String getRouterName() {
        return routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public String getVpnCidr() {
        return vpnCidr;
    }

    public void setVpnCidr(String vpnCidr) {
        this.vpnCidr = vpnCidr;
    }

    public String[] getTierCidrs() {
        return tierCidrs;
    }

    public void setTierCidrs(String[] tierCidrs) {
        this.tierCidrs = tierCidrs;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
