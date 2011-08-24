/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
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
