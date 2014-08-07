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

package com.cloud.agent.resource.virtualnetwork;

public class VRScripts {
    protected final static String CONFIG_PERSIST_LOCATION = "/etc/cloudstack/";
    protected final static String IP_ASSOCIATION_CONFIG = "ip_associations.json";
    protected final static String GUEST_NETWORK_CONFIG = "guest_network.json";
    protected final static String NETWORK_ACL_CONFIG = "network_acl.json";
    protected final static String VM_METADATA_CONFIG = "vm_metadata.json";
    protected final static String DHCP_ENTRY_CONFIG = "vm_dhcp_entry.json";

    protected final static String CONFIG_CACHE_LOCATION = "/var/cache/cloud/";
    protected final static int DEFAULT_EXECUTEINVR_TIMEOUT = 120; //Seconds

    // New scripts for use with chef
    protected static final String UPDATE_CONFIG = "update_config.py";

    protected static final String S2SVPN_CHECK = "checkbatchs2svpn.sh";
    protected static final String S2SVPN_IPSEC = "ipsectunnel.sh";
    protected static final String DHCP = "edithosts.sh";
    protected static final String DNSMASQ_CONFIG = "dnsmasq.sh";
    protected static final String FIREWALL_EGRESS = "firewall_egress.sh";
    protected static final String FIREWALL_INGRESS = "firewall_ingress.sh";
    protected static final String FIREWALL_NAT = "firewall_nat.sh";
    protected static final String IPALIAS_CREATE = "createIpAlias.sh";
    protected static final String IPALIAS_DELETE = "deleteIpAlias.sh";
    protected static final String IPASSOC = "ipassoc.sh";
    protected static final String LB = "loadbalancer.sh";
    protected static final String MONITOR_SERVICE = "monitor_service.sh";
    protected static final String ROUTER_ALERTS = "getRouterAlerts.sh";
    protected static final String PASSWORD = "savepassword.sh";
    protected static final String RVR_CHECK = "checkrouter.sh";
    protected static final String RVR_BUMPUP_PRI = "bumpup_priority.sh";
    protected static final String VMDATA = "vmdata.py";
    protected static final String VERSION = "get_template_version.sh";
    protected static final String VPC_ACL = "vpc_acl.sh";
    protected static final String VPC_GUEST_NETWORK = "vpc_guestnw.sh";
    protected static final String VPC_IPASSOC = "vpc_ipassoc.sh";
    protected static final String VPC_LB = "vpc_loadbalancer.sh";
    protected static final String VPC_PRIVATEGW = "vpc_privateGateway.sh";
    protected static final String VPC_PRIVATEGW_ACL = "vpc_privategw_acl.sh";
    protected static final String VPC_PORTFORWARDING = "vpc_portforwarding.sh";
    protected static final String VPC_SOURCE_NAT = "vpc_snat.sh";
    protected static final String VPC_STATIC_NAT = "vpc_staticnat.sh";
    protected static final String VPC_STATIC_ROUTE = "vpc_staticroute.sh";
    protected static final String VPN_L2TP = "vpn_l2tp.sh";

    protected static final String VR_CFG = "vr_cfg.sh";
}
