package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PxeServerResponse extends BaseResponse {
	@SerializedName(ApiConstants.ID) @Param(description="the ID of the PXE server")
    private Long id;
	
	public Long getId() {
    	return id;
    }
    
    public void setId(Long id) {
    	this.id = id;
    }
}
