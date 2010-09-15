package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class DeletePreallocatedLunResponse implements ResponseObject {
    @Param(name="success")
    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
