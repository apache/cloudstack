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
package com.cloud.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.annotation.Annotation;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.usage.Usage;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.host.Host;
import com.cloud.network.GuestVlan;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.as.AutoScaleCounter;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.Condition;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.server.ResourceTag;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Snapshot;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VirtualMachine;

public class EventTypes {

    //map of Event and corresponding entity for which Event is applicable
    private static Map<String, Object> entityEventDetails = null;

    // VM Events
    public static final String EVENT_VM_CREATE = "VM.CREATE";
    public static final String EVENT_VM_DESTROY = "VM.DESTROY";
    public static final String EVENT_VM_START = "VM.START";
    public static final String EVENT_VM_STOP = "VM.STOP";
    public static final String EVENT_VM_REBOOT = "VM.REBOOT";
    public static final String EVENT_VM_UPDATE = "VM.UPDATE";
    public static final String EVENT_VM_UPGRADE = "VM.UPGRADE";
    public static final String EVENT_VM_DYNAMIC_SCALE = "VM.DYNAMIC.SCALE";
    public static final String EVENT_VM_RESETPASSWORD = "VM.RESETPASSWORD";
    public static final String EVENT_VM_RESETSSHKEY = "VM.RESETSSHKEY";

    public static final String EVENT_VM_RESETUSERDATA = "VM.RESETUSERDATA";
    public static final String EVENT_VM_MIGRATE = "VM.MIGRATE";
    public static final String EVENT_VM_MOVE = "VM.MOVE";
    public static final String EVENT_VM_RESTORE = "VM.RESTORE";
    public static final String EVENT_VM_EXPUNGE = "VM.EXPUNGE";
    public static final String EVENT_VM_IMPORT = "VM.IMPORT";
    public static final String EVENT_VM_UNMANAGE = "VM.UNMANAGE";
    public static final String EVENT_VM_RECOVER = "VM.RECOVER";

    // Domain Router
    public static final String EVENT_ROUTER_CREATE = "ROUTER.CREATE";
    public static final String EVENT_ROUTER_DESTROY = "ROUTER.DESTROY";
    public static final String EVENT_ROUTER_START = "ROUTER.START";
    public static final String EVENT_ROUTER_STOP = "ROUTER.STOP";
    public static final String EVENT_ROUTER_REBOOT = "ROUTER.REBOOT";
    public static final String EVENT_ROUTER_HA = "ROUTER.HA";
    public static final String EVENT_ROUTER_UPGRADE = "ROUTER.UPGRADE";
    public static final String EVENT_ROUTER_DIAGNOSTICS = "ROUTER.DIAGNOSTICS";
    public static final String EVENT_ROUTER_HEALTH_CHECKS = "ROUTER.HEALTH.CHECKS";

    // Console proxy
    public static final String EVENT_PROXY_CREATE = "PROXY.CREATE";
    public static final String EVENT_PROXY_DESTROY = "PROXY.DESTROY";
    public static final String EVENT_PROXY_START = "PROXY.START";
    public static final String EVENT_PROXY_STOP = "PROXY.STOP";
    public static final String EVENT_PROXY_REBOOT = "PROXY.REBOOT";
    public static final String EVENT_PROXY_HA = "PROXY.HA";
    public static final String EVENT_PROXY_DIAGNOSTICS = "PROXY.DIAGNOSTICS";

    // VNC Console Events
    public static final String EVENT_VNC_CONNECT = "VNC.CONNECT";
    public static final String EVENT_VNC_DISCONNECT = "VNC.DISCONNECT";

    // Network Events
    public static final String EVENT_NET_IP_ASSIGN = "NET.IPASSIGN";
    public static final String EVENT_NET_IP_RELEASE = "NET.IPRELEASE";
    public static final String EVENT_NET_IP_RESERVE = "NET.IPRESERVE";
    public static final String EVENT_NET_IP_UPDATE = "NET.IPUPDATE";
    public static final String EVENT_PORTABLE_IP_ASSIGN = "PORTABLE.IPASSIGN";
    public static final String EVENT_PORTABLE_IP_RELEASE = "PORTABLE.IPRELEASE";
    public static final String EVENT_NET_RULE_ADD = "NET.RULEADD";
    public static final String EVENT_NET_RULE_DELETE = "NET.RULEDELETE";
    public static final String EVENT_NET_RULE_MODIFY = "NET.RULEMODIFY";
    public static final String EVENT_NETWORK_CREATE = "NETWORK.CREATE";
    public static final String EVENT_NETWORK_DELETE = "NETWORK.DELETE";
    public static final String EVENT_NETWORK_UPDATE = "NETWORK.UPDATE";
    public static final String EVENT_NETWORK_MIGRATE = "NETWORK.MIGRATE";
    public static final String EVENT_FIREWALL_OPEN = "FIREWALL.OPEN";
    public static final String EVENT_FIREWALL_CLOSE = "FIREWALL.CLOSE";
    public static final String EVENT_FIREWALL_UPDATE = "FIREWALL.UPDATE";

    public static final String EVENT_NET_IP6_ASSIGN = "NET.IP6ASSIGN";
    public static final String EVENT_NET_IP6_RELEASE = "NET.IP6RELEASE";
    public static final String EVENT_NET_IP6_UPDATE = "NET.IP6UPDATE";

    public static final String EVENT_FIREWALL_EGRESS_OPEN = "FIREWALL.EGRESS.OPEN";
    public static final String EVENT_FIREWALL_EGRESS_CLOSE = "FIREWALL.EGRESS.CLOSE";
    public static final String EVENT_FIREWALL_EGRESS_UPDATE = "FIREWALL.EGRESS.UPDATE";

    // Tungsten-Fabric
    public static final String EVENT_TUNGSTEN_ADD_POLICY_RULE = "TUNGSTEN.ADD.POLICY.RULE";
    public static final String EVENT_TUNGSTEN_APPLY_POLICY = "TUNGSTEN.APPLY.POLICY";
    public static final String EVENT_TUNGSTEN_APPLY_TAG = "TUNGSTEN.APPLY.TAG";
    public static final String EVENT_TUNGSTEN_CREATE_POLICY = "TUNGSTEN.CREATE.POLICY";
    public static final String EVENT_TUNGSTEN_CREATE_TAG = "TUNGSTEN.CREATE.TAG";
    public static final String EVENT_TUNGSTEN_CREATE_TAGTYPE = "TUNGSTEN.CREATE.TAGTYPE";
    public static final String EVENT_TUNGSTEN_CREATE_ADDRESS_GROUP = "TUNGSTEN.CREATE.ADDRESS.GROUP";
    public static final String EVENT_TUNGSTEN_CREATE_SERVICE_GROUP = "TUNGSTEN.CREATE.SERVICE.GROUP";
    public static final String EVENT_TUNGSTEN_CREATE_APPLICATION_POLICY_SET = "TUNGSTEN.CREATE.APS";
    public static final String EVENT_TUNGSTEN_CREATE_FIREWALL_POLICY = "TUNGSTEN.CREATE.FIREWALL.POLICY";
    public static final String EVENT_TUNGSTEN_CREATE_FIREWALL_RULE = "TUNGSTEN.CREATE.FIREWALL.RULE";
    public static final String EVENT_TUNGSTEN_DELETE_POLICY = "TUNGSTEN.DELETE.POLICY";
    public static final String EVENT_TUNGSTEN_DELETE_TAG = "TUNGSTEN.DELETE.TAG";
    public static final String EVENT_TUNGSTEN_DELETE_TAGTYPE = "TUNGSTEN.DELETE.TAGTYPE";
    public static final String EVENT_TUNGSTEN_DELETE_ADDRESS_GROUP = "TUNGSTEN.DELETE.ADDRESS.GROUP";
    public static final String EVENT_TUNGSTEN_DELETE_APPLICATION_POLICY_SET = "TUNGSTEN.DELETE.APS";
    public static final String EVENT_TUNGSTEN_DELETE_FIREWALL_POLICY = "TUNGSTEN.DELETE.FIREWALL.POLICY";
    public static final String EVENT_TUNGSTEN_DELETE_FIREWALL_RULE = "TUNGSTEN.DELETE.FIREWALL.RULE";
    public static final String EVENT_TUNGSTEN_DELETE_SERVICE_GROUP = "TUNGSTEN.DELETE.SERVICE.GROUP";
    public static final String EVENT_TUNGSTEN_REMOVE_POLICY = "TUNGSTEN.REMOVE.POLICY";
    public static final String EVENT_TUNGSTEN_REMOVE_TAG = "TUNGSTEN.REMOVE.TAG";
    public static final String EVENT_TUNGSTEN_REMOVE_POLICY_RULE = "TUNGSTEN.REMOVE.POLICY.RULE";
    public static final String EVENT_TUNGSTEN_CREATE_LOGICAL_ROUTER = "TUNGSTEN.CREATE.LOGICAL.ROUTER";
    public static final String EVENT_TUNGSTEN_ADD_NETWORK_GATEWAY_TO_LOGICAL_ROUTER = "TUNGSTEN.ADD.NETWORK.GW.TO.LG";
    public static final String EVENT_TUNGSTEN_REMOVE_NETWORK_GATEWAY_FROM_LOGICAL_ROUTER = "TUNGSTEN.RM.NETWORK.GW.FROM.LG";
    public static final String EVENT_TUNGSTEN_DELETE_LOGICAL_ROUTER = "TUNGSTEN.DELETE.LOGICAL.ROUTER";
    public static final String EVENT_TUNGSTEN_UPDATE_LB_HEALTH_MONITOR = "TUNGSTEN.UPDATE.LB.HM";

    //NIC Events
    public static final String EVENT_NIC_CREATE = "NIC.CREATE";
    public static final String EVENT_NIC_DELETE = "NIC.DELETE";
    public static final String EVENT_NIC_UPDATE = "NIC.UPDATE";
    public static final String EVENT_NIC_DETAIL_ADD = "NIC.DETAIL.ADD";
    public static final String EVENT_NIC_DETAIL_UPDATE = "NIC.DETAIL.UPDATE";
    public static final String EVENT_NIC_DETAIL_REMOVE = "NIC.DETAIL.REMOVE";

    // Load Balancers
    public static final String EVENT_ASSIGN_TO_LOAD_BALANCER_RULE = "LB.ASSIGN.TO.RULE";
    public static final String EVENT_REMOVE_FROM_LOAD_BALANCER_RULE = "LB.REMOVE.FROM.RULE";
    public static final String EVENT_LOAD_BALANCER_CREATE = "LB.CREATE";
    public static final String EVENT_LOAD_BALANCER_DELETE = "LB.DELETE";
    public static final String EVENT_LB_STICKINESSPOLICY_CREATE = "LB.STICKINESSPOLICY.CREATE";
    public static final String EVENT_LB_STICKINESSPOLICY_UPDATE = "LB.STICKINESSPOLICY.UPDATE";
    public static final String EVENT_LB_STICKINESSPOLICY_DELETE = "LB.STICKINESSPOLICY.DELETE";
    public static final String EVENT_LB_HEALTHCHECKPOLICY_CREATE = "LB.HEALTHCHECKPOLICY.CREATE";
    public static final String EVENT_LB_HEALTHCHECKPOLICY_DELETE = "LB.HEALTHCHECKPOLICY.DELETE";
    public static final String EVENT_LB_HEALTHCHECKPOLICY_UPDATE = "LB.HEALTHCHECKPOLICY.UPDATE";
    public static final String EVENT_LOAD_BALANCER_UPDATE = "LB.UPDATE";
    public static final String EVENT_LB_CERT_UPLOAD = "LB.CERT.UPLOAD";
    public static final String EVENT_LB_CERT_DELETE = "LB.CERT.DELETE";
    public static final String EVENT_LB_CERT_ASSIGN = "LB.CERT.ASSIGN";
    public static final String EVENT_LB_CERT_REMOVE = "LB.CERT.REMOVE";
    public static final String EVENT_LOAD_BALANCER_CONFIG_CREATE = "LB.CONFIG.CREATE";
    public static final String EVENT_LOAD_BALANCER_CONFIG_DELETE = "LB.CONFIG.DELETE";
    public static final String EVENT_LOAD_BALANCER_CONFIG_REPLACE = "LB.CONFIG.REPLACE";
    public static final String EVENT_LOAD_BALANCER_CONFIG_UPDATE = "LB.CONFIG.UPDATE";

    // Global Load Balancer rules
    public static final String EVENT_ASSIGN_TO_GLOBAL_LOAD_BALANCER_RULE = "GLOBAL.LB.ASSIGN";
    public static final String EVENT_REMOVE_FROM_GLOBAL_LOAD_BALANCER_RULE = "GLOBAL.LB.REMOVE";
    public static final String EVENT_GLOBAL_LOAD_BALANCER_CREATE = "GLOBAL.LB.CREATE";
    public static final String EVENT_GLOBAL_LOAD_BALANCER_DELETE = "GLOBAL.LB.DELETE";
    public static final String EVENT_GLOBAL_LOAD_BALANCER_UPDATE = "GLOBAL.LB.UPDATE";

    // Role events
    public static final String EVENT_ROLE_CREATE = "ROLE.CREATE";
    public static final String EVENT_ROLE_UPDATE = "ROLE.UPDATE";
    public static final String EVENT_ROLE_DELETE = "ROLE.DELETE";
    public static final String EVENT_ROLE_IMPORT = "ROLE.IMPORT";
    public static final String EVENT_ROLE_PERMISSION_CREATE = "ROLE.PERMISSION.CREATE";
    public static final String EVENT_ROLE_PERMISSION_UPDATE = "ROLE.PERMISSION.UPDATE";
    public static final String EVENT_ROLE_PERMISSION_DELETE = "ROLE.PERMISSION.DELETE";

    // Project Role events
    public static final  String EVENT_PROJECT_ROLE_CREATE = "PROJECT.ROLE.CREATE";
    public static final  String EVENT_PROJECT_ROLE_UPDATE = "PROJECT.ROLE.UPDATE";
    public static final  String EVENT_PROJECT_ROLE_DELETE = "PROJECT.ROLE.DELETE";
    public static final String EVENT_PROJECT_ROLE_PERMISSION_CREATE = "PROJECT.ROLE.PERMISSION.CREATE";
    public static final String EVENT_PROJECT_ROLE_PERMISSION_UPDATE = "PROJECT.ROLE.PERMISSION.UPDATE";
    public static final String EVENT_PROJECT_ROLE_PERMISSION_DELETE = "PROJECT.ROLE.PERMISSION.DELETE";

    // CA events
    public static final String EVENT_CA_CERTIFICATE_ISSUE = "CA.CERTIFICATE.ISSUE";
    public static final String EVENT_CA_CERTIFICATE_REVOKE = "CA.CERTIFICATE.REVOKE";
    public static final String EVENT_CA_CERTIFICATE_PROVISION = "CA.CERTIFICATE.PROVISION";

    // Account events
    public static final String EVENT_ACCOUNT_ENABLE = "ACCOUNT.ENABLE";
    public static final String EVENT_ACCOUNT_DISABLE = "ACCOUNT.DISABLE";
    public static final String EVENT_ACCOUNT_CREATE = "ACCOUNT.CREATE";
    public static final String EVENT_ACCOUNT_DELETE = "ACCOUNT.DELETE";
    public static final String EVENT_ACCOUNT_UPDATE = "ACCOUNT.UPDATE";
    public static final String EVENT_ACCOUNT_MARK_DEFAULT_ZONE = "ACCOUNT.MARK.DEFAULT.ZONE";

    // UserVO Events
    public static final String EVENT_USER_LOGIN = "USER.LOGIN";
    public static final String EVENT_USER_LOGOUT = "USER.LOGOUT";
    public static final String EVENT_USER_CREATE = "USER.CREATE";
    public static final String EVENT_USER_DELETE = "USER.DELETE";
    public static final String EVENT_USER_DISABLE = "USER.DISABLE";
    public static final String EVENT_USER_MOVE = "USER.MOVE";
    public static final String EVENT_USER_UPDATE = "USER.UPDATE";
    public static final String EVENT_USER_ENABLE = "USER.ENABLE";
    public static final String EVENT_USER_LOCK = "USER.LOCK";

    //registering SSH keypair events
    public static final String EVENT_REGISTER_SSH_KEYPAIR = "REGISTER.SSH.KEYPAIR";

    //registering userdata events
    public static final String EVENT_REGISTER_USER_DATA = "REGISTER.USER.DATA";

    //register for user API and secret keys
    public static final String EVENT_REGISTER_FOR_SECRET_API_KEY = "REGISTER.USER.KEY";

    // Template Events
    public static final String EVENT_TEMPLATE_CREATE = "TEMPLATE.CREATE";
    public static final String EVENT_TEMPLATE_DELETE = "TEMPLATE.DELETE";
    public static final String EVENT_TEMPLATE_UPDATE = "TEMPLATE.UPDATE";
    public static final String EVENT_TEMPLATE_DOWNLOAD_START = "TEMPLATE.DOWNLOAD.START";
    public static final String EVENT_TEMPLATE_DOWNLOAD_SUCCESS = "TEMPLATE.DOWNLOAD.SUCCESS";
    public static final String EVENT_TEMPLATE_DOWNLOAD_FAILED = "TEMPLATE.DOWNLOAD.FAILED";
    public static final String EVENT_TEMPLATE_COPY = "TEMPLATE.COPY";
    public static final String EVENT_TEMPLATE_EXTRACT = "TEMPLATE.EXTRACT";
    public static final String EVENT_TEMPLATE_UPLOAD = "TEMPLATE.UPLOAD";
    public static final String EVENT_TEMPLATE_CLEANUP = "TEMPLATE.CLEANUP";
    public static final String EVENT_FILE_MIGRATE = "FILE.MIGRATE";

    // Volume Events
    public static final String EVENT_VOLUME_CREATE = "VOLUME.CREATE";
    public static final String EVENT_VOLUME_DELETE = "VOLUME.DELETE";
    public static final String EVENT_VOLUME_ATTACH = "VOLUME.ATTACH";
    public static final String EVENT_VOLUME_DETACH = "VOLUME.DETACH";
    public static final String EVENT_VOLUME_EXTRACT = "VOLUME.EXTRACT";
    public static final String EVENT_VOLUME_UPLOAD = "VOLUME.UPLOAD";
    public static final String EVENT_VOLUME_MIGRATE = "VOLUME.MIGRATE";
    public static final String EVENT_VOLUME_RESIZE = "VOLUME.RESIZE";
    public static final String EVENT_VOLUME_DETAIL_UPDATE = "VOLUME.DETAIL.UPDATE";
    public static final String EVENT_VOLUME_DETAIL_ADD = "VOLUME.DETAIL.ADD";
    public static final String EVENT_VOLUME_DETAIL_REMOVE = "VOLUME.DETAIL.REMOVE";
    public static final String EVENT_VOLUME_UPDATE = "VOLUME.UPDATE";
    public static final String EVENT_VOLUME_DESTROY = "VOLUME.DESTROY";
    public static final String EVENT_VOLUME_RECOVER = "VOLUME.RECOVER";
    public static final String EVENT_VOLUME_CHANGE_DISK_OFFERING = "VOLUME.CHANGE.DISK.OFFERING";

    // Domains
    public static final String EVENT_DOMAIN_CREATE = "DOMAIN.CREATE";
    public static final String EVENT_DOMAIN_DELETE = "DOMAIN.DELETE";
    public static final String EVENT_DOMAIN_UPDATE = "DOMAIN.UPDATE";

    // Snapshots
    public static final String EVENT_SNAPSHOT_CREATE = "SNAPSHOT.CREATE";
    public static final String EVENT_SNAPSHOT_ON_PRIMARY = "SNAPSHOT.ON_PRIMARY";
    public static final String EVENT_SNAPSHOT_OFF_PRIMARY = "SNAPSHOT.OFF_PRIMARY";
    public static final String EVENT_SNAPSHOT_DELETE = "SNAPSHOT.DELETE";
    public static final String EVENT_SNAPSHOT_REVERT = "SNAPSHOT.REVERT";
    public static final String EVENT_SNAPSHOT_POLICY_CREATE = "SNAPSHOTPOLICY.CREATE";
    public static final String EVENT_SNAPSHOT_POLICY_UPDATE = "SNAPSHOTPOLICY.UPDATE";
    public static final String EVENT_SNAPSHOT_POLICY_DELETE = "SNAPSHOTPOLICY.DELETE";

    // ISO
    public static final String EVENT_ISO_CREATE = "ISO.CREATE";
    public static final String EVENT_ISO_UPDATE = "ISO.UPDATE";
    public static final String EVENT_ISO_DELETE = "ISO.DELETE";
    public static final String EVENT_ISO_COPY = "ISO.COPY";
    public static final String EVENT_ISO_ATTACH = "ISO.ATTACH";
    public static final String EVENT_ISO_DETACH = "ISO.DETACH";
    public static final String EVENT_ISO_EXTRACT = "ISO.EXTRACT";
    public static final String EVENT_ISO_UPLOAD = "ISO.UPLOAD";

    // SSVM
    public static final String EVENT_SSVM_CREATE = "SSVM.CREATE";
    public static final String EVENT_SSVM_DESTROY = "SSVM.DESTROY";
    public static final String EVENT_SSVM_START = "SSVM.START";
    public static final String EVENT_SSVM_STOP = "SSVM.STOP";
    public static final String EVENT_SSVM_REBOOT = "SSVM.REBOOT";
    public static final String EVENT_SSVM_HA = "SSVM.HA";
    public static final String EVENT_SSVM_DIAGNOSTICS = "SSVM.DIAGNOSTICS";

    // Service Offerings
    public static final String EVENT_SERVICE_OFFERING_CREATE = "SERVICE.OFFERING.CREATE";
    public static final String EVENT_SERVICE_OFFERING_EDIT = "SERVICE.OFFERING.EDIT";
    public static final String EVENT_SERVICE_OFFERING_DELETE = "SERVICE.OFFERING.DELETE";

    // Disk Offerings
    public static final String EVENT_DISK_OFFERING_CREATE = "DISK.OFFERING.CREATE";
    public static final String EVENT_DISK_OFFERING_EDIT = "DISK.OFFERING.EDIT";
    public static final String EVENT_DISK_OFFERING_DELETE = "DISK.OFFERING.DELETE";

    // Network offerings
    public static final String EVENT_NETWORK_OFFERING_CREATE = "NETWORK.OFFERING.CREATE";
    public static final String EVENT_NETWORK_OFFERING_ASSIGN = "NETWORK.OFFERING.ASSIGN";
    public static final String EVENT_NETWORK_OFFERING_EDIT = "NETWORK.OFFERING.EDIT";
    public static final String EVENT_NETWORK_OFFERING_REMOVE = "NETWORK.OFFERING.REMOVE";
    public static final String EVENT_NETWORK_OFFERING_DELETE = "NETWORK.OFFERING.DELETE";

    // Pods
    public static final String EVENT_POD_CREATE = "POD.CREATE";
    public static final String EVENT_POD_EDIT = "POD.EDIT";
    public static final String EVENT_POD_DELETE = "POD.DELETE";

    // Zones
    public static final String EVENT_ZONE_CREATE = "ZONE.CREATE";
    public static final String EVENT_ZONE_EDIT = "ZONE.EDIT";
    public static final String EVENT_ZONE_DELETE = "ZONE.DELETE";

    // VLANs/IP ranges
    public static final String EVENT_VLAN_IP_RANGE_CREATE = "VLAN.IP.RANGE.CREATE";
    public static final String EVENT_VLAN_IP_RANGE_DELETE = "VLAN.IP.RANGE.DELETE";
    public static final String EVENT_VLAN_IP_RANGE_DEDICATE = "VLAN.IP.RANGE.DEDICATE";
    public static final String EVENT_VLAN_IP_RANGE_RELEASE = "VLAN.IP.RANGE.RELEASE";
    public static final String EVENT_VLAN_IP_RANGE_UPDATE = "VLAN.IP.RANGE.UPDATE";

    public static final String EVENT_MANAGEMENT_IP_RANGE_CREATE = "MANAGEMENT.IP.RANGE.CREATE";
    public static final String EVENT_MANAGEMENT_IP_RANGE_DELETE = "MANAGEMENT.IP.RANGE.DELETE";
    public static final String EVENT_MANAGEMENT_IP_RANGE_UPDATE = "MANAGEMENT.IP.RANGE.UPDATE";

    public static final String EVENT_GUEST_IP6_PREFIX_CREATE = "GUEST.IP6.PREFIX.CREATE";
    public static final String EVENT_GUEST_IP6_PREFIX_DELETE = "GUEST.IP6.PREFIX.DELETE";

    public static final String EVENT_STORAGE_IP_RANGE_CREATE = "STORAGE.IP.RANGE.CREATE";
    public static final String EVENT_STORAGE_IP_RANGE_DELETE = "STORAGE.IP.RANGE.DELETE";
    public static final String EVENT_STORAGE_IP_RANGE_UPDATE = "STORAGE.IP.RANGE.UPDATE";

    public static final String EVENT_IMAGE_STORE_DATA_MIGRATE = "IMAGE.STORE.MIGRATE.DATA";

    // Configuration Table
    public static final String EVENT_CONFIGURATION_VALUE_EDIT = "CONFIGURATION.VALUE.EDIT";

    // Security Groups
    public static final String EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS = "SG.AUTH.INGRESS";
    public static final String EVENT_SECURITY_GROUP_REVOKE_INGRESS = "SG.REVOKE.INGRESS";
    public static final String EVENT_SECURITY_GROUP_AUTHORIZE_EGRESS = "SG.AUTH.EGRESS";
    public static final String EVENT_SECURITY_GROUP_REVOKE_EGRESS = "SG.REVOKE.EGRESS";
    public static final String EVENT_SECURITY_GROUP_CREATE = "SG.CREATE";
    public static final String EVENT_SECURITY_GROUP_DELETE = "SG.DELETE";
    public static final String EVENT_SECURITY_GROUP_ASSIGN = "SG.ASSIGN";
    public static final String EVENT_SECURITY_GROUP_REMOVE = "SG.REMOVE";
    public static final String EVENT_SECURITY_GROUP_UPDATE = "SG.UPDATE";

    // Host
    public static final String EVENT_HOST_RECONNECT = "HOST.RECONNECT";

    // Host on Degraded ResourceState
    public static final String EVENT_DECLARE_HOST_DEGRADED = "HOST.DECLARE.DEGRADED";
    public static final String EVENT_CANCEL_HOST_DEGRADED = "HOST.CANCEL.DEGRADED";

    // Host Out-of-band management
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_ENABLE = "HOST.OOBM.ENABLE";
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_DISABLE = "HOST.OOBM.DISABLE";
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_CONFIGURE = "HOST.OOBM.CONFIGURE";
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_ACTION = "HOST.OOBM.ACTION";
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_CHANGE_PASSWORD = "HOST.OOBM.CHANGEPASSWORD";
    public static final String EVENT_HOST_OUTOFBAND_MANAGEMENT_POWERSTATE_TRANSITION = "HOST.OOBM.POWERSTATE.TRANSITION";

    // HA
    public static final String EVENT_HA_RESOURCE_ENABLE = "HA.RESOURCE.ENABLE";
    public static final String EVENT_HA_RESOURCE_DISABLE = "HA.RESOURCE.DISABLE";
    public static final String EVENT_HA_RESOURCE_CONFIGURE = "HA.RESOURCE.CONFIGURE";
    public static final String EVENT_HA_STATE_TRANSITION = "HA.STATE.TRANSITION";

    // Maintenance
    public static final String EVENT_MAINTENANCE_CANCEL = "MAINT.CANCEL";
    public static final String EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE = "MAINT.CANCEL.PS";
    public static final String EVENT_MAINTENANCE_PREPARE = "MAINT.PREPARE";
    public static final String EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE = "MAINT.PREPARE.PS";

    // Primary storage pool
    public static final String EVENT_ENABLE_PRIMARY_STORAGE = "ENABLE.PS";
    public static final String EVENT_DISABLE_PRIMARY_STORAGE = "DISABLE.PS";
    public static final String EVENT_SYNC_STORAGE_POOL = "SYNC.STORAGE.POOL";

    // VPN
    public static final String EVENT_REMOTE_ACCESS_VPN_CREATE = "VPN.REMOTE.ACCESS.CREATE";
    public static final String EVENT_REMOTE_ACCESS_VPN_DESTROY = "VPN.REMOTE.ACCESS.DESTROY";
    public static final String EVENT_REMOTE_ACCESS_VPN_UPDATE = "VPN.REMOTE.ACCESS.UPDATE";
    public static final String EVENT_VPN_USER_ADD = "VPN.USER.ADD";
    public static final String EVENT_VPN_USER_REMOVE = "VPN.USER.REMOVE";
    public static final String EVENT_S2S_VPN_GATEWAY_CREATE = "VPN.S2S.VPN.GATEWAY.CREATE";
    public static final String EVENT_S2S_VPN_GATEWAY_DELETE = "VPN.S2S.VPN.GATEWAY.DELETE";
    public static final String EVENT_S2S_VPN_GATEWAY_UPDATE = "VPN.S2S.VPN.GATEWAY.UPDATE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE = "VPN.S2S.CUSTOMER.GATEWAY.CREATE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE = "VPN.S2S.CUSTOMER.GATEWAY.DELETE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_UPDATE = "VPN.S2S.CUSTOMER.GATEWAY.UPDATE";
    public static final String EVENT_S2S_VPN_CONNECTION_CREATE = "VPN.S2S.CONNECTION.CREATE";
    public static final String EVENT_S2S_VPN_CONNECTION_DELETE = "VPN.S2S.CONNECTION.DELETE";
    public static final String EVENT_S2S_VPN_CONNECTION_RESET = "VPN.S2S.CONNECTION.RESET";
    public static final String EVENT_S2S_VPN_CONNECTION_UPDATE = "VPN.S2S.CONNECTION.UPDATE";

    // Network
    public static final String EVENT_NETWORK_RESTART = "NETWORK.RESTART";

    // Custom certificates
    public static final String EVENT_UPLOAD_CUSTOM_CERTIFICATE = "UPLOAD.CUSTOM.CERTIFICATE";

    // OneToOnenat
    public static final String EVENT_ENABLE_STATIC_NAT = "STATICNAT.ENABLE";
    public static final String EVENT_DISABLE_STATIC_NAT = "STATICNAT.DISABLE";

    public static final String EVENT_ZONE_VLAN_ASSIGN = "ZONE.VLAN.ASSIGN";
    public static final String EVENT_ZONE_VLAN_RELEASE = "ZONE.VLAN.RELEASE";

    // Projects
    public static final String EVENT_PROJECT_CREATE = "PROJECT.CREATE";
    public static final String EVENT_PROJECT_UPDATE = "PROJECT.UPDATE";
    public static final String EVENT_PROJECT_DELETE = "PROJECT.DELETE";
    public static final String EVENT_PROJECT_ACTIVATE = "PROJECT.ACTIVATE";
    public static final String EVENT_PROJECT_SUSPEND = "PROJECT.SUSPEND";
    public static final String EVENT_PROJECT_ACCOUNT_ADD = "PROJECT.ACCOUNT.ADD";
    public static final String EVENT_PROJECT_USER_ADD = "PROJECT.USER.ADD";
    public static final String EVENT_PROJECT_INVITATION_UPDATE = "PROJECT.INVITATION.UPDATE";
    public static final String EVENT_PROJECT_INVITATION_REMOVE = "PROJECT.INVITATION.REMOVE";
    public static final String EVENT_PROJECT_ACCOUNT_REMOVE = "PROJECT.ACCOUNT.REMOVE";
    public static final String EVENT_PROJECT_USER_REMOVE = "PROJECT.USER.REMOVE";

    // Network as a Service
    public static final String EVENT_NETWORK_ELEMENT_CONFIGURE = "NETWORK.ELEMENT.CONFIGURE";

    // Physical Network Events
    public static final String EVENT_PHYSICAL_NETWORK_CREATE = "PHYSICAL.NETWORK.CREATE";
    public static final String EVENT_PHYSICAL_NETWORK_DELETE = "PHYSICAL.NETWORK.DELETE";
    public static final String EVENT_PHYSICAL_NETWORK_UPDATE = "PHYSICAL.NETWORK.UPDATE";

    // Physical Network Service Provider Events
    public static final String EVENT_SERVICE_PROVIDER_CREATE = "SERVICE.PROVIDER.CREATE";
    public static final String EVENT_SERVICE_PROVIDER_DELETE = "SERVICE.PROVIDER.DELETE";
    public static final String EVENT_SERVICE_PROVIDER_UPDATE = "SERVICE.PROVIDER.UPDATE";

    // Physical Network TrafficType Events
    public static final String EVENT_TRAFFIC_TYPE_CREATE = "TRAFFIC.TYPE.CREATE";
    public static final String EVENT_TRAFFIC_TYPE_DELETE = "TRAFFIC.TYPE.DELETE";
    public static final String EVENT_TRAFFIC_TYPE_UPDATE = "TRAFFIC.TYPE.UPDATE";

    // external network device events
    public static final String EVENT_EXTERNAL_LB_DEVICE_ADD = "PHYSICAL.LOADBALANCER.ADD";
    public static final String EVENT_EXTERNAL_LB_DEVICE_DELETE = "PHYSICAL.LOADBALANCER.DELETE";
    public static final String EVENT_EXTERNAL_LB_DEVICE_CONFIGURE = "PHYSICAL.LOADBALANCER.CONFIGURE";

    // external NCC device events
    public static final String EVENT_EXTERNAL_NCC_DEVICE_ADD = "PHYSICAL.NCC.ADD";
    public static final String EVENT_EXTERNAL_NCC_DEVICE_DELETE = "PHYSICAL.NCC.DELETE";

    // external switch management device events (E.g.: Cisco Nexus 1000v Virtual Supervisor Module.
    public static final String EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ADD = "SWITCH.MGMT.ADD";
    public static final String EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DELETE = "SWITCH.MGMT.DELETE";
    public static final String EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_CONFIGURE = "SWITCH.MGMT.CONFIGURE";
    public static final String EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ENABLE = "SWITCH.MGMT.ENABLE";
    public static final String EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE = "SWITCH.MGMT.DISABLE";

    public static final String EVENT_EXTERNAL_FIREWALL_DEVICE_ADD = "PHYSICAL.FIREWALL.ADD";
    public static final String EVENT_EXTERNAL_FIREWALL_DEVICE_DELETE = "PHYSICAL.FIREWALL.DELETE";
    public static final String EVENT_EXTERNAL_FIREWALL_DEVICE_CONFIGURE = "PHYSICAL.FIREWALL.CONFIGURE";

    // VPC
    public static final String EVENT_VPC_CREATE = "VPC.CREATE";
    public static final String EVENT_VPC_UPDATE = "VPC.UPDATE";
    public static final String EVENT_VPC_DELETE = "VPC.DELETE";
    public static final String EVENT_VPC_RESTART = "VPC.RESTART";

    // Network ACL
    public static final String EVENT_NETWORK_ACL_CREATE = "NETWORK.ACL.CREATE";
    public static final String EVENT_NETWORK_ACL_DELETE = "NETWORK.ACL.DELETE";
    public static final String EVENT_NETWORK_ACL_REPLACE = "NETWORK.ACL.REPLACE";
    public static final String EVENT_NETWORK_ACL_UPDATE = "NETWORK.ACL.UPDATE";
    public static final String EVENT_NETWORK_ACL_ITEM_CREATE = "NETWORK.ACL.ITEM.CREATE";
    public static final String EVENT_NETWORK_ACL_ITEM_UPDATE = "NETWORK.ACL.ITEM.UPDATE";
    public static final String EVENT_NETWORK_ACL_ITEM_DELETE = "NETWORK.ACL.ITEM.DELETE";

    // IPv6 firewall rule
    public static final String EVENT_IPV6_FIREWALL_RULE_CREATE = "IPV6.FIREWALL.RULE.CREATE";
    public static final String EVENT_IPV6_FIREWALL_RULE_UPDATE = "IPV6.FIREWALL.RULE.UPDATE";
    public static final String EVENT_IPV6_FIREWALL_RULE_DELETE = "IPV6.FIREWALL.RULE.DELETE";

    // VPC offerings
    public static final String EVENT_VPC_OFFERING_CREATE = "VPC.OFFERING.CREATE";
    public static final String EVENT_VPC_OFFERING_UPDATE = "VPC.OFFERING.UPDATE";
    public static final String EVENT_VPC_OFFERING_DELETE = "VPC.OFFERING.DELETE";

    // Private gateway
    public static final String EVENT_PRIVATE_GATEWAY_CREATE = "PRIVATE.GATEWAY.CREATE";
    public static final String EVENT_PRIVATE_GATEWAY_DELETE = "PRIVATE.GATEWAY.DELETE";

    // Static routes
    public static final String EVENT_STATIC_ROUTE_CREATE = "STATIC.ROUTE.CREATE";
    public static final String EVENT_STATIC_ROUTE_DELETE = "STATIC.ROUTE.DELETE";

    // tag related events
    public static final String EVENT_TAGS_CREATE = "CREATE_TAGS";
    public static final String EVENT_TAGS_DELETE = "DELETE_TAGS";

    // resource icon related events
    public static final String EVENT_RESOURCE_ICON_UPLOAD = "UPLOAD.RESOURCE.ICON";
    public static final String EVENT_RESOURCE_ICON_DELETE = "DELETE.RESOURCE.ICON";

    // meta data related events
    public static final String EVENT_RESOURCE_DETAILS_CREATE = "CREATE_RESOURCE_DETAILS";
    public static final String EVENT_RESOURCE_DETAILS_DELETE = "DELETE_RESOURCE_DETAILS";

    // vm snapshot events
    public static final String EVENT_VM_SNAPSHOT_CREATE = "VMSNAPSHOT.CREATE";
    public static final String EVENT_VM_SNAPSHOT_DELETE = "VMSNAPSHOT.DELETE";
    public static final String EVENT_VM_SNAPSHOT_ON_PRIMARY = "VMSNAPSHOT.ON_PRIMARY";
    public static final String EVENT_VM_SNAPSHOT_OFF_PRIMARY = "VMSNAPSHOT.OFF_PRIMARY";
    public static final String EVENT_VM_SNAPSHOT_REVERT = "VMSNAPSHOT.REVERTTO";

    // Backup and Recovery events
    public static final String EVENT_VM_BACKUP_IMPORT_OFFERING = "BACKUP.IMPORT.OFFERING";
    public static final String EVENT_VM_BACKUP_OFFERING_ASSIGN = "BACKUP.OFFERING.ASSIGN";
    public static final String EVENT_VM_BACKUP_OFFERING_REMOVE = "BACKUP.OFFERING.REMOVE";
    public static final String EVENT_VM_BACKUP_CREATE = "BACKUP.CREATE";
    public static final String EVENT_VM_BACKUP_RESTORE = "BACKUP.RESTORE";
    public static final String EVENT_VM_BACKUP_DELETE = "BACKUP.DELETE";
    public static final String EVENT_VM_BACKUP_RESTORE_VOLUME_TO_VM = "BACKUP.RESTORE.VOLUME.TO.VM";
    public static final String EVENT_VM_BACKUP_SCHEDULE_CONFIGURE = "BACKUP.SCHEDULE.CONFIGURE";
    public static final String EVENT_VM_BACKUP_SCHEDULE_DELETE = "BACKUP.SCHEDULE.DELETE";
    public static final String EVENT_VM_BACKUP_USAGE_METRIC = "BACKUP.USAGE.METRIC";
    public static final String EVENT_VM_BACKUP_EDIT = "BACKUP.OFFERING.EDIT";

    // external network device events
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_ADD = "PHYSICAL.NVPCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_DELETE = "PHYSICAL.NVPCONTROLLER.DELETE";
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_CONFIGURE = "PHYSICAL.NVPCONTROLLER.CONFIGURE";
    public static final String EVENT_EXTERNAL_OVS_CONTROLLER_ADD = "PHYSICAL.OVSCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_OVS_CONTROLLER_DELETE = "PHYSICAL.OVSCONTROLLER.DELETE";

    // external network mapping events
    // AutoScale
    public static final String EVENT_COUNTER_CREATE = "COUNTER.CREATE";
    public static final String EVENT_COUNTER_DELETE = "COUNTER.DELETE";
    public static final String EVENT_CONDITION_CREATE = "CONDITION.CREATE";
    public static final String EVENT_CONDITION_DELETE = "CONDITION.DELETE";
    public static final String EVENT_CONDITION_UPDATE = "CONDITION.UPDATE";
    public static final String EVENT_AUTOSCALEPOLICY_CREATE = "AUTOSCALEPOLICY.CREATE";
    public static final String EVENT_AUTOSCALEPOLICY_UPDATE = "AUTOSCALEPOLICY.UPDATE";
    public static final String EVENT_AUTOSCALEPOLICY_DELETE = "AUTOSCALEPOLICY.DELETE";
    public static final String EVENT_AUTOSCALEVMPROFILE_CREATE = "AUTOSCALEVMPROFILE.CREATE";
    public static final String EVENT_AUTOSCALEVMPROFILE_DELETE = "AUTOSCALEVMPROFILE.DELETE";
    public static final String EVENT_AUTOSCALEVMPROFILE_UPDATE = "AUTOSCALEVMPROFILE.UPDATE";
    public static final String EVENT_AUTOSCALEVMGROUP_CREATE = "AUTOSCALEVMGROUP.CREATE";
    public static final String EVENT_AUTOSCALEVMGROUP_DELETE = "AUTOSCALEVMGROUP.DELETE";
    public static final String EVENT_AUTOSCALEVMGROUP_UPDATE = "AUTOSCALEVMGROUP.UPDATE";
    public static final String EVENT_AUTOSCALEVMGROUP_ENABLE = "AUTOSCALEVMGROUP.ENABLE";
    public static final String EVENT_AUTOSCALEVMGROUP_DISABLE = "AUTOSCALEVMGROUP.DISABLE";
    public static final String EVENT_AUTOSCALEVMGROUP_SCALEDOWN = "AUTOSCALEVMGROUP.SCALEDOWN";
    public static final String EVENT_AUTOSCALEVMGROUP_SCALEUP = "AUTOSCALEVMGROUP.SCALEUP";

    public static final String EVENT_BAREMETAL_DHCP_SERVER_ADD = "PHYSICAL.DHCP.ADD";
    public static final String EVENT_BAREMETAL_DHCP_SERVER_DELETE = "PHYSICAL.DHCP.DELETE";
    public static final String EVENT_BAREMETAL_PXE_SERVER_ADD = "PHYSICAL.PXE.ADD";
    public static final String EVENT_BAREMETAL_PXE_SERVER_DELETE = "PHYSICAL.PXE.DELETE";
    public static final String EVENT_BAREMETAL_RCT_ADD = "BAREMETAL.RCT.ADD";
    public static final String EVENT_BAREMETAL_RCT_DELETE = "BAREMETAL.RCT.DELETE";
    public static final String EVENT_BAREMETAL_PROVISION_DONE = "BAREMETAL.PROVISION.DONE";

    public static final String EVENT_AFFINITY_GROUP_CREATE = "AG.CREATE";
    public static final String EVENT_AFFINITY_GROUP_DELETE = "AG.DELETE";
    public static final String EVENT_AFFINITY_GROUP_ASSIGN = "AG.ASSIGN";
    public static final String EVENT_AFFINITY_GROUP_REMOVE = "AG.REMOVE";
    public static final String EVENT_VM_AFFINITY_GROUP_UPDATE = "VM.AG.UPDATE";

    public static final String EVENT_INTERNAL_LB_VM_START = "INTERNALLBVM.START";
    public static final String EVENT_INTERNAL_LB_VM_STOP = "INTERNALLBVM.STOP";

    public static final String EVENT_HOST_RESERVATION_RELEASE = "HOST.RESERVATION.RELEASE";
    // Dedicated guest vlan range
    public static final String EVENT_GUEST_VLAN_RANGE_DEDICATE = "GUESTVLANRANGE.DEDICATE";
    public static final String EVENT_DEDICATED_GUEST_VLAN_RANGE_RELEASE = "GUESTVLANRANGE.RELEASE";

    public static final String EVENT_PORTABLE_IP_RANGE_CREATE = "PORTABLE.IP.RANGE.CREATE";
    public static final String EVENT_PORTABLE_IP_RANGE_DELETE = "PORTABLE.IP.RANGE.DELETE";
    public static final String EVENT_PORTABLE_IP_TRANSFER = "PORTABLE.IP.TRANSFER";

    // Dedicated Resources
    public static final String EVENT_DEDICATE_RESOURCE = "DEDICATE.RESOURCE";
    public static final String EVENT_DEDICATE_RESOURCE_RELEASE = "DEDICATE.RESOURCE.RELEASE";

    public static final String EVENT_CLEANUP_VM_RESERVATION = "VM.RESERVATION.CLEANUP";

    public static final String EVENT_UCS_ASSOCIATED_PROFILE = "UCS.ASSOCIATEPROFILE";

    // Object store migration
    public static final String EVENT_MIGRATE_PREPARE_SECONDARY_STORAGE = "MIGRATE.PREPARE.SS";

    //Alert generation
    public static final String ALERT_GENERATE = "ALERT.GENERATE";

    // OpenDaylight
    public static final String EVENT_EXTERNAL_OPENDAYLIGHT_ADD_CONTROLLER = "PHYSICAL.ODLCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_OPENDAYLIGHT_DELETE_CONTROLLER = "PHYSICAL.ODLCONTROLLER.DELETE";
    public static final String EVENT_EXTERNAL_OPENDAYLIGHT_CONFIGURE_CONTROLLER = "PHYSICAL.ODLCONTROLLER.CONFIGURE";

    //Guest OS related events
    public static final String EVENT_GUEST_OS_ADD = "GUEST.OS.ADD";
    public static final String EVENT_GUEST_OS_REMOVE = "GUEST.OS.REMOVE";
    public static final String EVENT_GUEST_OS_UPDATE = "GUEST.OS.UPDATE";
    public static final String EVENT_GUEST_OS_MAPPING_ADD = "GUEST.OS.MAPPING.ADD";
    public static final String EVENT_GUEST_OS_MAPPING_REMOVE = "GUEST.OS.MAPPING.REMOVE";
    public static final String EVENT_GUEST_OS_MAPPING_UPDATE = "GUEST.OS.MAPPING.UPDATE";

    public static final String EVENT_NIC_SECONDARY_IP_ASSIGN = "NIC.SECONDARY.IP.ASSIGN";
    public static final String EVENT_NIC_SECONDARY_IP_UNASSIGN = "NIC.SECONDARY.IP.UNASSIGN";
    public static final String EVENT_NIC_SECONDARY_IP_CONFIGURE = "NIC.SECONDARY.IP.CONFIGURE";
    public static final String EVENT_NETWORK_EXTERNAL_DHCP_VM_IPFETCH = "EXTERNAL.DHCP.VM.IP.FETCH";

    //Usage related events
    public static final String EVENT_USAGE_REMOVE_USAGE_RECORDS = "USAGE.REMOVE.USAGE.RECORDS";

    // Netscaler Service Package events
    public static final String EVENT_NETSCALER_SERVICEPACKAGE_ADD = "NETSCALER.SERVICEPACKAGE.ADD";
    public static final String EVENT_NETSCALER_SERVICEPACKAGE_DELETE = "NETSCALER.SERVICEPACKAGE.DELETE";

    public static final String EVENT_NETSCALER_VM_START = "NETSCALERVM.START";
    public static final String EVENT_NETSCALER_VM_STOP = "NETSCALERVM.STOP";

    public static final String EVENT_ANNOTATION_CREATE = "ANNOTATION.CREATE";
    public static final String EVENT_ANNOTATION_REMOVE = "ANNOTATION.REMOVE";

    public static final String EVENT_TEMPLATE_DIRECT_DOWNLOAD_FAILURE = "TEMPLATE.DIRECT.DOWNLOAD.FAILURE";
    public static final String EVENT_ISO_DIRECT_DOWNLOAD_FAILURE = "ISO.DIRECT.DOWNLOAD.FAILURE";

    // Diagnostics Events
    public static final String EVENT_SYSTEM_VM_DIAGNOSTICS = "SYSTEM.VM.DIAGNOSTICS";

    // Rolling Maintenance
    public static final String EVENT_START_ROLLING_MAINTENANCE = "SYSTEM.ROLLING.MAINTENANCE";
    public static final String EVENT_HOST_ROLLING_MAINTENANCE = "HOST.ROLLING.MAINTENANCE";
    public static final String EVENT_CLUSTER_ROLLING_MAINTENANCE = "CLUSTER.ROLLING.MAINTENANCE";
    public static final String EVENT_POD_ROLLING_MAINTENANCE = "POD.ROLLING.MAINTENANCE";
    public static final String EVENT_ZONE_ROLLING_MAINTENANCE = "ZONE.ROLLING.MAINTENANCE";

    // Storage Policies
    public static final String EVENT_IMPORT_VCENTER_STORAGE_POLICIES = "IMPORT.VCENTER.STORAGE.POLICIES";

    // SystemVM
    public static final String EVENT_LIVE_PATCH_SYSTEMVM = "LIVE.PATCH.SYSTEM.VM";

    static {

        // TODO: need a way to force author adding event types to declare the entity details as well, with out braking

        entityEventDetails = new HashMap<String, Object>();

        entityEventDetails.put(EVENT_VM_CREATE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_DESTROY, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_START, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_STOP, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_REBOOT, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_UPDATE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_UPGRADE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_DYNAMIC_SCALE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_RESETPASSWORD, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_RESETSSHKEY, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_MIGRATE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_MOVE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_RESTORE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_EXPUNGE, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_IMPORT, VirtualMachine.class);
        entityEventDetails.put(EVENT_VM_UNMANAGE, VirtualMachine.class);

        entityEventDetails.put(EVENT_ROUTER_CREATE, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_DESTROY, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_START, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_STOP, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_REBOOT, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_HA, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_UPGRADE, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_DIAGNOSTICS, VirtualRouter.class);
        entityEventDetails.put(EVENT_ROUTER_HEALTH_CHECKS, VirtualRouter.class);

        entityEventDetails.put(EVENT_PROXY_CREATE, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_DESTROY, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_START, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_STOP, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_REBOOT, VirtualMachine.class);
        entityEventDetails.put(EVENT_ROUTER_HA, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_HA, VirtualMachine.class);
        entityEventDetails.put(EVENT_PROXY_DIAGNOSTICS, VirtualMachine.class);

        entityEventDetails.put(EVENT_VNC_CONNECT, "VNC");
        entityEventDetails.put(EVENT_VNC_DISCONNECT, "VNC");

        // Network Events
        entityEventDetails.put(EVENT_NETWORK_CREATE, Network.class);
        entityEventDetails.put(EVENT_NETWORK_DELETE, Network.class);
        entityEventDetails.put(EVENT_NETWORK_UPDATE, Network.class);
        entityEventDetails.put(EVENT_NETWORK_RESTART, Network.class);
        entityEventDetails.put(EVENT_NET_IP_ASSIGN, IpAddress.class);
        entityEventDetails.put(EVENT_PORTABLE_IP_ASSIGN, IpAddress.class);
        entityEventDetails.put(EVENT_PORTABLE_IP_RELEASE, IpAddress.class);
        entityEventDetails.put(EVENT_NET_IP_RELEASE, IpAddress.class);
        entityEventDetails.put(EVENT_NET_RULE_ADD, FirewallRule.class);
        entityEventDetails.put(EVENT_NET_RULE_DELETE, FirewallRule.class);
        entityEventDetails.put(EVENT_NET_RULE_MODIFY, FirewallRule.class);
        entityEventDetails.put(EVENT_FIREWALL_OPEN, FirewallRule.class);
        entityEventDetails.put(EVENT_FIREWALL_CLOSE, FirewallRule.class);
        entityEventDetails.put(EVENT_FIREWALL_EGRESS_OPEN, FirewallRule.class);
        entityEventDetails.put(EVENT_FIREWALL_EGRESS_CLOSE, FirewallRule.class);
        entityEventDetails.put(EVENT_FIREWALL_EGRESS_UPDATE, FirewallRule.class);
        entityEventDetails.put(EVENT_NET_IP6_ASSIGN, Network.class);
        entityEventDetails.put(EVENT_NET_IP6_RELEASE, Network.class);
        entityEventDetails.put(EVENT_NET_IP6_UPDATE, Network.class);

        // Nic Events
        entityEventDetails.put(EVENT_NIC_CREATE, Nic.class);

        // Load Balancers
        entityEventDetails.put(EVENT_ASSIGN_TO_LOAD_BALANCER_RULE, FirewallRule.class);
        entityEventDetails.put(EVENT_REMOVE_FROM_LOAD_BALANCER_RULE, FirewallRule.class);
        entityEventDetails.put(EVENT_LOAD_BALANCER_CREATE, LoadBalancer.class);
        entityEventDetails.put(EVENT_LOAD_BALANCER_DELETE, FirewallRule.class);
        entityEventDetails.put(EVENT_LB_STICKINESSPOLICY_CREATE, StickinessPolicy.class);
        entityEventDetails.put(EVENT_LB_STICKINESSPOLICY_UPDATE, StickinessPolicy.class);
        entityEventDetails.put(EVENT_LB_STICKINESSPOLICY_DELETE, StickinessPolicy.class);
        entityEventDetails.put(EVENT_LB_HEALTHCHECKPOLICY_CREATE, HealthCheckPolicy.class);
        entityEventDetails.put(EVENT_LB_HEALTHCHECKPOLICY_UPDATE, HealthCheckPolicy.class);
        entityEventDetails.put(EVENT_LB_HEALTHCHECKPOLICY_DELETE, HealthCheckPolicy.class);
        entityEventDetails.put(EVENT_LOAD_BALANCER_UPDATE, LoadBalancer.class);
        entityEventDetails.put(EVENT_LB_CERT_UPLOAD, LoadBalancer.class);
        entityEventDetails.put(EVENT_LB_CERT_DELETE, LoadBalancer.class);
        entityEventDetails.put(EVENT_LB_CERT_ASSIGN, LoadBalancer.class);
        entityEventDetails.put(EVENT_LB_CERT_REMOVE, LoadBalancer.class);

        // Role events
        entityEventDetails.put(EVENT_ROLE_CREATE, Role.class);
        entityEventDetails.put(EVENT_ROLE_UPDATE, Role.class);
        entityEventDetails.put(EVENT_ROLE_DELETE, Role.class);
        entityEventDetails.put(EVENT_ROLE_IMPORT, Role.class);
        entityEventDetails.put(EVENT_ROLE_PERMISSION_CREATE, RolePermission.class);
        entityEventDetails.put(EVENT_ROLE_PERMISSION_UPDATE, RolePermission.class);
        entityEventDetails.put(EVENT_ROLE_PERMISSION_DELETE, RolePermission.class);

        // Account events
        entityEventDetails.put(EVENT_ACCOUNT_ENABLE, Account.class);
        entityEventDetails.put(EVENT_ACCOUNT_DISABLE, Account.class);
        entityEventDetails.put(EVENT_ACCOUNT_CREATE, Account.class);
        entityEventDetails.put(EVENT_ACCOUNT_DELETE, Account.class);
        entityEventDetails.put(EVENT_ACCOUNT_UPDATE, Account.class);
        entityEventDetails.put(EVENT_ACCOUNT_MARK_DEFAULT_ZONE, Account.class);

        // UserVO Events
        entityEventDetails.put(EVENT_USER_LOGIN, User.class);
        entityEventDetails.put(EVENT_USER_LOGOUT, User.class);
        entityEventDetails.put(EVENT_USER_CREATE, User.class);
        entityEventDetails.put(EVENT_USER_DELETE, User.class);
        entityEventDetails.put(EVENT_USER_DISABLE, User.class);
        entityEventDetails.put(EVENT_USER_UPDATE, User.class);
        entityEventDetails.put(EVENT_USER_ENABLE, User.class);
        entityEventDetails.put(EVENT_USER_LOCK, User.class);

        // Template Events
        entityEventDetails.put(EVENT_TEMPLATE_CREATE, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_DELETE, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_UPDATE, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_START, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_SUCCESS, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_FAILED, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_COPY, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_EXTRACT, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_UPLOAD, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_TEMPLATE_CLEANUP, VirtualMachineTemplate.class);

        // Volume Events
        entityEventDetails.put(EVENT_VOLUME_CREATE, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_DELETE, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_ATTACH, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_DETACH, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_EXTRACT, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_UPLOAD, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_MIGRATE, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_RESIZE, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_DESTROY, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_RECOVER, Volume.class);
        entityEventDetails.put(EVENT_VOLUME_CHANGE_DISK_OFFERING, Volume.class);

        // Domains
        entityEventDetails.put(EVENT_DOMAIN_CREATE, Domain.class);
        entityEventDetails.put(EVENT_DOMAIN_DELETE, Domain.class);
        entityEventDetails.put(EVENT_DOMAIN_UPDATE, Domain.class);

        // Snapshots
        entityEventDetails.put(EVENT_SNAPSHOT_CREATE, Snapshot.class);
        entityEventDetails.put(EVENT_SNAPSHOT_DELETE, Snapshot.class);
        entityEventDetails.put(EVENT_SNAPSHOT_ON_PRIMARY, Snapshot.class);
        entityEventDetails.put(EVENT_SNAPSHOT_OFF_PRIMARY, Snapshot.class);
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_CREATE, SnapshotPolicy.class);
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_UPDATE, SnapshotPolicy.class);
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_DELETE, SnapshotPolicy.class);

        // ISO
        entityEventDetails.put(EVENT_ISO_CREATE, "Iso");
        entityEventDetails.put(EVENT_ISO_DELETE, "Iso");
        entityEventDetails.put(EVENT_ISO_COPY, "Iso");
        entityEventDetails.put(EVENT_ISO_ATTACH, "Iso");
        entityEventDetails.put(EVENT_ISO_DETACH, "Iso");
        entityEventDetails.put(EVENT_ISO_EXTRACT, "Iso");
        entityEventDetails.put(EVENT_ISO_UPLOAD, "Iso");

        // SSVM
        entityEventDetails.put(EVENT_SSVM_CREATE, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_DESTROY, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_START, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_STOP, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_REBOOT, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_HA, VirtualMachine.class);
        entityEventDetails.put(EVENT_SSVM_DIAGNOSTICS, VirtualMachine.class);

        // Service Offerings
        entityEventDetails.put(EVENT_SERVICE_OFFERING_CREATE, ServiceOffering.class);
        entityEventDetails.put(EVENT_SERVICE_OFFERING_EDIT, ServiceOffering.class);
        entityEventDetails.put(EVENT_SERVICE_OFFERING_DELETE, ServiceOffering.class);

        // Disk Offerings
        entityEventDetails.put(EVENT_DISK_OFFERING_CREATE, DiskOffering.class);
        entityEventDetails.put(EVENT_DISK_OFFERING_EDIT, DiskOffering.class);
        entityEventDetails.put(EVENT_DISK_OFFERING_DELETE, DiskOffering.class);

        // Network offerings
        entityEventDetails.put(EVENT_NETWORK_OFFERING_CREATE, NetworkOffering.class);
        entityEventDetails.put(EVENT_NETWORK_OFFERING_ASSIGN, NetworkOffering.class);
        entityEventDetails.put(EVENT_NETWORK_OFFERING_EDIT, NetworkOffering.class);
        entityEventDetails.put(EVENT_NETWORK_OFFERING_REMOVE, NetworkOffering.class);
        entityEventDetails.put(EVENT_NETWORK_OFFERING_DELETE, NetworkOffering.class);

        // Pods
        entityEventDetails.put(EVENT_POD_CREATE, Pod.class);
        entityEventDetails.put(EVENT_POD_EDIT, Pod.class);
        entityEventDetails.put(EVENT_POD_DELETE, Pod.class);

        // Zones
        entityEventDetails.put(EVENT_ZONE_CREATE, DataCenter.class);
        entityEventDetails.put(EVENT_ZONE_EDIT, DataCenter.class);
        entityEventDetails.put(EVENT_ZONE_DELETE, DataCenter.class);

        // VLANs/IP ranges
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_CREATE, Vlan.class);
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_DELETE, Vlan.class);
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_DEDICATE, Vlan.class);
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_RELEASE, Vlan.class);

        entityEventDetails.put(EVENT_MANAGEMENT_IP_RANGE_CREATE, Pod.class);
        entityEventDetails.put(EVENT_MANAGEMENT_IP_RANGE_DELETE, Pod.class);

        entityEventDetails.put(EVENT_GUEST_IP6_PREFIX_CREATE, DataCenterGuestIpv6Prefix.class);
        entityEventDetails.put(EVENT_GUEST_IP6_PREFIX_DELETE, DataCenterGuestIpv6Prefix.class);

        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_CREATE, StorageNetworkIpRange.class);
        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_DELETE, StorageNetworkIpRange.class);
        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_UPDATE, StorageNetworkIpRange.class);

        // Configuration Table
        entityEventDetails.put(EVENT_CONFIGURATION_VALUE_EDIT, Configuration.class);

        // Security Groups
        entityEventDetails.put(EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_REVOKE_INGRESS, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_AUTHORIZE_EGRESS, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_REVOKE_EGRESS, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_CREATE, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_DELETE, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_ASSIGN, SecurityGroup.class);
        entityEventDetails.put(EVENT_SECURITY_GROUP_REMOVE, SecurityGroup.class);

        // Host
        entityEventDetails.put(EVENT_HOST_RECONNECT, Host.class);

        // Host Out-of-band management
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_ENABLE, Host.class);
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_DISABLE, Host.class);
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_CONFIGURE, Host.class);
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_ACTION, Host.class);
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_CHANGE_PASSWORD, Host.class);
        entityEventDetails.put(EVENT_HOST_OUTOFBAND_MANAGEMENT_POWERSTATE_TRANSITION, Host.class);

        // HA
        entityEventDetails.put(EVENT_HA_RESOURCE_ENABLE, HAConfig.class);
        entityEventDetails.put(EVENT_HA_RESOURCE_DISABLE, HAConfig.class);
        entityEventDetails.put(EVENT_HA_RESOURCE_CONFIGURE, HAConfig.class);
        entityEventDetails.put(EVENT_HA_STATE_TRANSITION, HAConfig.class);

        // Maintenance
        entityEventDetails.put(EVENT_MAINTENANCE_CANCEL, Host.class);
        entityEventDetails.put(EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE, Host.class);
        entityEventDetails.put(EVENT_MAINTENANCE_PREPARE, Host.class);
        entityEventDetails.put(EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE, Host.class);

        // Primary storage pool
        entityEventDetails.put(EVENT_ENABLE_PRIMARY_STORAGE, StoragePool.class);
        entityEventDetails.put(EVENT_DISABLE_PRIMARY_STORAGE, StoragePool.class);

        // VPN
        entityEventDetails.put(EVENT_REMOTE_ACCESS_VPN_CREATE, RemoteAccessVpn.class);
        entityEventDetails.put(EVENT_REMOTE_ACCESS_VPN_DESTROY, RemoteAccessVpn.class);
        entityEventDetails.put(EVENT_VPN_USER_ADD, RemoteAccessVpn.class);
        entityEventDetails.put(EVENT_VPN_USER_REMOVE, RemoteAccessVpn.class);
        entityEventDetails.put(EVENT_S2S_VPN_GATEWAY_CREATE, Site2SiteVpnGateway.class);
        entityEventDetails.put(EVENT_S2S_VPN_GATEWAY_DELETE, Site2SiteVpnGateway.class);
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE, Site2SiteCustomerGateway.class);
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE, Site2SiteCustomerGateway.class);
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_UPDATE, Site2SiteCustomerGateway.class);
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_CREATE, Site2SiteVpnConnection.class);
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_DELETE, Site2SiteVpnConnection.class);
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_RESET, Site2SiteVpnConnection.class);

        // Custom certificates
        entityEventDetails.put(EVENT_UPLOAD_CUSTOM_CERTIFICATE, "Certificate");

        // OneToOnenat
        entityEventDetails.put(EVENT_ENABLE_STATIC_NAT, StaticNat.class);
        entityEventDetails.put(EVENT_DISABLE_STATIC_NAT, StaticNat.class);

        entityEventDetails.put(EVENT_ZONE_VLAN_ASSIGN, Vlan.class);
        entityEventDetails.put(EVENT_ZONE_VLAN_RELEASE, Vlan.class);

        // Projects
        entityEventDetails.put(EVENT_PROJECT_CREATE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_UPDATE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_DELETE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_ACTIVATE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_SUSPEND, Project.class);
        entityEventDetails.put(EVENT_PROJECT_ACCOUNT_ADD, Project.class);
        entityEventDetails.put(EVENT_PROJECT_INVITATION_UPDATE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_INVITATION_REMOVE, Project.class);
        entityEventDetails.put(EVENT_PROJECT_ACCOUNT_REMOVE, Project.class);

        // Network as a Service
        entityEventDetails.put(EVENT_NETWORK_ELEMENT_CONFIGURE, Network.class);

        // Physical Network Events
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_CREATE, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_DELETE, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_UPDATE, PhysicalNetwork.class);

        // Physical Network Service Provider Events
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_CREATE, PhysicalNetworkServiceProvider.class);
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_DELETE, PhysicalNetworkServiceProvider.class);
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_UPDATE, PhysicalNetworkServiceProvider.class);

        // Physical Network TrafficType Events
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_CREATE, PhysicalNetworkTrafficType.class);
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_DELETE, PhysicalNetworkTrafficType.class);
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_UPDATE, PhysicalNetworkTrafficType.class);

        // external network device events
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_ADD, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_DELETE, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_CONFIGURE, PhysicalNetwork.class);

        // external switch management device events (E.g.: Cisco Nexus 1000v Virtual Supervisor Module.
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ADD, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DELETE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_CONFIGURE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ENABLE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE, "Nexus1000v");

        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_ADD, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_DELETE, PhysicalNetwork.class);
        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_CONFIGURE, PhysicalNetwork.class);

        // Network ACL
        entityEventDetails.put(EVENT_NETWORK_ACL_CREATE, NetworkACL.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_DELETE, NetworkACL.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_REPLACE, NetworkACL.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_UPDATE, NetworkACL.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_ITEM_CREATE, NetworkACLItem.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_ITEM_UPDATE, NetworkACLItem.class);
        entityEventDetails.put(EVENT_NETWORK_ACL_ITEM_DELETE, NetworkACLItem.class);

        // VPC
        entityEventDetails.put(EVENT_VPC_CREATE, Vpc.class);
        entityEventDetails.put(EVENT_VPC_UPDATE, Vpc.class);
        entityEventDetails.put(EVENT_VPC_DELETE, Vpc.class);
        entityEventDetails.put(EVENT_VPC_RESTART, Vpc.class);

        // VPC offerings
        entityEventDetails.put(EVENT_VPC_OFFERING_CREATE, VpcOffering.class);
        entityEventDetails.put(EVENT_VPC_OFFERING_UPDATE, VpcOffering.class);
        entityEventDetails.put(EVENT_VPC_OFFERING_DELETE, VpcOffering.class);

        // Private gateway
        entityEventDetails.put(EVENT_PRIVATE_GATEWAY_CREATE, PrivateGateway.class);
        entityEventDetails.put(EVENT_PRIVATE_GATEWAY_DELETE, PrivateGateway.class);

        // Static routes
        entityEventDetails.put(EVENT_STATIC_ROUTE_CREATE, StaticRoute.class);
        entityEventDetails.put(EVENT_STATIC_ROUTE_DELETE, StaticRoute.class);

        // tag related events
        entityEventDetails.put(EVENT_TAGS_CREATE, ResourceTag.class);
        entityEventDetails.put(EVENT_TAGS_DELETE, ResourceTag.class);

        // external network device events
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_ADD, "NvpController");
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_DELETE, "NvpController");
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_CONFIGURE, "NvpController");

        // external network mapping events

        // AutoScale
        entityEventDetails.put(EVENT_COUNTER_CREATE, AutoScaleCounter.class);
        entityEventDetails.put(EVENT_COUNTER_DELETE, AutoScaleCounter.class);
        entityEventDetails.put(EVENT_CONDITION_CREATE, Condition.class);
        entityEventDetails.put(EVENT_CONDITION_DELETE, Condition.class);
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_CREATE, AutoScalePolicy.class);
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_UPDATE, AutoScalePolicy.class);
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_DELETE, AutoScalePolicy.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_CREATE, AutoScaleVmProfile.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_DELETE, AutoScaleVmProfile.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_UPDATE, AutoScaleVmProfile.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_CREATE, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_DELETE, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_UPDATE, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_ENABLE, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_DISABLE, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_SCALEDOWN, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_SCALEUP, AutoScaleVmGroup.class);
        entityEventDetails.put(EVENT_GUEST_VLAN_RANGE_DEDICATE, GuestVlan.class);
        entityEventDetails.put(EVENT_DEDICATED_GUEST_VLAN_RANGE_RELEASE, GuestVlan.class);

        entityEventDetails.put(EVENT_AFFINITY_GROUP_CREATE, AffinityGroup.class);
        entityEventDetails.put(EVENT_AFFINITY_GROUP_DELETE, AffinityGroup.class);
        entityEventDetails.put(EVENT_AFFINITY_GROUP_ASSIGN, AffinityGroup.class);
        entityEventDetails.put(EVENT_AFFINITY_GROUP_REMOVE, AffinityGroup.class);

        // OpenDaylight
        entityEventDetails.put(EVENT_EXTERNAL_OPENDAYLIGHT_ADD_CONTROLLER, "OpenDaylightController");
        entityEventDetails.put(EVENT_EXTERNAL_OPENDAYLIGHT_DELETE_CONTROLLER, "OpenDaylightController");
        entityEventDetails.put(EVENT_EXTERNAL_OPENDAYLIGHT_CONFIGURE_CONTROLLER, "OpenDaylightController");

        //Guest OS
        entityEventDetails.put(EVENT_GUEST_OS_ADD, GuestOS.class);
        entityEventDetails.put(EVENT_GUEST_OS_REMOVE, GuestOS.class);
        entityEventDetails.put(EVENT_GUEST_OS_UPDATE, GuestOS.class);
        entityEventDetails.put(EVENT_GUEST_OS_MAPPING_ADD, GuestOSHypervisor.class);
        entityEventDetails.put(EVENT_GUEST_OS_MAPPING_REMOVE, GuestOSHypervisor.class);
        entityEventDetails.put(EVENT_GUEST_OS_MAPPING_UPDATE, GuestOSHypervisor.class);
        entityEventDetails.put(EVENT_NIC_SECONDARY_IP_ASSIGN, NicSecondaryIp.class);
        entityEventDetails.put(EVENT_NIC_SECONDARY_IP_UNASSIGN, NicSecondaryIp.class);
        entityEventDetails.put(EVENT_NIC_SECONDARY_IP_CONFIGURE, NicSecondaryIp.class);

        //Usage
        entityEventDetails.put(EVENT_USAGE_REMOVE_USAGE_RECORDS, Usage.class);
        // Netscaler Service Packages
        entityEventDetails.put(EVENT_NETSCALER_SERVICEPACKAGE_ADD, "NETSCALER.SERVICEPACKAGE.CREATE");
        entityEventDetails.put(EVENT_NETSCALER_SERVICEPACKAGE_DELETE, "NETSCALER.SERVICEPACKAGE.DELETE");

        entityEventDetails.put(EVENT_ANNOTATION_CREATE, Annotation.class);
        entityEventDetails.put(EVENT_ANNOTATION_REMOVE, Annotation.class);

        entityEventDetails.put(EVENT_TEMPLATE_DIRECT_DOWNLOAD_FAILURE, VirtualMachineTemplate.class);
        entityEventDetails.put(EVENT_ISO_DIRECT_DOWNLOAD_FAILURE, "Iso");
        entityEventDetails.put(EVENT_SYSTEM_VM_DIAGNOSTICS, VirtualMachine.class);

        entityEventDetails.put(EVENT_ZONE_ROLLING_MAINTENANCE, ZoneResponse.class);
        entityEventDetails.put(EVENT_POD_ROLLING_MAINTENANCE, PodResponse.class);
        entityEventDetails.put(EVENT_CLUSTER_ROLLING_MAINTENANCE, ClusterResponse.class);
        entityEventDetails.put(EVENT_HOST_ROLLING_MAINTENANCE, HostResponse.class);

        entityEventDetails.put(EVENT_IMPORT_VCENTER_STORAGE_POLICIES, "StoragePolicies");

        entityEventDetails.put(EVENT_IMAGE_STORE_DATA_MIGRATE, ImageStore.class);
        entityEventDetails.put(EVENT_LIVE_PATCH_SYSTEMVM, "SystemVMs");
    }

    public static String getEntityForEvent(String eventName) {
        Object entityClass = entityEventDetails.get(eventName);
        if (entityClass == null) {
            return null;
        } else if (entityClass instanceof String){
            return (String)entityClass;
        } else if (entityClass instanceof Class){
            String entityClassName = ((Class)entityClass).getName();
            int index = entityClassName.lastIndexOf(".");
            String entityName = entityClassName;
            if (index != -1) {
                entityName = entityClassName.substring(index + 1);
            }
            return entityName;
        }

        return null;
    }

    public static Class getEntityClassForEvent(String eventName) {
        Object clz = entityEventDetails.get(eventName);

        if(clz instanceof Class){
            return (Class)entityEventDetails.get(eventName);
        }

        return null;
    }
}
