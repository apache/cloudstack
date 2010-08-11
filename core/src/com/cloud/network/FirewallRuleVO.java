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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.gson.Gson;

/**
 * A bean representing a IP Forwarding
 * 
 * @author Will Chan
 *
 */
@Entity
@Table(name=("ip_forwarding"))
public class FirewallRuleVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
	private Long id;

    @Column(name="group_id")
    private Long groupId;

	@Column(name="public_ip_address")
	private String publicIpAddress = null;

	@Column(name="public_port")
	private String publicPort = null;
	
	@Column(name="private_ip_address")
	private String privateIpAddress = null;
	
	@Column(name="private_port")
	private String privatePort = null;
	
	@Column(name="enabled")
	private boolean enabled = false;
	
	@Column(name="protocol")
	private String protocol = "TCP";
	
	@Column(name="forwarding")
	private boolean forwarding = true;
	
	@Column(name="algorithm")
	private String algorithm = null;
	
	@Transient
	private String vlanNetmask;
	
	public FirewallRuleVO() {
	}
	
	public FirewallRuleVO(Long id, Long groupId, String publicIpAddress, String publicPort, String privateIpAddress, String privatePort, boolean enabled, String protocol,
			boolean forwarding, String algorithm) {
	    this.id = id;
	    this.groupId = groupId;
	    this.publicIpAddress = publicIpAddress;
	    this.publicPort = publicPort;
	    this.privateIpAddress = privateIpAddress;
	    this.privatePort = privatePort;
	    this.enabled = enabled;
	    this.protocol = protocol;
	}
	
	public FirewallRuleVO(FirewallRuleVO fwRule) {
		this(fwRule.getId(), fwRule.getGroupId(), fwRule.getPublicIpAddress(), 
			 fwRule.getPublicPort(), fwRule.getPrivateIpAddress(), 
			 fwRule.getPrivatePort(), fwRule.isEnabled(), fwRule.getProtocol(),
			 fwRule.isForwarding(), fwRule.getAlgorithm());
	}

	public Long getId() {
		return id;
	}

	public Long getGroupId() {
	    return groupId;
	}

	public void setGroupId(Long groupId) {
	    this.groupId = groupId;
	}

	public String getPublicIpAddress() {
		return publicIpAddress;
	}
	
	public void setPublicIpAddress(String address) {
		this.publicIpAddress = address;
	}
	
	public String getPublicPort() {
		return publicPort;
	}
	
	public void setPublicPort(String port) {
		this.publicPort = port;
	}
	
	public String getPrivateIpAddress() {
		return privateIpAddress;
	}
	
	public void setPrivateIpAddress(String privateIpAddress) {
		this.privateIpAddress = privateIpAddress;
	}
	
	public String getPrivatePort() {
		return privatePort;
	}
	
	public void setPrivatePort(String privatePort) {
		this.privatePort = privatePort;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getProtocol() {
		return this.protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public boolean isForwarding() {
		return forwarding;
	}
	public void setForwarding(boolean forwarding) {
		this.forwarding = forwarding;
	}
	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	
	public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
	}
	
	public void setVlanNetmask(String vlanNetmask) {
		this.vlanNetmask = vlanNetmask;
	}
	
	public String getVlanNetmask() {
		return vlanNetmask;
	}

}

