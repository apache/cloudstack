package com.cloud.api.response;

import com.cloud.serializer.Param;

public class DeletePreallocatedLunResponse extends BaseResponse {
    @Param(name="success")
    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
