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

import com.cloud.configuration.Configuration;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.host.Host;
import com.cloud.network.*;
import com.cloud.network.as.*;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.vm.VirtualMachine;

import java.util.HashMap;
import java.util.Map;

public class EventTypes {

    //map of Event and corresponding entity for which Event is applicable
    private static Map<String, String> entityEventDetails = null;

    // VM Events
    public static final String EVENT_VM_CREATE = "VM.CREATE";
    public static final String EVENT_VM_DESTROY = "VM.DESTROY";
    public static final String EVENT_VM_START = "VM.START";
    public static final String EVENT_VM_STOP = "VM.STOP";
    public static final String EVENT_VM_REBOOT = "VM.REBOOT";
    public static final String EVENT_VM_UPDATE = "VM.UPDATE";
    public static final String EVENT_VM_UPGRADE = "VM.UPGRADE";
    public static final String EVENT_VM_RESETPASSWORD = "VM.RESETPASSWORD";
    public static final String EVENT_VM_RESETSSHKEY = "VM.RESETSSHKEY";
    public static final String EVENT_VM_MIGRATE = "VM.MIGRATE";
    public static final String EVENT_VM_MOVE = "VM.MOVE";
    public static final String EVENT_VM_RESTORE = "VM.RESTORE";

    // Domain Router
    public static final String EVENT_ROUTER_CREATE = "ROUTER.CREATE";
    public static final String EVENT_ROUTER_DESTROY = "ROUTER.DESTROY";
    public static final String EVENT_ROUTER_START = "ROUTER.START";
    public static final String EVENT_ROUTER_STOP = "ROUTER.STOP";
    public static final String EVENT_ROUTER_REBOOT = "ROUTER.REBOOT";
    public static final String EVENT_ROUTER_HA = "ROUTER.HA";
    public static final String EVENT_ROUTER_UPGRADE = "ROUTER.UPGRADE";

    // Console proxy
    public static final String EVENT_PROXY_CREATE = "PROXY.CREATE";
    public static final String EVENT_PROXY_DESTROY = "PROXY.DESTROY";
    public static final String EVENT_PROXY_START = "PROXY.START";
    public static final String EVENT_PROXY_STOP = "PROXY.STOP";
    public static final String EVENT_PROXY_REBOOT = "PROXY.REBOOT";
    public static final String EVENT_PROXY_HA = "PROXY.HA";

    // VNC Console Events
    public static final String EVENT_VNC_CONNECT = "VNC.CONNECT";
    public static final String EVENT_VNC_DISCONNECT = "VNC.DISCONNECT";

    // Network Events
    public static final String EVENT_NET_IP_ASSIGN = "NET.IPASSIGN";
    public static final String EVENT_NET_IP_RELEASE = "NET.IPRELEASE";
    public static final String EVENT_NET_RULE_ADD = "NET.RULEADD";
    public static final String EVENT_NET_RULE_DELETE = "NET.RULEDELETE";
    public static final String EVENT_NET_RULE_MODIFY = "NET.RULEMODIFY";
    public static final String EVENT_NETWORK_CREATE = "NETWORK.CREATE";
    public static final String EVENT_NETWORK_DELETE = "NETWORK.DELETE";
    public static final String EVENT_NETWORK_UPDATE = "NETWORK.UPDATE";
    public static final String EVENT_FIREWALL_OPEN = "FIREWALL.OPEN";
    public static final String EVENT_FIREWALL_CLOSE = "FIREWALL.CLOSE";

    //NIC Events
    public static final String EVENT_NIC_CREATE = "NIC.CREATE";
    public static final String EVENT_NIC_DELETE = "NIC.DELETE";
    public static final String EVENT_NIC_UPDATE = "NIC.UPDATE";

    // Load Balancers
    public static final String EVENT_ASSIGN_TO_LOAD_BALANCER_RULE = "LB.ASSIGN.TO.RULE";
    public static final String EVENT_REMOVE_FROM_LOAD_BALANCER_RULE = "LB.REMOVE.FROM.RULE";
    public static final String EVENT_LOAD_BALANCER_CREATE = "LB.CREATE";
    public static final String EVENT_LOAD_BALANCER_DELETE = "LB.DELETE";
    public static final String EVENT_LB_STICKINESSPOLICY_CREATE = "LB.STICKINESSPOLICY.CREATE";
    public static final String EVENT_LB_STICKINESSPOLICY_DELETE = "LB.STICKINESSPOLICY.DELETE";
    public static final String EVENT_LB_HEALTHCHECKPOLICY_CREATE = "LB.HEALTHCHECKPOLICY.CREATE";
    public static final String EVENT_LB_HEALTHCHECKPOLICY_DELETE = "LB.HEALTHCHECKPOLICY.DELETE";
    public static final String EVENT_LOAD_BALANCER_UPDATE = "LB.UPDATE";

    // Account events
    public static final String EVENT_ACCOUNT_DISABLE = "ACCOUNT.DISABLE";
    public static final String EVENT_ACCOUNT_CREATE = "ACCOUNT.CREATE";
    public static final String EVENT_ACCOUNT_DELETE = "ACCOUNT.DELETE";
    public static final String EVENT_ACCOUNT_MARK_DEFAULT_ZONE = "ACCOUNT.MARK.DEFAULT.ZONE";

    // UserVO Events
    public static final String EVENT_USER_LOGIN = "USER.LOGIN";
    public static final String EVENT_USER_LOGOUT = "USER.LOGOUT";
    public static final String EVENT_USER_CREATE = "USER.CREATE";
    public static final String EVENT_USER_DELETE = "USER.DELETE";
    public static final String EVENT_USER_DISABLE = "USER.DISABLE";
    public static final String EVENT_USER_UPDATE = "USER.UPDATE";
    public static final String EVENT_USER_ENABLE = "USER.ENABLE";
    public static final String EVENT_USER_LOCK = "USER.LOCK";

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

    // Volume Events
    public static final String EVENT_VOLUME_CREATE = "VOLUME.CREATE";
    public static final String EVENT_VOLUME_DELETE = "VOLUME.DELETE";
    public static final String EVENT_VOLUME_ATTACH = "VOLUME.ATTACH";
    public static final String EVENT_VOLUME_DETACH = "VOLUME.DETACH";
    public static final String EVENT_VOLUME_EXTRACT = "VOLUME.EXTRACT";
    public static final String EVENT_VOLUME_UPLOAD = "VOLUME.UPLOAD";
    public static final String EVENT_VOLUME_MIGRATE = "VOLUME.MIGRATE";
    public static final String EVENT_VOLUME_RESIZE = "VOLUME.RESIZE";

    // Domains
    public static final String EVENT_DOMAIN_CREATE = "DOMAIN.CREATE";
    public static final String EVENT_DOMAIN_DELETE = "DOMAIN.DELETE";
    public static final String EVENT_DOMAIN_UPDATE = "DOMAIN.UPDATE";

    // Snapshots
    public static final String EVENT_SNAPSHOT_CREATE = "SNAPSHOT.CREATE";
    public static final String EVENT_SNAPSHOT_DELETE = "SNAPSHOT.DELETE";
    public static final String EVENT_SNAPSHOT_POLICY_CREATE = "SNAPSHOTPOLICY.CREATE";
    public static final String EVENT_SNAPSHOT_POLICY_UPDATE = "SNAPSHOTPOLICY.UPDATE";
    public static final String EVENT_SNAPSHOT_POLICY_DELETE = "SNAPSHOTPOLICY.DELETE";

    // ISO
    public static final String EVENT_ISO_CREATE = "ISO.CREATE";
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

    public static final String EVENT_STORAGE_IP_RANGE_CREATE = "STORAGE.IP.RANGE.CREATE";
    public static final String EVENT_STORAGE_IP_RANGE_DELETE = "STORAGE.IP.RANGE.DELETE";
    public static final String EVENT_STORAGE_IP_RANGE_UPDATE = "STORAGE.IP.RANGE.UPDATE";

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

    // Host
    public static final String EVENT_HOST_RECONNECT = "HOST.RECONNECT";

    // Maintenance
    public static final String EVENT_MAINTENANCE_CANCEL = "MAINT.CANCEL";
    public static final String EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE = "MAINT.CANCEL.PS";
    public static final String EVENT_MAINTENANCE_PREPARE = "MAINT.PREPARE";
    public static final String EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE = "MAINT.PREPARE.PS";

    // VPN
    public static final String EVENT_REMOTE_ACCESS_VPN_CREATE = "VPN.REMOTE.ACCESS.CREATE";
    public static final String EVENT_REMOTE_ACCESS_VPN_DESTROY = "VPN.REMOTE.ACCESS.DESTROY";
    public static final String EVENT_VPN_USER_ADD = "VPN.USER.ADD";
    public static final String EVENT_VPN_USER_REMOVE = "VPN.USER.REMOVE";
    public static final String EVENT_S2S_VPN_GATEWAY_CREATE = "VPN.S2S.VPN.GATEWAY.CREATE";
    public static final String EVENT_S2S_VPN_GATEWAY_DELETE = "VPN.S2S.VPN.GATEWAY.DELETE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE = "VPN.S2S.CUSTOMER.GATEWAY.CREATE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE = "VPN.S2S.CUSTOMER.GATEWAY.DELETE";
    public static final String EVENT_S2S_VPN_CUSTOMER_GATEWAY_UPDATE = "VPN.S2S.CUSTOMER.GATEWAY.UPDATE";
    public static final String EVENT_S2S_VPN_CONNECTION_CREATE = "VPN.S2S.CONNECTION.CREATE";
    public static final String EVENT_S2S_VPN_CONNECTION_DELETE = "VPN.S2S.CONNECTION.DELETE";
    public static final String EVENT_S2S_VPN_CONNECTION_RESET = "VPN.S2S.CONNECTION.RESET";

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
    public static final String EVENT_PROJECT_INVITATION_UPDATE = "PROJECT.INVITATION.UPDATE";
    public static final String EVENT_PROJECT_INVITATION_REMOVE = "PROJECT.INVITATION.REMOVE";
    public static final String EVENT_PROJECT_ACCOUNT_REMOVE = "PROJECT.ACCOUNT.REMOVE";

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
    
	// vm snapshot events
    public static final String EVENT_VM_SNAPSHOT_CREATE = "VMSNAPSHOT.CREATE";
    public static final String EVENT_VM_SNAPSHOT_DELETE = "VMSNAPSHOT.DELETE";
    public static final String EVENT_VM_SNAPSHOT_REVERT = "VMSNAPSHOT.REVERTTO";

    // external network device events
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_ADD = "PHYSICAL.NVPCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_DELETE = "PHYSICAL.NVPCONTROLLER.DELETE";
    public static final String EVENT_EXTERNAL_NVP_CONTROLLER_CONFIGURE = "PHYSICAL.NVPCONTROLLER.CONFIGURE";

    // AutoScale
    public static final String EVENT_COUNTER_CREATE = "COUNTER.CREATE";
    public static final String EVENT_COUNTER_DELETE = "COUNTER.DELETE";
    public static final String EVENT_CONDITION_CREATE = "CONDITION.CREATE";
    public static final String EVENT_CONDITION_DELETE = "CONDITION.DELETE";
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


    public static final String EVENT_BAREMETAL_DHCP_SERVER_ADD = "PHYSICAL.DHCP.ADD";
    public static final String EVENT_BAREMETAL_DHCP_SERVER_DELETE = "PHYSICAL.DHCP.DELETE";
    public static final String EVENT_BAREMETAL_PXE_SERVER_ADD = "PHYSICAL.PXE.ADD";
    public static final String EVENT_BAREMETAL_PXE_SERVER_DELETE = "PHYSICAL.PXE.DELETE";

    static {

        // TODO: need a way to force author adding event types to declare the entity details as well, with out braking
        // current ActionEvent annotation semantics

        entityEventDetails = new HashMap<String, String>();

        entityEventDetails.put(EVENT_VM_CREATE, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_DESTROY, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_START, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_STOP, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_REBOOT, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_UPDATE, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_UPGRADE, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_RESETPASSWORD, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_MIGRATE, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_MOVE, VirtualMachine.class.getName());
        entityEventDetails.put(EVENT_VM_RESTORE, VirtualMachine.class.getName());

        entityEventDetails.put(EVENT_ROUTER_CREATE, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_DESTROY, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_START, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_STOP, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_REBOOT, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_HA, VirtualRouter.class.getName());
        entityEventDetails.put(EVENT_ROUTER_UPGRADE, VirtualRouter.class.getName());

        entityEventDetails.put(EVENT_PROXY_CREATE, "ConsoleProxy");
        entityEventDetails.put(EVENT_PROXY_DESTROY, "ConsoleProxy");
        entityEventDetails.put(EVENT_PROXY_START, "ConsoleProxy");
        entityEventDetails.put(EVENT_PROXY_STOP, "ConsoleProxy");
        entityEventDetails.put(EVENT_PROXY_REBOOT, "ConsoleProxy");
        entityEventDetails.put(EVENT_ROUTER_HA, "ConsoleProxy");
        entityEventDetails.put(EVENT_PROXY_HA, "ConsoleProxy");

        entityEventDetails.put(EVENT_VNC_CONNECT, "VNC");
        entityEventDetails.put(EVENT_VNC_DISCONNECT, "VNC");

        // Network Events
        entityEventDetails.put(EVENT_NETWORK_CREATE, Network.class.getName());
        entityEventDetails.put(EVENT_NETWORK_DELETE, Network.class.getName());
        entityEventDetails.put(EVENT_NETWORK_UPDATE, Network.class.getName());
        entityEventDetails.put(EVENT_NETWORK_RESTART, Network.class.getName());
        entityEventDetails.put(EVENT_NET_IP_ASSIGN, PublicIpAddress.class.getName());
        entityEventDetails.put(EVENT_NET_IP_RELEASE, PublicIpAddress.class.getName());
        entityEventDetails.put(EVENT_NET_RULE_ADD, Network.class.getName());
        entityEventDetails.put(EVENT_NET_RULE_DELETE, Network.class.getName());
        entityEventDetails.put(EVENT_NET_RULE_MODIFY, Network.class.getName());
        entityEventDetails.put(EVENT_FIREWALL_OPEN, Network.class.getName());
        entityEventDetails.put(EVENT_FIREWALL_CLOSE, Network.class.getName());

        // Load Balancers
        entityEventDetails.put(EVENT_ASSIGN_TO_LOAD_BALANCER_RULE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_REMOVE_FROM_LOAD_BALANCER_RULE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_LOAD_BALANCER_CREATE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_LOAD_BALANCER_DELETE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_LB_STICKINESSPOLICY_CREATE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_LB_STICKINESSPOLICY_DELETE, LoadBalancer.class.getName());
        entityEventDetails.put(EVENT_LOAD_BALANCER_UPDATE, LoadBalancer.class.getName());

        // Account events
        entityEventDetails.put(EVENT_ACCOUNT_DISABLE, Account.class.getName());
        entityEventDetails.put(EVENT_ACCOUNT_CREATE, Account.class.getName());
        entityEventDetails.put(EVENT_ACCOUNT_DELETE, Account.class.getName());
        entityEventDetails.put(EVENT_ACCOUNT_MARK_DEFAULT_ZONE, Account.class.getName());

        // UserVO Events
        entityEventDetails.put(EVENT_USER_LOGIN, User.class.getName());
        entityEventDetails.put(EVENT_USER_LOGOUT, User.class.getName());
        entityEventDetails.put(EVENT_USER_CREATE, User.class.getName());
        entityEventDetails.put(EVENT_USER_DELETE, User.class.getName());
        entityEventDetails.put(EVENT_USER_DISABLE, User.class.getName());
        entityEventDetails.put(EVENT_USER_UPDATE, User.class.getName());
        entityEventDetails.put(EVENT_USER_ENABLE, User.class.getName());
        entityEventDetails.put(EVENT_USER_LOCK, User.class.getName());

        // Template Events
        entityEventDetails.put(EVENT_TEMPLATE_CREATE, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_DELETE, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_UPDATE, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_START, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_SUCCESS, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_DOWNLOAD_FAILED, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_COPY, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_EXTRACT, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_UPLOAD, VirtualMachineTemplate.class.getName());
        entityEventDetails.put(EVENT_TEMPLATE_CLEANUP, VirtualMachineTemplate.class.getName());

        // Volume Events
        entityEventDetails.put(EVENT_VOLUME_CREATE, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_DELETE, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_ATTACH, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_DETACH, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_EXTRACT, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_UPLOAD, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_MIGRATE, Volume.class.getName());
        entityEventDetails.put(EVENT_VOLUME_RESIZE, Volume.class.getName());

        // Domains
        entityEventDetails.put(EVENT_DOMAIN_CREATE, Domain.class.getName());
        entityEventDetails.put(EVENT_DOMAIN_DELETE, Domain.class.getName());
        entityEventDetails.put(EVENT_DOMAIN_UPDATE, Domain.class.getName());

        // Snapshots
        entityEventDetails.put(EVENT_SNAPSHOT_CREATE, Snapshot.class.getName());
        entityEventDetails.put(EVENT_SNAPSHOT_DELETE, Snapshot.class.getName());
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_CREATE, Snapshot.class.getName());
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_UPDATE, Snapshot.class.getName());
        entityEventDetails.put(EVENT_SNAPSHOT_POLICY_DELETE, Snapshot.class.getName());

        // ISO
        entityEventDetails.put(EVENT_ISO_CREATE, "Iso");
        entityEventDetails.put(EVENT_ISO_DELETE, "Iso");
        entityEventDetails.put(EVENT_ISO_COPY, "Iso");
        entityEventDetails.put(EVENT_ISO_ATTACH, "Iso");
        entityEventDetails.put(EVENT_ISO_DETACH, "Iso");
        entityEventDetails.put(EVENT_ISO_EXTRACT, "Iso");
        entityEventDetails.put(EVENT_ISO_UPLOAD, "Iso");

        // SSVM
        entityEventDetails.put(EVENT_SSVM_CREATE, "SecondaryStorageVm");
        entityEventDetails.put(EVENT_SSVM_DESTROY, "SecondaryStorageVm");
        entityEventDetails.put(EVENT_SSVM_START, "SecondaryStorageVm");
        entityEventDetails.put(EVENT_SSVM_STOP, "SecondaryStorageVm");
        entityEventDetails.put(EVENT_SSVM_REBOOT, "SecondaryStorageVm");
        entityEventDetails.put(EVENT_SSVM_HA, "SecondaryStorageVm");

        // Service Offerings
        entityEventDetails.put(EVENT_SERVICE_OFFERING_CREATE, ServiceOffering.class.getName());
        entityEventDetails.put(EVENT_SERVICE_OFFERING_EDIT, ServiceOffering.class.getName());
        entityEventDetails.put(EVENT_SERVICE_OFFERING_DELETE, ServiceOffering.class.getName());

        // Disk Offerings
        entityEventDetails.put(EVENT_DISK_OFFERING_CREATE, DiskOffering.class.getName());
        entityEventDetails.put(EVENT_DISK_OFFERING_EDIT, DiskOffering.class.getName());
        entityEventDetails.put(EVENT_DISK_OFFERING_DELETE, DiskOffering.class.getName());

        // Network offerings
        entityEventDetails.put(EVENT_NETWORK_OFFERING_CREATE, NetworkOffering.class.getName());
        entityEventDetails.put(EVENT_NETWORK_OFFERING_ASSIGN, NetworkOffering.class.getName());
        entityEventDetails.put(EVENT_NETWORK_OFFERING_EDIT, NetworkOffering.class.getName());
        entityEventDetails.put(EVENT_NETWORK_OFFERING_REMOVE, NetworkOffering.class.getName());
        entityEventDetails.put(EVENT_NETWORK_OFFERING_DELETE, NetworkOffering.class.getName());

        // Pods
        entityEventDetails.put(EVENT_POD_CREATE, Pod.class.getName());
        entityEventDetails.put(EVENT_POD_EDIT, Pod.class.getName());
        entityEventDetails.put(EVENT_POD_DELETE, Pod.class.getName());

        // Zones
        entityEventDetails.put(EVENT_ZONE_CREATE, DataCenter.class.getName());
        entityEventDetails.put(EVENT_ZONE_EDIT, DataCenter.class.getName());
        entityEventDetails.put(EVENT_ZONE_DELETE, DataCenter.class.getName());

        // VLANs/IP ranges
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_CREATE, Vlan.class.getName());
        entityEventDetails.put(EVENT_VLAN_IP_RANGE_DELETE,Vlan.class.getName());

        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_CREATE, StorageNetworkIpRange.class.getName());
        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_DELETE, StorageNetworkIpRange.class.getName());
        entityEventDetails.put(EVENT_STORAGE_IP_RANGE_UPDATE, StorageNetworkIpRange.class.getName());

        // Configuration Table
        entityEventDetails.put(EVENT_CONFIGURATION_VALUE_EDIT, Configuration.class.getName());

        // Security Groups
        entityEventDetails.put(EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_REVOKE_INGRESS, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_AUTHORIZE_EGRESS, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_REVOKE_EGRESS, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_CREATE, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_DELETE, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_ASSIGN, SecurityGroup.class.getName());
        entityEventDetails.put(EVENT_SECURITY_GROUP_REMOVE, SecurityGroup.class.getName());

        // Host
        entityEventDetails.put(EVENT_HOST_RECONNECT,  Host.class.getName());

        // Maintenance
        entityEventDetails.put(EVENT_MAINTENANCE_CANCEL,  Host.class.getName());
        entityEventDetails.put(EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE,  Host.class.getName());
        entityEventDetails.put(EVENT_MAINTENANCE_PREPARE,  Host.class.getName());
        entityEventDetails.put(EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE,  Host.class.getName());

        // VPN
        entityEventDetails.put(EVENT_REMOTE_ACCESS_VPN_CREATE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_REMOTE_ACCESS_VPN_DESTROY, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_VPN_USER_ADD, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_VPN_USER_REMOVE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_GATEWAY_CREATE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_GATEWAY_DELETE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CUSTOMER_GATEWAY_UPDATE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_CREATE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_DELETE, RemoteAccessVpn.class.getName());
        entityEventDetails.put(EVENT_S2S_VPN_CONNECTION_RESET, RemoteAccessVpn.class.getName());

        // Custom certificates
        entityEventDetails.put(EVENT_UPLOAD_CUSTOM_CERTIFICATE, "Certificate");

        // OneToOnenat
        entityEventDetails.put(EVENT_ENABLE_STATIC_NAT, StaticNat.class.getName());
        entityEventDetails.put(EVENT_DISABLE_STATIC_NAT, StaticNat.class.getName());

        entityEventDetails.put(EVENT_ZONE_VLAN_ASSIGN,Vlan.class.getName());
        entityEventDetails.put(EVENT_ZONE_VLAN_RELEASE,Vlan.class.getName());

        // Projects
        entityEventDetails.put(EVENT_PROJECT_CREATE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_UPDATE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_DELETE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_ACTIVATE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_SUSPEND, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_ACCOUNT_ADD, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_INVITATION_UPDATE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_INVITATION_REMOVE, Project.class.getName());
        entityEventDetails.put(EVENT_PROJECT_ACCOUNT_REMOVE, Project.class.getName());

        // Network as a Service
        entityEventDetails.put(EVENT_NETWORK_ELEMENT_CONFIGURE,Network.class.getName());

        // Physical Network Events
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_CREATE, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_DELETE, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_PHYSICAL_NETWORK_UPDATE, PhysicalNetwork.class.getName());

        // Physical Network Service Provider Events
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_CREATE, PhysicalNetworkServiceProvider.class.getName());
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_DELETE, PhysicalNetworkServiceProvider.class.getName());
        entityEventDetails.put(EVENT_SERVICE_PROVIDER_UPDATE, PhysicalNetworkServiceProvider.class.getName());

        // Physical Network TrafficType Events
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_CREATE, PhysicalNetworkTrafficType.class.getName());
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_DELETE, PhysicalNetworkTrafficType.class.getName());
        entityEventDetails.put(EVENT_TRAFFIC_TYPE_UPDATE, PhysicalNetworkTrafficType.class.getName());

        // external network device events
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_ADD, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_DELETE, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_EXTERNAL_LB_DEVICE_CONFIGURE, PhysicalNetwork.class.getName());

        // external switch management device events (E.g.: Cisco Nexus 1000v Virtual Supervisor Module.
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ADD, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DELETE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_CONFIGURE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ENABLE, "Nexus1000v");
        entityEventDetails.put(EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE, "Nexus1000v");


        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_ADD, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_DELETE, PhysicalNetwork.class.getName());
        entityEventDetails.put(EVENT_EXTERNAL_FIREWALL_DEVICE_CONFIGURE, PhysicalNetwork.class.getName());

        // VPC
        entityEventDetails.put(EVENT_VPC_CREATE, Vpc.class.getName());
        entityEventDetails.put(EVENT_VPC_UPDATE, Vpc.class.getName());
        entityEventDetails.put(EVENT_VPC_DELETE, Vpc.class.getName());
        entityEventDetails.put(EVENT_VPC_RESTART, Vpc.class.getName());

        // VPC offerings
        entityEventDetails.put(EVENT_VPC_OFFERING_CREATE, Vpc.class.getName());
        entityEventDetails.put(EVENT_VPC_OFFERING_UPDATE, Vpc.class.getName());
        entityEventDetails.put(EVENT_VPC_OFFERING_DELETE, Vpc.class.getName());

        // Private gateway
        entityEventDetails.put(EVENT_PRIVATE_GATEWAY_CREATE, PrivateGateway.class.getName());
        entityEventDetails.put(EVENT_PRIVATE_GATEWAY_DELETE, PrivateGateway.class.getName());

        // Static routes
        entityEventDetails.put(EVENT_STATIC_ROUTE_CREATE, StaticRoute.class.getName());
        entityEventDetails.put(EVENT_STATIC_ROUTE_DELETE, StaticRoute.class.getName());

        // tag related events
        entityEventDetails.put(EVENT_TAGS_CREATE, "Tag");
        entityEventDetails.put(EVENT_TAGS_DELETE, "tag");

        // external network device events
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_ADD,  "NvpController");
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_DELETE,  "NvpController");
        entityEventDetails.put(EVENT_EXTERNAL_NVP_CONTROLLER_CONFIGURE, "NvpController");

        // AutoScale
        entityEventDetails.put(EVENT_COUNTER_CREATE, AutoScaleCounter.class.getName());
        entityEventDetails.put(EVENT_COUNTER_DELETE, AutoScaleCounter.class.getName());
        entityEventDetails.put(EVENT_CONDITION_CREATE, Condition.class.getName());
        entityEventDetails.put(EVENT_CONDITION_DELETE, Condition.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_CREATE, AutoScalePolicy.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_UPDATE, AutoScalePolicy.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEPOLICY_DELETE, AutoScalePolicy.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_CREATE, AutoScaleVmProfile.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_DELETE, AutoScaleVmProfile.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMPROFILE_UPDATE, AutoScaleVmProfile.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_CREATE, AutoScaleVmGroup.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_DELETE, AutoScaleVmGroup.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_UPDATE, AutoScaleVmGroup.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_ENABLE, AutoScaleVmGroup.class.getName());
        entityEventDetails.put(EVENT_AUTOSCALEVMGROUP_DISABLE, AutoScaleVmGroup.class.getName());
    }

    public static String getEntityForEvent (String eventName) {
        String entityClassName = entityEventDetails.get(eventName);
        if (entityClassName == null || entityClassName.isEmpty()) {
            return null;
        }
        int index = entityClassName.lastIndexOf(".");
        String entityName = entityClassName;
        if (index != -1) {
            entityName = entityClassName.substring(index+1);
        }
        return entityName;
    }
}
