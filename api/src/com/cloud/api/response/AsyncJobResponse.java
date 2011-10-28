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

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AsyncJobResponse extends BaseResponse {
    @SerializedName(ApiConstants.JOB_ID) @Param(description="async job ID")
    private String id;

    @SerializedName("accountid") @Param(description="the account that executed the async command")
    private Long accountId;

    @SerializedName(ApiConstants.USER_ID) @Param(description="the user that executed the async command")
    private Long userId;

    @SerializedName("cmd") @Param(description="the async command executed")
    private String cmd;

    @SerializedName("jobstatus") @Param(description="the current job status-should be 0 for PENDING")
    private Integer jobStatus;

    @SerializedName("jobprocstatus") @Param(description="the progress information of the PENDING job")
    private Integer jobProcStatus;

    @SerializedName("jobresultcode") @Param(description="the result code for the job")
    private Integer jobResultCode;

    @SerializedName("jobresulttype") @Param(description="the result type")
    private String jobResultType;

    @SerializedName("jobresult") @Param(description="the result reason")
    private ResponseObject jobResult;
 
    @SerializedName("jobinstancetype") @Param(description="the instance/entity object related to the job")
    private String jobInstanceType;

    @SerializedName("jobinstanceid") @Param(description="the unique ID of the instance/entity object related to the job")
    // private Long jobInstanceId;
    IdentityProxy jobInstanceIdProxy = new IdentityProxy();

    @SerializedName(ApiConstants.CREATED) @Param(description="	the created date of the job")
    private Date created;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Integer getJobProcStatus() {
        return jobProcStatus;
    }

    public void setJobProcStatus(Integer jobProcStatus) {
        this.jobProcStatus = jobProcStatus;
    }

    public Integer getJobResultCode() {
        return jobResultCode;
    }

    public void setJobResultCode(Integer jobResultCode) {
        this.jobResultCode = jobResultCode;
    }

    public String getJobResultType() {
        return jobResultType;
    }

    public void setJobResultType(String jobResultType) {
        this.jobResultType = jobResultType;
    }

    public ResponseObject getJobResult() {
        return jobResult;
    }

    public void setJobResult(ResponseObject jobResult) {
        this.jobResult = jobResult;
    }

    public String getJobInstanceType() {
        return jobInstanceType;
    }

    public void setJobInstanceType(String jobInstanceType) {
        this.jobInstanceType = jobInstanceType;
        if(jobInstanceType != null) {
        	if(jobInstanceType.equalsIgnoreCase("volume")) {
        		this.jobInstanceIdProxy.setTableName("volumes");
        	} else if(jobInstanceType.equalsIgnoreCase("template")) {
        		this.jobInstanceIdProxy.setTableName("vm_template");
        	} else if(jobInstanceType.equalsIgnoreCase("iso")) {
        		this.jobInstanceIdProxy.setTableName("vm_template");
        	} else {
        		// TODO : when we hit here, we need to add instanceType -> UUID entity table mapping
        		assert(false);
        	}
        }
    }

    public Long getJobInstanceId() {
        return jobInstanceIdProxy.getValue();
    }

    public void setJobInstanceId(Long jobInstanceId) {
        this.jobInstanceIdProxy.setValue(jobInstanceId);
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
