package com.cloud.agent.resource.virtualnetwork;

public class VRScripts {
    protected final static String CONFIG_PERSIST_LOCATION = "/etc/cloudstack/";
    protected final static String IP_ASSOCIATION_CONFIG = "ip_associations.json";
    protected final static String CONFIG_CACHE_LOCATION = "/var/cache/cloud/";
    protected final static int DEFAULT_EXECUTEINVR_TIMEOUT = 120; //Seconds

    protected static final String S2SVPN_CHECK = "checkbatchs2svpn.sh";
    protected static final String S2SVPN_IPSEC = "ipsectunnel.sh";
    protected static final String DHCP = "edithosts.sh";
    protected static final String DNSMASQ_CONFIG = "dnsmasq.sh";
    protected static final String FIREWALL_EGRESS = "firewall_egress.sh";
    protected static final String FIREWALL_INGRESS = "firewall_ingress.sh";
    protected static final String FIREWALL_NAT = "firewall_nat.sh";
    protected static final String IPALIAS_CREATE = "createipAlias.sh";
    protected static final String IPALIAS_DELETE = "deleteipAlias.sh";
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
