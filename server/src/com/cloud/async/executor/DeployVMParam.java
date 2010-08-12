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

public class DeployVMParam extends VMOperationParam {
	private long accountId;
	private long dataCenterId;
	private long serviceOfferingId;	
	private long templateId;
	private Long diskOfferingId;
	private String domain;
	private String password;
	private String displayName;
	private String group;
	private String userData;
	private long domainId;
	private String [] networkGroups;
	
	public DeployVMParam() {
	}
	
	public DeployVMParam(long userId, long accountId, long dataCenterId, 
		long serviceOfferingId, long templateId,
		Long diskOfferingId, String domain, String password,
		String displayName, String group, String userData, String [] networkGroups) {
		
		setUserId(userId);
		this.accountId = accountId;
		this.dataCenterId = dataCenterId;
		this.serviceOfferingId = serviceOfferingId;
		this.templateId = templateId;
		this.diskOfferingId = diskOfferingId;
		this.domain = domain;
		this.password = password;
		this.displayName = displayName;
		this.group = group;
		this.userData = userData;
		this.setNetworkGroups(networkGroups);
	}
	
	public DeployVMParam(long userId, long accountId, long dataCenterId, 
	        long serviceOfferingId, long templateId,
	        Long diskOfferingId, String domain, String password,
	        String displayName, String group, String userData, 
	        String [] networkGroups, long eventId) {
	        
	        setUserId(userId);
	        this.accountId = accountId;
	        this.dataCenterId = dataCenterId;
	        this.serviceOfferingId = serviceOfferingId;
	        this.templateId = templateId;
	        this.diskOfferingId = diskOfferingId;
	        this.domain = domain;
	        this.password = password;
	        this.displayName = displayName;
	        this.group = group;
	        this.userData = userData;
	        this.setNetworkGroups(networkGroups);
	        this.eventId = eventId;
	    }

	public long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}
	
	public long getDataCenterId() {
		return dataCenterId;
	}
	
	public void setDataCenterId(long dataCenterId) {
		this.dataCenterId = dataCenterId;
	}
	
	public long getServiceOfferingId() {
		return serviceOfferingId;
	}
	
	public void setServiceOfferingId(long serviceOfferingId) {
		this.serviceOfferingId = serviceOfferingId;
	}
	
	public Long getDiskOfferingId() {
		return diskOfferingId;
	}
	
	public void setDiskOfferingId(Long diskOfferingId) {
		this.diskOfferingId = diskOfferingId;
	}
	
	public long getTemplateId() {
		return templateId;
	}
	
	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
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
	public long getDomainId() {
		return domainId;
	}
	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public void setUserData(String userData) {
		this.userData = userData;
	}

	public String getUserData() {
		return userData;
	}

	public void setNetworkGroups(String [] networkGroups) {
		this.networkGroups = networkGroups;
	}

	public String [] getNetworkGroup() {
		return networkGroups;
	}
}
