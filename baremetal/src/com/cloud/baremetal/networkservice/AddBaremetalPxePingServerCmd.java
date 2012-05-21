package com.cloud.baremetal.networkservice;

import com.cloud.api.ApiConstants;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;

public class AddBaremetalPxePingServerCmd extends AddBaremetalPxeCmd {

    @Parameter(name=ApiConstants.PING_STORAGE_SERVER_IP, type=CommandType.STRING, required = true, description="PING storage server ip")
    private String pingStorageServerIp;
    
    @Parameter(name=ApiConstants.PING_DIR, type=CommandType.STRING, required = true, description="Root directory on PING storage server")
    private String pingDir;
    
    @Parameter(name=ApiConstants.TFTP_DIR, type=CommandType.STRING, required = true, description="Tftp root directory of PXE server")
    private String tftpDir;
    
    @Parameter(name=ApiConstants.PING_CIFS_USERNAME, type=CommandType.STRING, required = true, description="Username of PING storage server")
    private String pingStorageServerUserName;
    
    @Parameter(name=ApiConstants.PING_CIFS_PASSWORD, type=CommandType.STRING, required = true, description="Password of PING storage server")
    private String pingStorageServerPassword;

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

    public String getPingStorageServerUserName() {
        return pingStorageServerUserName;
    }

    public void setPingStorageServerUserName(String pingStorageServerUserName) {
        this.pingStorageServerUserName = pingStorageServerUserName;
    }

    public String getPingStorageServerPassword() {
        return pingStorageServerPassword;
    }

    public void setPingStorageServerPassword(String pingStorageServerPassword) {
        this.pingStorageServerPassword = pingStorageServerPassword;
    }
}
