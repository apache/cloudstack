package com.cloud.api;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.CreateCmdResponse;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResponse(long jobId, long objectId) {
        CreateCmdResponse response = new CreateCmdResponse();
        response.setJobId(jobId);
        response.setId(objectId);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response, getResponseType());
    }
}
