//
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
//

package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.brocade.Constants;
import com.cloud.serializer.Param;

@EntityReference(value = BrocadeVcsDeviceVO.class)
public class BrocadeVcsDeviceResponse extends BaseResponse {
    @SerializedName(Constants.BROCADE_VCS_DEVICE_ID)
    @Param(description = "Device id of the Brocade Vcs")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "The physical Network to which this Brocade VCS belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "Name of the provider")
    private String providerName;

    @SerializedName(Constants.BROCADE_VCS_DEVICE_NAME)
    @Param(description = "Device name")
    private String deviceName;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "The principal switch Ip address")
    private String hostName;

    public String getId() {
        return id;
    }

    public String getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setId(String vcsDeviceId) {
        this.id = vcsDeviceId;
    }

    public void setPhysicalNetworkId(final String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setProviderName(final String providerName) {
        this.providerName = providerName;
    }

    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

}
