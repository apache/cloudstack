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

import com.google.gson.annotations.SerializedName;

public class AsyncJobResponse extends BaseResponse {
    @SerializedName("jobid")
    private Long id;

    @SerializedName("accountid")
    private Long accountId;

    @SerializedName("userid")
    private Long userId;

    @SerializedName("cmd")
    private String cmd;

    @SerializedName("jobstatus")
    private Integer jobStatus;

    @SerializedName("jobprocstatus")
    private Integer jobProcStatus;

    @SerializedName("jobresultcode")
    private Integer jobResultCode;

    @SerializedName("jobresulttype")
    private String jobResultType;

    @SerializedName("jobresult")
    private String jobResult;

    @SerializedName("jobinstancetype")
    private String jobInstanceType;

    @SerializedName("jobinstanceid")
    private Long jobInstanceId;

    @SerializedName("created")
    private Date created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getJobResult() {
        return jobResult;
    }

    public void setJobResult(String jobResult) {
        this.jobResult = jobResult;
    }

    public String getJobInstanceType() {
        return jobInstanceType;
    }

    public void setJobInstanceType(String jobInstanceType) {
        this.jobInstanceType = jobInstanceType;
    }

    public Long getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
