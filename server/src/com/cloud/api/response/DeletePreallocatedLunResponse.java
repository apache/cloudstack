package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class DeletePreallocatedLunResponse extends BaseResponse {
    @SerializedName("success")
    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
