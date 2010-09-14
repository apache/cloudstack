package com.cloud.api;

import com.cloud.api.response.CreateCmdResponse;
import com.cloud.serializer.SerializerHelper;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="id")
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
        return SerializerHelper.toSerializedString(response);
    }
}
