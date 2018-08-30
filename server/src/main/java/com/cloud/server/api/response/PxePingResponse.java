// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.server.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.serializer.Param;

public class PxePingResponse extends NwDevicePxeServerResponse {
    @SerializedName(ApiConstants.PING_STORAGE_SERVER_IP)
    @Param(description = "IP of PING storage server")
    private String storageServerIp;

    @SerializedName(ApiConstants.PING_DIR)
    @Param(description = "Direcotry on PING server where to get restore image")
    private String pingDir;

    @SerializedName(ApiConstants.TFTP_DIR)
    @Param(description = "Tftp root directory of PXE server")
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
