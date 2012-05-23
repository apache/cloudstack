/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;

/**
 * CiscoNexusVSMDeviceVO contains information on external Cisco Nexus 1000v VSM devices added into a deployment.
 * This should be probably made as a more generic class so that we can handle multiple versions of Nexus VSMs
 * in future.
 */

@Entity
@Table(name="virtual_supervisor_module")
public class CiscoNexusVSMDeviceVO implements CiscoNexusVSMDevice, Identity{
	
	// We need to know what properties a VSM has. Put them here.
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "vsm_name")
    private String vsmName;
    
    @Column(name = "username")
    private String vsmUserName;
    
    @Column(name = "password")
    private String vsmPassword;

    @Column(name = "ipaddr")
    private String ipaddr;
    
    @Column(name = "vcenter_ipaddr")
    private String vCenterIPAddr;
    
    // Name of the DataCenter (as seen in vCenter) that this VSM manages.
    @Column(name = "vcenter_dc_name")
    private String vCenterDCName;
    
    @Column(name = "management_vlan")
    private int managementVlan;

    @Column(name = "control_vlan")
    private int controlVlan;
    
    @Column(name = "packet_vlan")
    private int packetVlan;
    
    @Column(name = "storage_vlan")
    private int storageVlan;
    
    @Column(name = "vsm_domain_id")
    private long vsmDomainId;
    
    @Column(name = "config_mode")
    private VSMConfigMode vsmConfigMode;
    
    @Column(name = "config_state")
    private VSMConfigState vsmConfigState;
    
    @Column(name = "vsm_device_state")
    private VSMDeviceState vsmDeviceState;

    // Accessor methods
    public long getId() {
        return id;
    }

    public String getvsmName() {
    	return vsmName;
    }
    
    public long getHostId() {
        return hostId;
    }
    
    public String getUserName() {
    	return vsmUserName;
    }
    
    public String getPassword() {
    	return vsmPassword;
    }

    public String getipaddr() {
    	return ipaddr;
    }
    
    public String getvCenterIPAddr() {
    	return vCenterIPAddr;
    }
    
    public String getvCenterDCName() {
    	return vCenterDCName;
    }
    
    public int getManagementVlan() {
    	return managementVlan;
    }
    
    public int getControlVlan() {
    	return controlVlan;
    }
    
    public int getPacketVlan() {
    	return packetVlan;
    }  

    public int getStorageVlan() {
    	return storageVlan;
    }
    
    public long getvsmDomainId() {
    	return vsmDomainId;
    }
    
    public VSMConfigMode getvsmConfigMode() {
    	return vsmConfigMode;
    }
    
    public VSMConfigState getvsmConfigState() {
    	return vsmConfigState;
    }
    
    public VSMDeviceState getvsmDeviceState() {
    	return vsmDeviceState;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    // Setter methods

    public void setHostId(long hostid) {
        this.hostId = hostid;
    }
    
    public void setVsmUserName(String username) {
    	this.vsmUserName = username;
    }
    
    public void setVsmName(String vsmName) {
    	this.vsmName = vsmName;
    }
    
    public void setVsmPassword(String password) {
    	this.vsmPassword = password;
    }

    public void setMgmtIpAddr(String ipaddr) {
    	this.ipaddr = ipaddr;
    }
    
    public void setvCenterIPAddr(String ipaddr) {
    	this.vCenterIPAddr = ipaddr;
    }
    
    public void setvCenterDCName(String dcname) {
    	this.vCenterDCName = dcname;
    }
    
    public void setManagementVlan(int vlan) {
    	this.managementVlan = vlan;
    }
    
    public void setControlVlan(int vlan) {
    	this.controlVlan = vlan;
    }
    
    public void setPacketVlan(int vlan) {
    	this.packetVlan = vlan;
    }  

    public void setStorageVlan(int vlan) {
    	this.storageVlan = vlan;
    }
    
    public void setVsmDomainId(long id) {
    	this.vsmDomainId = id;
    }
    
    public void setVsmConfigMode(VSMConfigMode mode) {
    	this.vsmConfigMode = mode;
    }
    
    public void setVsmConfigState(VSMConfigState state) {
    	this.vsmConfigState = state;
    }
    
    public void setVsmDeviceState(VSMDeviceState devState) {
    	this.vsmDeviceState = devState;
    }
 
        
    // Constructor methods.
    
    public CiscoNexusVSMDeviceVO(String vsmIpAddr, String username, String password, String vCenterIpaddr, String vCenterDcName) {    	
    	// Set all the VSM's properties here.
        this.uuid = UUID.randomUUID().toString();
        this.setMgmtIpAddr(vsmIpAddr);
        this.setVsmUserName(username);
        this.setVsmPassword(password);
        this.setvCenterIPAddr(vCenterIpaddr);
        this.setvCenterDCName(vCenterDcName);
        // By default, enable a VSM.
        this.setVsmDeviceState(VSMDeviceState.Enabled);
    }
    
    public CiscoNexusVSMDeviceVO(String vsmIpAddr, String username, String password, long dummy) {    	
    	// Set all the VSM's properties here.
        this.uuid = UUID.randomUUID().toString();
        this.setMgmtIpAddr(vsmIpAddr);
        this.setVsmUserName(username);
        this.setVsmPassword(password);
        this.setVsmName(vsmName);
        this.setVsmDeviceState(VSMDeviceState.Enabled);
    }
    
    public CiscoNexusVSMDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }    
}
