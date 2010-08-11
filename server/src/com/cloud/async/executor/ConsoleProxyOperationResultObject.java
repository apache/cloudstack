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

package com.cloud.async.executor;

import java.util.Date;

import com.cloud.serializer.Param;

public class ConsoleProxyOperationResultObject {

	@Param(name="id")
	private long id;
	
	@Param(name="name")
	private String name;
	
	@Param(name="zoneid")
	private long zoneId;
	
	@Param(name="zonename")
	private String zoneName;
	
	@Param(name="dns1")
	private String dns1;
	
	@Param(name="dns2")
	private String dns2;
	
	@Param(name="networkdomain")
	private String networkDomain;
	
	@Param(name="gateway")
	private String gateway;

	@Param(name="podid")
	private long podId;
	
	@Param(name="hostid")
	private Long hostId;
	
	@Param(name="hostname")
	private String hostName;
	
	@Param(name="privateip")
	private String privateIp;
	
	@Param(name="privatemacaddress")
	private String privateMac;
	
	@Param(name="privatenetmask")
	private String privateNetmask;
	
	@Param(name="publicip")
	private String publicIp;
	
	@Param(name="publicmacaddress")
	private String publicMac;
	
	@Param(name="publicnetmask")
	private String publicNetmask;

	@Param(name="templateid")
	private long templateId;
	
	@Param(name="created")
	private Date created;
	
	@Param(name="activeviewersessions")
	private int actionSessions;
	
	@Param(name="state")
	private String state;

	public ConsoleProxyOperationResultObject() {
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getZoneId() {
		return zoneId;
	}

	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}

	public String getZoneName() {
		return zoneName;
	}

	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public String getDns1() {
		return dns1;
	}

	public void setDns1(String dns1) {
		this.dns1 = dns1;
	}

	public String getDns2() {
		return dns2;
	}

	public void setDns2(String dns2) {
		this.dns2 = dns2;
	}

	public String getNetworkDomain() {
		return networkDomain;
	}

	public void setNetworkDomain(String networkDomain) {
		this.networkDomain = networkDomain;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public long getPodId() {
		return podId;
	}

	public void setPodId(long podId) {
		this.podId = podId;
	}

	public Long getHostId() {
		return hostId;
	}

	public void setHostId(Long hostId) {
		this.hostId = hostId;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}

	public String getPrivateMac() {
		return privateMac;
	}

	public void setPrivateMac(String privateMac) {
		this.privateMac = privateMac;
	}

	public String getPrivateNetmask() {
		return privateNetmask;
	}

	public void setPrivateNetmask(String privateNetmask) {
		this.privateNetmask = privateNetmask;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public String getPublicMac() {
		return publicMac;
	}

	public void setPublicMac(String publicMac) {
		this.publicMac = publicMac;
	}

	public String getPublicNetmask() {
		return publicNetmask;
	}

	public void setPublicNetmask(String publicNetmask) {
		this.publicNetmask = publicNetmask;
	}

	public long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public int getActionSessions() {
		return actionSessions;
	}

	public void setActionSessions(int actionSessions) {
		this.actionSessions = actionSessions;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
