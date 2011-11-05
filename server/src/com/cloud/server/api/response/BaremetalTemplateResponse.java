package com.cloud.server.api.response;

import com.cloud.api.IdentityProxy;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaremetalTemplateResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the template ID")
    private IdentityProxy id = new IdentityProxy("vm_template");
    
    public Long getId() {
        return id.getValue();
    }
    
    public void setId(Long id) {
        this.id.setValue(id);
    }
}
