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
package org.apache.cloudstack.api.response;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.serializer.Param;

@EntityReference(value = JobInfo.class)
public class AsyncJobResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "the account id that executed the async command")
    private String accountId;

    @SerializedName("account")
    @Param(description = "the account that executed the async command")
    private String account;

    @SerializedName("domainid")
    @Param(description = "the domain id that executed the async command")
    private String domainid;

    @SerializedName("domainpath")
    @Param(description = "the domain that executed the async command")
    private String domainPath;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "the user that executed the async command")
    private String userId;

    @SerializedName("cmd")
    @Param(description = "the async command executed")
    private String cmd;

    @SerializedName("jobstatus")
    @Param(description = "the current job status-should be 0 for PENDING")
    private Integer jobStatus;

    @SerializedName("jobprocstatus")
    @Param(description = "the progress information of the PENDING job")
    private Integer jobProcStatus;

    @SerializedName("jobresultcode")
    @Param(description = "the result code for the job")
    private Integer jobResultCode;

    @SerializedName("jobresulttype")
    @Param(description = "the result type")
    private String jobResultType;

    @SerializedName("jobresult")
    @Param(description = "the result reason")
    private ResponseObject jobResult;

    @SerializedName("jobinstancetype")
    @Param(description = "the instance/entity object related to the job")
    private String jobInstanceType;

    @SerializedName("jobinstanceid")
    @Param(description = "the unique ID of the instance/entity object related to the job")
    private String jobInstanceId;

    @SerializedName("managementserverid")
    @Param(description = "the msid of the management server on which the job is running", since = "4.19")
    private Long msid;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "  the created date of the job")
    private Date created;

    @SerializedName(ApiConstants.COMPLETED)
    @Param(description = "  the completed date of the job")
    private Date removed;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setDomainId(String domainid) {
        this.domainid = domainid;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public void setJobProcStatus(Integer jobProcStatus) {
        this.jobProcStatus = jobProcStatus;
    }

    public void setJobResultCode(Integer jobResultCode) {
        this.jobResultCode = jobResultCode;
    }

    public void setJobResultType(String jobResultType) {
        this.jobResultType = jobResultType;
    }

    public void setJobResult(ResponseObject jobResult) {
        this.jobResult = jobResult;
    }

    public void setJobInstanceType(String jobInstanceType) {
        this.jobInstanceType = jobInstanceType;
    }

    public void setJobInstanceId(String jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(final Date removed) {
        this.removed = removed;
    }

    public void setMsid(Long msid) {
        this.msid = msid;
    }
}
