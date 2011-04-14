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

import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.serializer.Param;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.upload.UploadState;

public class ExtractJobResultObject {
	
	public ExtractJobResultObject(Long accountId, String typeName, String currState, int uploadPercent, Long uploadId){
		this.accountId = accountId;
		this.name = typeName;
		this.state = currState;
		this.id = uploadId;
		this.uploadPercent = uploadPercent;
	}

    public ExtractJobResultObject(Long accountId, String typeName, String currState, Long uploadId, String url){
        this.accountId = accountId;
        this.name = typeName;
        this.state = currState;
        this.id = uploadId;
        this.url = url;
    }	
    
	public ExtractJobResultObject(){		
	}
	
	@Param(name="id")
	private long id;
    
	@Param(name="name")
	private String name;
	
    @Param(name="uploadPercentage")
	private int uploadPercent;
    
    @Param(name="uploadStatus")
	private String uploadStatus;
    
    @Param(name="accountid")
    long accountId;    
 
    @Param(name="result_string")
    String result_string;    

    @Param(name="created")
    private Date createdDate;

    @Param(name="state")
    private String state;
    
    @Param(name="storagetype")
	String storageType;
    
    @Param(name="storage")
    private String storage;
    
    @Param(name="zoneid")
    private Long zoneId;

    @Param(name="zonename")
    private String zoneName;

    @Param(name="url")
    private String url;
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getUploadPercent() {
        return uploadPercent;
    }

    public void setUploadPercent(int i) {
        this.uploadPercent = i;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getResult_string() {
        return result_string;
    }

    public void setResult_string(String resultString) {
        result_string = resultString;
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

	public String getStorage() {
		return storage;
	}

	public void setStorage(String storage) {
		this.storage = storage;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
          
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setState(String status) {
        this.state = status;
    }

    public String getState() {
        return state;
    }
   
    public void setStorageType (String storageType) {
    	this.storageType = storageType;
    }
    
    public String getStorageType() {
    	return storageType;
    }
    
}
