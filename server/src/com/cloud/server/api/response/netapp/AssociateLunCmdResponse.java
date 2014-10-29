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
package com.cloud.server.api.response.netapp;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class AssociateLunCmdResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the LUN id")
    private String lun;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the IP address of ")
    private String ipAddress;

    @SerializedName(ApiConstants.TARGET_IQN)
    @Param(description = "the target IQN")
    private String targetIQN;

    public String getLun() {
        return lun;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getTargetIQN() {
        return targetIQN;
    }

    public void setLun(String lun) {
        this.lun = lun;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setTargetIQN(String targetIQN) {
        this.targetIQN = targetIQN;
    }

}
