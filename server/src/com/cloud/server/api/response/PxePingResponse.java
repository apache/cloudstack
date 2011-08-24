package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PxePingResponse extends NwDevicePxeServerResponse {
	@SerializedName(ApiConstants.PING_STORAGE_SERVER_IP) @Param(description="IP of PING storage server")
    private String storageServerIp;
	
	@SerializedName(ApiConstants.PING_DIR) @Param(description="Direcotry on PING server where to get restore image")
    private String pingDir;
	
	@SerializedName(ApiConstants.TFTP_DIR) @Param(description="Tftp root directory of PXE server")
    private String tftpDir;
	
	public void setStorageServerIp(String ip) {
		this.storageServerIp = ip;
	}
	public String getStorageServerIp() {
		return this.storageServerIp;
	}
	
	public void setPingDir(String dir) {
		this.pingDir = dir;
	}
	public String getPingDir() {
		return this.pingDir;
	}
	
	public void setTftpDir(String dir) {
		this.tftpDir = dir;
	}
	public String getTftpDir() {
		return this.tftpDir;
	}
}
