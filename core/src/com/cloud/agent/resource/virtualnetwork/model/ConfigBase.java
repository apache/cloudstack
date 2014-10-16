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

public abstract class ConfigBase {
    public final static String UNKNOWN = "unknown";
    public final static String VM_DHCP = "dhcpentry";
    public final static String IP_ASSOCIATION = "ips";
    public final static String GUEST_NETWORK = "guestnetwork";
    public static final String NETWORK_ACL = "networkacl";
    public static final String VM_METADATA = "vmdata";
    public static final String VM_PASSWORD = "vmpassword";
    public static final String FORWARDING_RULES = "forwardrules";
    public static final String FIREWALL_RULES = "firewallrules";
    public static final String VPN_USER_LIST = "vpnuserlist";
    public static final String STATICNAT_RULES = "staticnatrules";
    public static final String IP_ALIAS_CONFIG = "ipaliases";
    public static final String SITE2SITEVPN = "site2sitevpn";
    public static final String STATIC_ROUTES = "staticroutes";
    public static final String REMOTEACCESSVPN = "remoteaccessvpn";
    public static final String MONITORSERVICE = "monitorservice";
    public static final String DHCP_CONFIG = "dhcpconfig";
    public static final String LOAD_BALANCER = "loadbalancer";

    private String type = UNKNOWN;

    private ConfigBase() {
        // Empty constructor for (de)serialization
    }

    protected ConfigBase(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

}
