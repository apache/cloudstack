package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class HypervisorResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME) @Param(description="Hypervisor name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
