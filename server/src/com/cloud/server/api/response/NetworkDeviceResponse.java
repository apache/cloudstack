package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkDeviceResponse extends BaseResponse {
	@SerializedName(ApiConstants.ID)
	@Param(description = "the ID of the network device")
	private IdentityProxy id = new IdentityProxy("host");

	public Long getId() {
		return id.getValue();
	}

	public void setId(Long id) {
		this.id.setValue(id);
	}
}
