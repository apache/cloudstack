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
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Kelven Yang
 */
public class SObjectItem implements Serializable {
	private static final long serialVersionUID = -7351173256185687851L;

	private Long id;
	
	private SObject theObject;
	private String version;
	private String md5;
	private String storedPath;
	private long storedSize;
	
	private Date createTime;
	private Date lastModifiedTime;
	private Date lastAccessTime;
	
	public SObjectItem() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public SObject getTheObject() {
		return theObject;
	}
	
	public void setTheObject(SObject theObject) {
		this.theObject = theObject;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getMd5() {
		return md5;
	}
	
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	
	public String getStoredPath() {
		return storedPath;
	}
	
	public void setStoredPath(String storedPath) {
		this.storedPath = storedPath;
	}
	
	public long getStoredSize() {
		return storedSize;
	}
	
	public void setStoredSize(long storedSize) {
		this.storedSize = storedSize;
	}
	
	public Date getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}
	
	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
	
	public Date getLastAccessTime() {
		return lastAccessTime;
	}
	
	public void setLastAccessTime(Date lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof SObjectItem))
			return false;

		if(version != null) {
			if(!version.equals(((SObjectItem)other).getVersion()))
				return false;
		} else {
			if(((SObjectItem)other).getVersion() != null)
				return false;
		}
		
		if(theObject.getId() != null) {
			if(!theObject.getId().equals(((SObjectItem)other).getTheObject()))
				return false;
		} else {
			if(((SObjectItem)other).getTheObject() != null)
				return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		if(version != null)
			hashCode = hashCode*17 + version.hashCode();
		
		if(theObject != null)
			hashCode = hashCode*17 + theObject.hashCode();
			
		return hashCode;
	}
}
