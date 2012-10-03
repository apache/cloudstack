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
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the relation
 *  Id,
 *  Name, 
 *  OwnerCanonicalId,
 *  SHost, 
 *  CreateTime, 
 *  VersioningStatus
 * For ORM see "com/cloud/bridge/model/SHost.hbm.xml"
 */
public interface SBucket {

    public static final int VERSIONING_NULL = 0;   
	public static final int VERSIONING_ENABLED = 1;
	public static final int VERSIONING_SUSPENDED = 2;

/*	private Long id;
	
	private String name;
	private String ownerCanonicalId;
	
	private SHost shost;
	private Date createTime;
	
	private int versioningStatus;
	
	private Set<SObject> objectsInBucket = new HashSet<SObject>();
	
	public SBucket() {
		versioningStatus = VERSIONING_NULL;
	}
	
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getOwnerCanonicalId() {
		return ownerCanonicalId;
	}
	
	public void setOwnerCanonicalId(String ownerCanonicalId) {
		this.ownerCanonicalId = ownerCanonicalId;
	}
	
	public SHost getShost() {
		return shost;
	}
	
	public void setShost(SHost shost) {
		this.shost = shost;
	}
	
	public Date getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	public int getVersioningStatus() {
		return versioningStatus;
	}
	
	public void setVersioningStatus( int versioningStatus ) {
		this.versioningStatus = versioningStatus;
	}
	
	public Set<SObject> getObjectsInBucket() {
		return objectsInBucket;
	}

	public void setObjectsInBucket(Set<SObject> objectsInBucket) {
		this.objectsInBucket = objectsInBucket;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof SBucket))
			return false;
		
		return getName().equals(((SBucket)other).getName());
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}*/
}
