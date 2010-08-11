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

public class VMOperationResultObject {

	@Param(name="id")
	private long id;
	
	@Param(name="name")
	private String name;
	
	@Param(name="created")
	private Date created;
	
	@Param(name="zoneid")
	private Long zoneId;
	
	@Param(name="zonename")
	private String zoneName;
	
	@Param(name="ipaddress")
	private String ipAddress;
	
	@Param(name="serviceofferingid")
	private Long serviceOfferingId;
	
	@Param(name="haenable")
	private boolean haEnabled;
	
	@Param(name="state")
	private String state;
	
	@Param(name="templateid")
	private Long templateId;
	
	@Param(name="password")
	private String password;
	
	@Param(name="templatename")
	private String templateName;
	
	@Param(name="templatedisplaytext")
	private String templateDisplayText;
	
	@Param(name="isoid")
	private Long isoId;
	
	@Param(name="isoname")
	private String isoName;
	
	@Param(name="passwordenabled")
	private boolean passwordEnabled;
	
	@Param(name="serviceofferingname")
	private String serviceOfferingName;
	
	@Param(name="diskofferingid")
	private Long diskOfferingId;
	
	@Param(name="datadiskofferingname")
	private String diskOfferingName;
	
	@Param(name="cpunumber")
	private String cpuNumber;
	
	@Param(name="cpuspeed")
	private String cpuSpeed;
	
	@Param(name="memory")
	private String memory;
	
	@Param(name="storage")
	private String storage;
	
	@Param(name="displayname")
	private String displayName;
	
	@Param(name="group")
	private String group;
	
	@Param(name="domainid")
	private Long domainId;
	
	@Param(name="domain")
	private String domain;
	
	@Param(name="account")
	private String account;
	
	@Param(name="hostname")
	private String hostname;
	
	@Param(name="hostid")
	private Long hostid;
	
	@Param(name="networkgrouplist")
	private String networkGroupList;
	
	public String getNetworkGroupList(){
		return this.networkGroupList;
	}
	
	public void setNetworkGroupList(String nGroups){
		this.networkGroupList = nGroups;
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
	
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public Long getZoneId() {
		return zoneId;
	}
	
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}
	
	public String getZoneName() {
		return zoneName;
	}
	
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public Long getServiceOfferingId() {
		return serviceOfferingId;
	}
	
	public void setServiceOfferingId(Long serviceOfferingId) {
		this.serviceOfferingId = serviceOfferingId;
	}
	
	public boolean isHaEnabled() {
		return haEnabled;
	}
	
	public void setHaEnabled(boolean haEnabled) {
		this.haEnabled = haEnabled;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public Long getTemplateId() {
		return templateId;
	}
	
	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	public String getTemplateDisplayText() {
		return templateDisplayText;
	}

	public void setTemplateDisplayText(String templateDisplayText) {
		this.templateDisplayText = templateDisplayText;
	}

	public Long getIsoId() {
		return isoId;
	}
	
	public void setIsoId(Long isoId) {
		this.isoId = isoId;
	}
	
	public String getIsoName() {
		return isoName;
	}
	
	public void setIsoName(String isoName) {
		this.isoName = isoName;
	}

	public boolean isPasswordEnabled() {
		return passwordEnabled;
	}

	public void setPasswordEnabled(boolean passwordEnabled) {
		this.passwordEnabled = passwordEnabled;
	}

	public String getServiceOfferingName() {
		return serviceOfferingName;
	}

	public void setServiceOfferingName(String serviceOfferingName) {
		this.serviceOfferingName = serviceOfferingName;
	}

	public Long getDiskOfferingId() {
		return diskOfferingId;
	}

	public void setDiskOfferingId(Long diskOfferingId) {
		this.diskOfferingId = diskOfferingId;
	}

	public String getDiskOfferingName() {
		return diskOfferingName;
	}

	public void setDiskOfferingName(String diskOfferingName) {
		this.diskOfferingName = diskOfferingName;
	}

	public String getCpuNumber() {
		return cpuNumber;
	}

	public void setCpuNumber(String cpuNumber) {
		this.cpuNumber = cpuNumber;
	}

	public String getCpuSpeed() {
		return cpuSpeed;
	}

	public void setCpuSpeed(String cpuSpeed) {
		this.cpuSpeed = cpuSpeed;
	}

	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	public String getStorage() {
		return storage;
	}

	public void setStorage(String stroage) {
		this.storage = stroage;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public Long getDomainId() {
		return domainId;
	}
	
	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}
	
	public String getAccount() {
		return account;
	}
	
	public void setAccount(String account) {
		this.account = account;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	public Long getHostid() {
		return hostid;
	}
	
	public void setHostid(Long hostid) {
		this.hostid = hostid;
	}
	  
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
}
