package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NwDevicePxeServerResponse extends NetworkDeviceResponse {

	@SerializedName(ApiConstants.ZONE_ID) @Param(description="Zone where to add PXE server")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
	
	@SerializedName(ApiConstants.POD_ID) @Param(description="Pod where to add PXE server")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");
	
	@SerializedName(ApiConstants.URL) @Param(description="Ip of PXE server")
    private String url;
	
	@SerializedName(ApiConstants.TYPE) @Param(description="Type of add PXE server")
    private String type;
	
	public void setZoneId(Long zoneId) {
		this.zoneId.setValue(zoneId);
	}
	public Long getZoneId() {
		return zoneId.getValue();
	}
	
	public void setPodId(Long podId) {
		this.podId.setValue(podId);
	}
	public Long getPodId() {
		return podId.getValue();
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
