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
import com.cloud.exception.InvalidParameterValueException;

/**
 * PortProfileVO contains information on portprofiles that are created on a Cisco Nexus 1000v VSM associated
 * with a VMWare cluster. 
 */

@Entity
@Table(name="port_profile")
public class PortProfileVO {
	
	// We need to know what properties a VSM has. Put them here.
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name = "port_profile_name")
    private String portProfileName;

    @Column(name = "port_mode")
    private PortMode portMode;
    
    @Column(name = "vsm_id")
    private long vsmId;

    @Column(name = "trunk_low_vlan_id")
    private int lowVlanId;
    
    @Column(name = "trunk_high_vlan_id")
    private int highVlanId;
    
    @Column(name = "access_vlan_id")
    private int accessVlanId;
    
    @Column(name = "port_type")
    private PortType portType;
    
    @Column(name = "port_binding")
    private BindingType portBinding;

    public enum BindingType {
    	Static,
    	Ephemeral
    }
    
    public enum PortType {
    	Ethernet,
    	vEthernet
    }
    
    // This tells us whether the port trunks multiple VLANs
    // or carries traffic of a single VLAN.
    public enum PortMode {
    	Access,
    	Trunk
    }

    // Accessor methods
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPortProfileName() {
    	return portProfileName;
    }
    
    public PortMode getPortMode() {
    	return portMode;
    }
    
    public long getVsmId() {
    	return vsmId;
    }
    
    public int getLowVlanId() {
    	return lowVlanId;
    }
    
    public int getHighVlanId() {
    	return highVlanId;
    }
    
    public int getAccessVlanId() {
    	return accessVlanId;
    }
    
    public PortType getPortType() {
    	return portType;
    }
    
    public BindingType getPortBinding() {
    	return portBinding;
    }
    
    // Setter methods

    public void setPortProfileName(String name) {
    	portProfileName = name;
    }
    
    public void setPortMode(PortMode mode) {
    	portMode = mode;
    }
    
    public void setVsmId(long id) {
    	vsmId = id;
    }
    
    public void setLowVlanId(int vlanId) {
    	lowVlanId = vlanId;
    }
    
    public void setHighVlanId(int vlanId) {
    	highVlanId = vlanId;
    }
    
    public void setAccessVlanId(int vlanId) {
    	accessVlanId = vlanId;
    }
    
    public void setPortType(PortType type) {
    	portType = type;
    }
    
    public void setPortBinding(BindingType bindingType) {
    	portBinding = bindingType;
    }

    // Constructor methods.
    
    public PortProfileVO(String portProfName, long vsmId, int vlanId, PortType pType, BindingType bType) {
    	// Set the relevant portprofile properties here.
    	// When supplied with a single vlanId, we set this portprofile as an access port profile.
    	
    	this.setPortMode(PortMode.Access);
    	
        this.uuid = UUID.randomUUID().toString();
        this.setPortProfileName(portProfName);
        this.setVsmId(vsmId);
        this.setAccessVlanId(vlanId);
        this.setPortType(pType);
        this.setPortBinding(bType);
    }
    
    public PortProfileVO(String portProfName, long vsmId, int lowVlanId, int highVlanId, PortType pType, BindingType bType) {
    	// Set the relevant portprofile properties here.
    	// When supplied with a vlan range, we set this portprofile as a trunk port profile.
    	
    	if (lowVlanId >= highVlanId) {
    		throw new InvalidParameterValueException("Low Vlan Id cannot be greater than or equal to high Vlan Id");
    	}
    	this.setPortMode(PortMode.Trunk);
    	
        this.uuid = UUID.randomUUID().toString();
        this.setPortProfileName(portProfName);
        this.setVsmId(vsmId);
        this.setLowVlanId(lowVlanId);
        this.setHighVlanId(highVlanId);
        this.setPortType(pType);
        this.setPortBinding(bType);
    }    
    
    public PortProfileVO() {
        this.uuid = UUID.randomUUID().toString();
    }    
}
