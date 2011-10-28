/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.event;

public class EventTypes {
	// VM Events
	public static final String EVENT_VM_CREATE = "VM.CREATE";
	public static final String EVENT_VM_DESTROY = "VM.DESTROY";
	public static final String EVENT_VM_START = "VM.START";
	public static final String EVENT_VM_STOP = "VM.STOP";
	public static final String EVENT_VM_REBOOT = "VM.REBOOT";
    public static final String EVENT_VM_UPDATE = "VM.UPDATE";	
	public static final String EVENT_VM_UPGRADE = "VM.UPGRADE";
	public static final String EVENT_VM_RESETPASSWORD = "VM.RESETPASSWORD";
	public static final String EVENT_VM_MIGRATE = "VM.MIGRATE";
    public static final String EVENT_VM_MOVE = "VM.MOVE";
    
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
    
    // Load Balancers
    public static final String EVENT_ASSIGN_TO_LOAD_BALANCER_RULE = "LB.ASSIGN.TO.RULE";
    public static final String EVENT_REMOVE_FROM_LOAD_BALANCER_RULE = "LB.REMOVE.FROM.RULE";
    public static final String EVENT_LOAD_BALANCER_CREATE = "LB.CREATE";
    public static final String EVENT_LOAD_BALANCER_DELETE = "LB.DELETE";
    public static final String EVENT_LOAD_BALANCER_UPDATE = "LB.UPDATE";

    // Account events
    public static final String EVENT_ACCOUNT_DISABLE = "ACCOUNT.DISABLE";
    public static final String EVENT_ACCOUNT_CREATE = "ACCOUNT.CREATE";
    public static final String EVENT_ACCOUNT_DELETE = "ACCOUNT.DELETE";
    
	// UserVO Events
	public static final String EVENT_USER_LOGIN = "USER.LOGIN";
	public static final String EVENT_USER_LOGOUT = "USER.LOGOUT";
	public static final String EVENT_USER_CREATE = "USER.CREATE";
	public static final String EVENT_USER_DELETE = "USER.DELETE";
    public static final String EVENT_USER_DISABLE = "USER.DISABLE";
	public static final String EVENT_USER_UPDATE = "USER.UPDATE";
	public static final String EVENT_USER_ENABLE = "USER.ENABLE";
	public static final String EVENT_USER_LOCK = "USER.LOCK";

	//Template Events
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

    //SSVM
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
    
    //Network offerings
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

	// Configuration Table
	public static final String EVENT_CONFIGURATION_VALUE_EDIT = "CONFIGURATION.VALUE.EDIT";

	// Security Groups
	public static final String EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS = "SG.AUTH.INGRESS";
    public static final String EVENT_SECURITY_GROUP_REVOKE_INGRESS = "SG.REVOKE.INGRESS";
    public static final String EVENT_SECURITY_GROUP_CREATE = "SG.CREATE";
    public static final String EVENT_SECURITY_GROUP_DELETE = "SG.DELETE";

    // Host
    public static final String EVENT_HOST_RECONNECT = "HOST.RECONNECT";

    // Maintenance
    public static final String EVENT_MAINTENANCE_CANCEL = "MAINT.CANCEL";
    public static final String EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE = "MAINT.CANCEL.PS";
    public static final String EVENT_MAINTENANCE_PREPARE = "MAINT.PREPARE";
    public static final String EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE = "MAINT.PREPARE.PS";
    
    //VPN
    public static final String EVENT_REMOTE_ACCESS_VPN_CREATE = "VPN.REMOTE.ACCESS.CREATE";
    public static final String EVENT_REMOTE_ACCESS_VPN_DESTROY = "VPN.REMOTE.ACCESS.DESTROY";
    public static final String EVENT_VPN_USER_ADD = "VPN.USER.ADD";
    public static final String EVENT_VPN_USER_REMOVE = "VPN.USER.REMOVE";
    
    //Network
    public static final String EVENT_NETWORK_RESTART = "NETWORK.RESTART";
    
    //Custom certificates
	public static final String EVENT_UPLOAD_CUSTOM_CERTIFICATE = "UPLOAD.CUSTOM.CERTIFICATE";
	
	//OneToOnenat
	public static final String EVENT_ENABLE_STATIC_NAT = "STATICNAT.ENABLE";
	public static final String EVENT_DISABLE_STATIC_NAT = "STATICNAT.DISABLE";
	
	public static final String EVENT_ZONE_VLAN_ASSIGN = "ZONE.VLAN.ASSIGN";
	public static final String EVENT_ZONE_VLAN_RELEASE = "ZONE.VLAN.RELEASE";
	
	//Projects
	public static final String EVENT_PROJECT_CREATE = "PROJECT.CREATE";
	public static final String EVENT_PROJECT_UPDATE = "PROJECT.UPDATE";
	public static final String EVENT_PROJECT_DELETE = "PROJECT.DELETE";
	public static final String EVENT_PROJECT_ACTIVATE = "PROJECT.ACTIVATE";
	public static final String EVENT_PROJECT_SUSPEND = "PROJECT.SUSPEND";
	public static final String EVENT_PROJECT_ACCOUNT_ADD = "PROJECT.ACCOUNT.ADD";
	public static final String EVENT_PROJECT_ACCOUNT_REMOVE = "PROJECT.ACCOUNT.REMOVE";
	
	//Network as a Service
	public static final String EVENT_NETWORK_ELEMENT_CONFIGURE = "NETWORK.ELEMENT.CONFIGURE";

	//Physical Network Events
    public static final String EVENT_PHYSICAL_NETWORK_CREATE = "PHYSICAL.NETWORK.CREATE";
    public static final String EVENT_PHYSICAL_NETWORK_DELETE = "PHYSICAL.NETWORK.DELETE";
    public static final String EVENT_PHYSICAL_NETWORK_UPDATE = "PHYSICAL.NETWORK.UPDATE";
    
    //Physical Network Service Provider Events
    public static final String EVENT_SERVICE_PROVIDER_CREATE = "SERVICE.PROVIDER.CREATE";
    public static final String EVENT_SERVICE_PROVIDER_DELETE = "SERVICE.PROVIDER.DELETE";
    public static final String EVENT_SERVICE_PROVIDER_UPDATE = "SERVICE.PROVIDER.UPDATE";

    //Physical Network TrafficType Events
    public static final String EVENT_TRAFFIC_TYPE_CREATE = "TRAFFIC.TYPE.CREATE";
    public static final String EVENT_TRAFFIC_TYPE_DELETE = "TRAFFIC.TYPE.DELETE";
    public static final String EVENT_TRAFFIC_TYPE_UPDATE = "TRAFFIC.TYPE.UPDATE";

}
