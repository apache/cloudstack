/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.service.core.ec2;

public class EC2RunInstances {

	private String instanceType;
	private String zoneName;
	private String templateId;
	private String groupId;
	private String userData;
	private String keyName;
	private int    maxCount;
	private int    minCount;
    private Integer    size;  		// <- in gigs
	
	
	public EC2RunInstances() {
		instanceType = null;
		zoneName     = null;
		templateId   = null;
		groupId      = null;
		userData     = null;
		keyName = null;
		maxCount     = 0;
		minCount     = 0;
		size		 = 0;
	}
	
	public void setInstanceType( String instanceType ) {
		this.instanceType = instanceType;
	}
	
	public String getInstanceType() {
		return this.instanceType;
	}

	public void setZoneName( String zoneName ) {
		this.zoneName = zoneName;
	}
	
	public String getZoneName() {
		return this.zoneName;
	}

	public void setTemplateId( String templateId ) {
		this.templateId = templateId;
	}
	
	public String getTemplateId() {
		return this.templateId;
	}	

	public void setGroupId( String groupId ) {
		this.groupId = groupId;
	}
	
	public String getGroupId() {
		return this.groupId;
	}	

	public void setUserData( String userData ) {
		this.userData = userData;
	}
	
	public String getUserData() {
		return this.userData;
	}	
	
	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String publicKeyName) {
		this.keyName = publicKeyName;
	}

	public void setMaxCount( int maxCount ) {
		this.maxCount = maxCount;
	}
	
	public int getMaxCount() {
		return this.maxCount;
	}
	
	public void setMinCount( int minCount ) {
		this.minCount = minCount;
	}
	
	public int getMinCount() {
		return this.minCount;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
	
}
