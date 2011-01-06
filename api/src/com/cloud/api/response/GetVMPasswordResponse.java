package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GetVMPasswordResponse extends BaseResponse {
	
	@SerializedName("encryptedpassword") @Param(description="The encrypted password of the VM")
	private String encryptedPassword;
	
	public GetVMPasswordResponse() {}
	
	public GetVMPasswordResponse(String responseName, String encryptedPassword) {
		setResponseName(responseName);
		setObjectName("password");
		setEncryptedPassword(encryptedPassword);
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}
	
}
