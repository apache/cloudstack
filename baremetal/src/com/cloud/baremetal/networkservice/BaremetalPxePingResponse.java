package com.cloud.baremetal.networkservice;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaremetalPxePingResponse extends BaremetalPxeResponse {
    @SerializedName(ApiConstants.PING_STORAGE_SERVER_IP) @Param(description="PING storage server ip")
    private String pingStorageServerIp;
    
    @SerializedName(ApiConstants.PING_DIR) @Param(description="Root directory on PING storage server")
    private String pingDir;
    
    @SerializedName(ApiConstants.TFTP_DIR) @Param(description="Tftp root directory of PXE server")
    private String tftpDir;

    public String getPingStorageServerIp() {
        return pingStorageServerIp;
    }

    public void setPingStorageServerIp(String pingStorageServerIp) {
        this.pingStorageServerIp = pingStorageServerIp;
    }

    public String getPingDir() {
        return pingDir;
    }

    public void setPingDir(String pingDir) {
        this.pingDir = pingDir;
    }

    public String getTftpDir() {
        return tftpDir;
    }

    public void setTftpDir(String tftpDir) {
        this.tftpDir = tftpDir;
    }
}
