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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * CiscoNexusVSMDeviceVO contains information on external Cisco Nexus 1000v VSM devices added into a deployment.
 * This should be probably made as a more generic class so that we can handle multiple versions of Nexus VSMs
 * in future.
 */

@Entity
@Table(name="external_virtual_switch_management_devices")
public class CiscoNexusVSMDeviceVO {
	
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

    @Column(name = "vmsmgmtipaddr")
    private String vsmMgmtIPAddr;
    
    @Column(name = "vcenteripaddr")
    private String vCenterIPAddr;
    
    // Name of the DataCenter (as seen in vCenter) that this VSM manages.
    @Column(name = "vcenterdcname")
    private String vCenterDCName;
    
    @Column(name = "management_vlan")
    private int managementVlan;

    @Column(name = "control_vlan")
    private int controlVlan;
    
    @Column(name = "packet_vlan")
    private int packetVlan;
    
    @Column(name = "storage_vlan")
    private int storageVlan;
    
    @Column(name = "vsmDomainId")
    private long vsmDomainId;
    
    @Column(name = "config_mode")
    private VSMConfigMode vsmConfigMode;
    
    @Column(name = "ConfigState")
    private VSMConfigState vsmConfigState;
    
    @Column(name = "vsmDeviceState")
    private VSMDeviceState vsmDeviceState;    
    
    // ********** The ones below could be removed...
    
    // Id of the DataCenter (as seen in vCenter) that this VSM manages.
    // We can probably remove this.
    @Column(name = "vcenteredcid")
    private long vCenterDCId;
    
    // Name of the DVS that gets created on vCenter to represent this VSM.
    // Can be queried and hence can be most probably removed.
    @Column(name = "dvsname")
    private String dvsName;
        
    // Number of VEMs being currently managed by this VSM.
    // Again, queriable/removable.
    @Column(name = "num_of_vems")
    private int numVEMS;
    
    // ******** End of removable candidates.
    
    

    // This tells us whether the VSM is currently enabled or disabled. We may
    // need this if we would like to carry out any sort of maintenance on the
    // VSM or CS.
    public enum VSMDeviceState {
    	Enabled,
    	Disabled
    }    
    
    // This tells us whether the VSM is currently configured with a standby (HA)
    // or does not have any standby (Standalone).
    public enum VSMConfigMode {
        Standalone,
        HA
    }
    
    // This tells us whether the VSM is currently a primary or a standby VSM.
    public enum VSMConfigState {
        Primary,
        Standby
    }

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

    public String getMgmtIpAddr() {
    	return vsmMgmtIPAddr;
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
    
    public void getUserName(String username) {
    	this.vsmUserName = username;
    }
    
    public void setvsmName(String vsmName) {
    	this.vsmName = vsmName;
    }
    
    public void setPassword(String password) {
    	this.vsmPassword = password;
    }

    public void setMgmtIpAddr(String ipaddr) {
    	this.vsmMgmtIPAddr = ipaddr;
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
    
    public void setvsmDomainId(long id) {
    	this.vsmDomainId = id;
    }
    
    public void setvsmConfigMode(VSMConfigMode mode) {
    	this.vsmConfigMode = mode;
    }
    
    public void setvsmConfigState(VSMConfigState state) {
    	this.vsmConfigState = state;
    }
    
    public void setvsmDeviceState(VSMDeviceState devState) {
    	this.vsmDeviceState = devState;
    }
 
        
    // Constructor methods.
    
    public CiscoNexusVSMDeviceVO(long id, String vsmIpAddr, String username, String password) {    	
    	// Set all the VSM's properties here.
    	this.id = id;
        this.uuid = UUID.randomUUID().toString();
        this.vsmMgmtIPAddr = vsmIpAddr;
        this.vsmUserName = username;
        this.vsmPassword = password;        
    }

    public CiscoNexusVSMDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }    
}
