package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SuccessResponse extends BaseResponse {
	 @SerializedName("success") @Param(description="true if operation is executed successfully")
	 private Boolean success;
	 
	 public Boolean getSuccess() {
	 	return success;
	 }

	public void setSuccess(Boolean success) {
		this.success = success;
	}
}
