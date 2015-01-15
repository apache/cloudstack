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

import java.util.HashMap;

import com.cloud.agent.api.Command;

public abstract class NetworkElementCommand extends Command {
    HashMap<String, String> accessDetails = new HashMap<String, String>(0);

    public static final String ACCOUNT_ID = "account.id";
    public static final String GUEST_NETWORK_CIDR = "guest.network.cidr";
    public static final String GUEST_NETWORK_GATEWAY = "guest.network.gateway";
    public static final String GUEST_VLAN_TAG = "guest.vlan.tag";
    public static final String ROUTER_NAME = "router.name";
    public static final String ROUTER_IP = "router.ip";
    public static final String ROUTER_GUEST_IP = "router.guest.ip";
    public static final String ZONE_NETWORK_TYPE = "zone.network.type";
    public static final String GUEST_BRIDGE = "guest.bridge";
    public static final String VPC_PRIVATE_GATEWAY = "vpc.gateway.private";
    public static final String FIREWALL_EGRESS_DEFAULT = "firewall.egress.default";
    public static final String ROUTER_MONITORING_ENABLE = "router.monitor.enable";

    private String routerAccessIp;

    protected NetworkElementCommand() {
        super();
    }

    public void setAccessDetail(final String name, final String value) {
        accessDetails.put(name, value);
    }

    public String getAccessDetail(final String name) {
        return accessDetails.get(name);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getRouterAccessIp() {
        return routerAccessIp;
    }

    public void setRouterAccessIp(final String routerAccessIp) {
        this.routerAccessIp = routerAccessIp;
    }

    public int getAnswersCount() {
        return 1;
    }

    public boolean isQuery() {
        return false;
    }
}
