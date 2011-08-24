package com.cloud.server.api.response;

import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaremetalTemplateResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the template ID")
    private long id;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
}
