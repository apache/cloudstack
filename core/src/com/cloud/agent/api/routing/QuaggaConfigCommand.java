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

import com.cloud.network.vpc.OSPFZoneConfig;
import com.cloud.utils.net.cidr.CIDR;

public class QuaggaConfigCommand extends NetworkElementCommand {

    private String routerPublicIp;
    private String routerName;
    private String vpnCidr;
    private CIDR[] tierCidrs;
    private OSPFZoneConfig zoneConfig;

    public QuaggaConfigCommand(final String routerPublicIp, final String routerName, final String vpnCidr, final CIDR[] tierCidrs, final OSPFZoneConfig qzc) {
        this.routerPublicIp = routerPublicIp;
        this.routerName = routerName;
        this.vpnCidr = vpnCidr;
        this.tierCidrs = tierCidrs;
        this.zoneConfig = qzc;
    }

    public OSPFZoneConfig getZoneConfig() {
        return zoneConfig;
    }

    public void setZoneConfig(OSPFZoneConfig zoneConfig) {
        this.zoneConfig = zoneConfig;
    }

    public String getRouterPublicIp() {
        return routerPublicIp;
    }

    public void setRouterPublicIp(String routerPublicIp) {
        this.routerPublicIp = routerPublicIp;
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

    public CIDR[] getTierCidrs() {
        return tierCidrs;
    }

    public void setTierCidrs(CIDR[] tierCidrs) {
        this.tierCidrs = tierCidrs;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
