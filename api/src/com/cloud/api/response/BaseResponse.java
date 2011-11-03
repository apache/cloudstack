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

package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public abstract class BaseResponse implements ResponseObject {
    private transient String responseName;
    private transient String objectName;
    
    @Override
    public String getResponseName() {
        return responseName;
    }

    @Override
    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }
    
    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
    
    public Long getObjectId() {
    	return null;
    }
    
    // For use by list commands with pending async jobs
    @SerializedName(ApiConstants.JOB_ID) @Param(description="the ID of the latest async job acting on this object")
    protected IdentityProxy jobId = new IdentityProxy("async_job");
    
    @SerializedName(ApiConstants.JOB_STATUS) @Param(description="the current status of the latest async job acting on this object")
    private Integer jobStatus;
    
    public Long getJobId() {
    	return jobId.getValue();
    }
    
    public void setJobId(Long jobId) {
    	this.jobId.setValue(jobId);
    }
    
    public Integer getJobStatus() {
    	return jobStatus;
    }
    
    public void setJobStatus(Integer jobStatus) {
    	this.jobStatus = jobStatus;
    }
}
