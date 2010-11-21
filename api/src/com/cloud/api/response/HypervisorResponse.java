package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.google.gson.annotations.SerializedName;

public class HypervisorResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
