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

public class RouterOperationResultObject {
	@Param(name="id")
	private long id;
	
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
	
	@Param(name="name")
	private String name;
	
	@Param(name="podid")
	private long podId;
	
	@Param(name="privateip")
	private String privateIp;
	
	@Param(name="privatemacaddress")
	private String privateMacAddress;
	
	@Param(name="privatenetmask")
	private String privateNetMask;
	
	@Param(name="publicip")
	private String publicIp;
	
	@Param(name="publicmacaddress")
	private String publicMacAddress;
	
	@Param(name="publicnetmask")
	private String publicNetMask;
	
	@Param(name="ipaddress")
	private String guestIp;
	
	@Param(name="macaddress")
	private String guestMacAddress;
	
	@Param(name="templateid")
	private long templateId;
	
	@Param(name="created")
	private Date created;
	
	@Param(name="account")
	private String account;
	
	@Param(name="domainid")
	private long domainId;
	
	@Param(name="domain")
	private String domain;
	
	@Param(name="hostid")
	private Long hostId;
	
	@Param(name="state")
	private String state;
	
	@Param(name="hostname")
	private String hostname;

	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public long getPodId() {
		return podId;
	}
	
	public void setPodId(long podId) {
		this.podId = podId;
	}
	
	public String getPrivateIp() {
		return privateIp;
	}
	
	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}
	
	public String getPrivateMacAddress() {
		return privateMacAddress;
	}
	
	public void setPrivateMacAddress(String privateMacAddress) {
		this.privateMacAddress = privateMacAddress;
	}
	
	public String getPrivateNetMask() {
		return privateNetMask;
	}
	
	public void setPrivateNetMask(String privateNetMask) {
		this.privateNetMask = privateNetMask;
	}
	
	public String getPublicIp() {
		return publicIp;
	}
	
	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}
	
	public String getPublicMacAddress() {
		return publicMacAddress;
	}
	
	public void setPublicMacAddress(String publicMacAddress) {
		this.publicMacAddress = publicMacAddress;
	}
	
	public String getPublicNetMask() {
		return publicNetMask;
	}
	
	public void setPublicNetMask(String publicNetMask) {
		this.publicNetMask = publicNetMask;
	}
	
	public String getGuestIp() {
		return guestIp;
	}
	
	public void setGuestIp(String guestIp) {
		this.guestIp = guestIp;
	}
	
	public String getGuestMacAddress() {
		return guestMacAddress;
	}
	
	public void setGuestMacAddress(String guestMacAddress) {
		this.guestMacAddress = guestMacAddress;
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
	
	public String getAccount() {
		return account;
	}
	
	public void setAccount(String account) {
		this.account = account;
	}
	
	public Long getHostId() {
		return hostId;
	}
	
	public void setHostId(Long hostId) {
		this.hostId = hostId;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public long getDomainId() {
		return domainId;
	}
	
	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
}
