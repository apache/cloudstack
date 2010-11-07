package com.cloud.api;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.CreateCmdResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="id", type=CommandType.LONG)
    private Long id;
    
    public abstract void callCreate() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ResourceUnavailableException,  ConcurrentOperationException, ResourceAllocationException;

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
