package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class AsyncJobResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="accountid")
    private Long accountId;

    @Param(name="userid")
    private Long userId;

    @Param(name="cmd")
    private String cmd;

    @Param(name="jobstatus")
    private Integer jobStatus;

    @Param(name="jobprocstatus")
    private Integer jobProcStatus;

    @Param(name="jobresultcode")
    private Integer jobResultCode;

    @Param(name="jobresult")
    private String jobResult;

    @Param(name="jobinstancetype")
    private String jobInstanceType;

    @Param(name="jobinstanceid")
    private Long jobInstanceId;

    @Param(name="created")
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
