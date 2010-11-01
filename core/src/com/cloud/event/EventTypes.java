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
    public static final String EVENT_VM_DISABLE_HA = "VM.DISABLEHA";
	public static final String EVENT_VM_ENABLE_HA = "VM.ENABLEHA";
	public static final String EVENT_VM_UPGRADE = "VM.UPGRADE";
	public static final String EVENT_VM_RESETPASSWORD = "VM.RESETPASSWORD";

	// Domain Router
	public static final String EVENT_ROUTER_CREATE = "ROUTER.CREATE";
	public static final String EVENT_ROUTER_DESTROY = "ROUTER.DESTROY";
	public static final String EVENT_ROUTER_START = "ROUTER.START";
	public static final String EVENT_ROUTER_STOP = "ROUTER.STOP";
	public static final String EVENT_ROUTER_REBOOT = "ROUTER.REBOOT";
	public static final String EVENT_ROUTER_HA = "ROUTER.HA";

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

    // Load Balancers
    public static final String EVENT_ASSIGN_TO_LOAD_BALANCER_RULE = "LB.ASSIGN.TO.RULE";
    public static final String EVENT_REMOVE_FROM_LOAD_BALANCER_RULE = "LB.REMOVE.FROM.RULE";
    public static final String EVENT_LOAD_BALANCER_CREATE = "LB.CREATE";
    public static final String EVENT_LOAD_BALANCER_DELETE = "LB.DELETE";
    public static final String EVENT_LOAD_BALANCER_UPDATE = "LB.UPDATE";

    // Account events
    public static final String EVENT_ACCOUNT_DISABLE = "ACCOUNT.DISABLE";
    
	// UserVO Events
	public static final String EVENT_USER_LOGIN = "USER.LOGIN";
	public static final String EVENT_USER_LOGOUT = "USER.LOGOUT";
	public static final String EVENT_USER_CREATE = "USER.CREATE";
	public static final String EVENT_USER_DELETE = "USER.DELETE";
    public static final String EVENT_USER_DISABLE = "USER.DISABLE";
	public static final String EVENT_USER_UPDATE = "USER.UPDATE";

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

	// Network Groups
	public static final String EVENT_NETWORK_GROUP_AUTHORIZE_INGRESS = "NG.AUTH.INGRESS";
    public static final String EVENT_NETWORK_GROUP_REVOKE_INGRESS = "NG.REVOKE.INGRESS";

    // Host
    public static final String EVENT_HOST_RECONNECT = "HOST.RECONNECT";

    // Maintenance
    public static final String EVENT_MAINTENANCE_CANCEL = "MAINT.CANCEL";
    public static final String EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE = "MAINT.CANCEL.PS";
    public static final String EVENT_MAINTENANCE_PREPARE = "MAINT.PREPARE";
    public static final String EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE = "MAINT.PREPARE.PS";
}
