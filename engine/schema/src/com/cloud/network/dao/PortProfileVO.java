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


package com.cloud.network.dao;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * PortProfileVO contains information on portprofiles that are created on a Cisco Nexus 1000v VSM associated
 * with a VMWare cluster. 
 */

@Entity
@Table(name="port_profile")
public class PortProfileVO implements InternalIdentity {
	
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
