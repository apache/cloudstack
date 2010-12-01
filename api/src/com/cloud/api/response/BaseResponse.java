package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaseResponse implements ResponseObject {
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
    private Long jobId;
    
    @SerializedName(ApiConstants.JOB_STATUS) @Param(description="the current status of the latest async job acting on this object")
    private Integer jobStatus;
    
    public Long getJobId() {
    	return jobId;
    }
    
    public void setJobId(Long jobId) {
    	this.jobId = jobId;
    }
    
    public Integer getJobStatus() {
    	return jobStatus;
    }
    
    public void setJobStatus(Integer jobStatus) {
    	this.jobStatus = jobStatus;
    }
   
}
