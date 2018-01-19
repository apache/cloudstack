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

import com.cloud.api.commands.BcfConstants;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.serializer.Param;

@EntityReference(value = BigSwitchBcfDeviceVO.class)
public class BigSwitchBcfDeviceResponse extends BaseResponse {
    @SerializedName(BcfConstants.BIGSWITCH_BCF_DEVICE_ID)
    @Param(description = "device id of the BigSwitch BCF Controller")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network to which this BigSwitch BCF segment belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "name of the provider")
    private String providerName;

    @SerializedName(BcfConstants.BIGSWITCH_BCF_DEVICE_NAME)
    @Param(description = "device name")
    private String deviceName;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the controller Ip address")
    private String hostName;

    @SerializedName(ApiConstants.USERNAME) @Param(description="the controller username")
    private String username;

    @SerializedName(ApiConstants.PASSWORD) @Param(description="the controller password", isSensitive = true)
    private String password;

    @SerializedName(BcfConstants.BIGSWITCH_BCF_DEVICE_NAT)
    @Param(description = "NAT support")
    private Boolean nat;

    public String getId() {
        return this.id;
    }

    public void setId(final String bcfDeviceId) {
        this.id = bcfDeviceId;
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

    public void setUserName(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setNat(final Boolean nat) {
        this.nat = nat;
    }
}
