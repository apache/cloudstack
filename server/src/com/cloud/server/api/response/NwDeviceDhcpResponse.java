package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NwDeviceDhcpResponse extends NetworkDeviceResponse {
	@SerializedName(ApiConstants.ZONE_ID) @Param(description="Zone where to add PXE server")
    private Long zoneId;
	
	@SerializedName(ApiConstants.POD_ID) @Param(description="Pod where to add PXE server")
    private Long podId;
	
	@SerializedName(ApiConstants.URL) @Param(description="Ip of PXE server")
    private String url;
	
	@SerializedName(ApiConstants.TYPE) @Param(description="Type of add PXE server")
    private String type;
	
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}
	public Long getZoneId() {
		return zoneId;
	}
	
	public void setPodId(Long podId) {
		this.podId = podId;
	}
	public Long getPodId() {
		return podId;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
}
