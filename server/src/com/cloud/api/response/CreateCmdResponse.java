package com.cloud.api.response;

import com.cloud.serializer.Param;

public class CreateCmdResponse extends BaseResponse {
    @Param(name="jobid")
    private Long jobId;

    @Param(name="id")
    private Long id;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
