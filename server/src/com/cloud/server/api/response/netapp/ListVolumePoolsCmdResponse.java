package com.cloud.server.api.response.netapp;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListVolumePoolsCmdResponse extends BaseResponse {
	@SerializedName(ApiConstants.ID) @Param(description="pool id")
    private Long id;
	@SerializedName(ApiConstants.NAME) @Param(description="pool name")
    private String name;
	
	@SerializedName(ApiConstants.ALGORITHM) @Param(description="pool algorithm")
    private String algorithm;
	
	
	public Long getId() {
		return id;
	}
	
	
	public String getName() {
		return name;
	}
	
	public String getAlgorithm() {
		return algorithm;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
}
