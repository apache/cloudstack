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

import org.joda.time.Duration;

public class VRScripts {
    public final static String CONFIG_PERSIST_LOCATION = "/var/cache/cloud/";
    public final static String IP_ASSOCIATION_CONFIG = "ip_associations.json";
    public final static String GUEST_NETWORK_CONFIG = "guest_network.json";
    public final static String NETWORK_ACL_CONFIG = "network_acl.json";
    public final static String VM_METADATA_CONFIG = "vm_metadata.json";
    public final static String VM_DHCP_CONFIG = "vm_dhcp_entry.json";
    public final static String VM_PASSWORD_CONFIG = "vm_password.json";
    public static final String FORWARDING_RULES_CONFIG = "forwarding_rules.json";
    public static final String FIREWALL_RULES_CONFIG = "firewall_rules.json";
    public static final String IPV6_FIREWALL_RULES_CONFIG = "ipv6_firewall_rules.json";
    public static final String VPN_USER_LIST_CONFIG = "vpn_user_list.json";
    public static final String STATICNAT_RULES_CONFIG = "staticnat_rules.json";
    public static final String SITE_2_SITE_VPN_CONFIG = "site_2_site_vpn.json";
    public static final String STATIC_ROUTES_CONFIG = "static_routes.json";
    public static final String REMOTE_ACCESS_VPN_CONFIG = "remote_access_vpn.json";
    public static final String MONITOR_SERVICE_CONFIG = "monitor_service.json";
    public static final String DHCP_CONFIG = "dhcp.json";
    public static final String IP_ALIAS_CONFIG = "ip_aliases.json";
    public static final String LOAD_BALANCER_CONFIG = "load_balancer.json";

    public static final String SYSTEM_VM_PATCHED = "patched.sh";
    public final static String CONFIG_CACHE_LOCATION = "/var/cache/cloud/";
    public final static Duration VR_SCRIPT_EXEC_TIMEOUT = Duration.standardMinutes(10);
    public final static Duration CONNECTION_TIMEOUT = Duration.standardMinutes(1);

    // New scripts for use with chef
    public static final String UPDATE_CONFIG = "update_config.py";
    public static final String CONFIGURE = "configure.py";


    // Script still in use - mostly by HyperV
    public static final String S2SVPN_CHECK = "checkbatchs2svpn.sh";
    public static final String S2SVPN_IPSEC = "ipsectunnel.sh";
    public static final String DHCP = "edithosts.sh";
    public static final String DNSMASQ_CONFIG = "dnsmasq.sh";
    public static final String IPASSOC = "ipassoc.sh";
    public static final String LB = "loadbalancer.sh";
    public static final String MONITOR_SERVICE = "monitor_service.sh";
    public static final String PASSWORD = "savepassword.sh";
    public static final String ROUTER_ALERTS = "getRouterAlerts.sh";
    public static final String RVR_CHECK = "checkrouter.sh";
    public static final String VMDATA = "vmdata.py";
    public static final String RVR_BUMPUP_PRI = "bumpup_priority.sh";
    public static final String VERSION = "get_template_version.sh";
    public static final String VPC_SOURCE_NAT = "vpc_snat.sh";
    public static final String VPC_STATIC_ROUTE = "vpc_staticroute.sh";
    public static final String VPN_L2TP = "vpn_l2tp.sh";
    public static final String UPDATE_HOST_PASSWD = "update_host_passwd.sh";
    public static final String ROUTER_MONITOR_RESULTS = "getRouterMonitorResults.sh";

    public static final String VR_CFG = "vr_cfg.sh";

    public static final String DIAGNOSTICS = "diagnostics.py";
    public static final String RETRIEVE_DIAGNOSTICS = "get_diagnostics_files.py";
    public static final String VR_FILE_CLEANUP = "cleanup.sh";

    public static final String VR_UPDATE_INTERFACE_CONFIG = "update_interface_config.sh";

    public static final String ROUTER_FILESYSTEM_WRITABLE_CHECK = "filesystem_writable_check.py";
}
