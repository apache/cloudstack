package com.cloud.server.api.response.netapp;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListLunsCmdResponse extends BaseResponse {
	@SerializedName(ApiConstants.ID) @Param(description="lun id")
    private Long id;
 
	@SerializedName(ApiConstants.IQN) @Param(description="lun iqn")
    private String iqn;
	
	@SerializedName(ApiConstants.NAME) @Param(description="lun name")
    private String name;
	
	@SerializedName(ApiConstants.VOLUME_ID) @Param(description="volume id")
    private Long volumeId;
	
	
	public Long getId() {
		return id;
	}
	
	public String getIqn() {
		return iqn;
	}
	
	public String getName() {
		return name;
	}
	
	public Long getVolumeId() {
		return volumeId;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public void setIqn(String iqn) {
		this.iqn = iqn;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setVolumeId(Long id) {
		this.volumeId = id;
	}
}