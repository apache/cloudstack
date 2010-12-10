package com.cloud.api;

import com.cloud.api.response.CreateCmdResponse;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="id", type=CommandType.LONG)
    private Long id;
    
    public abstract void create();

    public Long getEntityId() {
        return id;
    }

    public void setEntityId(Long id) {
        this.id = id;
    }

    public String getResponse(long jobId, long objectId) {
        CreateCmdResponse response = new CreateCmdResponse();
        response.setJobId(jobId);
        response.setId(objectId);
        response.setResponseName(getCommandName());
        return _responseGenerator.toSerializedString(response, getResponseType());
    }
}
