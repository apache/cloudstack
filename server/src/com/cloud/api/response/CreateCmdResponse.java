package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class CreateCmdResponse extends BaseResponse {
    @SerializedName("jobid")
    private Long jobId;

    @SerializedName("id")
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
