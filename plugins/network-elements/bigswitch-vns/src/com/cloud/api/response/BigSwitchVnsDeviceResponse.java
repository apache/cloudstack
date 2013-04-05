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
package com.cloud.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import com.cloud.api.commands.VnsConstants;
import com.cloud.network.BigSwitchVnsDeviceVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=BigSwitchVnsDeviceVO.class)
public class BigSwitchVnsDeviceResponse extends BaseResponse {
    @SerializedName(VnsConstants.BIGSWITCH_VNS_DEVICE_ID) @Param(description="device id of the BigSwitch Vns")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this BigSwitch Vns belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private String providerName;

    @SerializedName(VnsConstants.BIGSWITCH_VNS_DEVICE_NAME) @Param(description="device name")
    private String deviceName;

    @SerializedName(ApiConstants.HOST_NAME) @Param(description="the controller Ip address")
    private String hostName;

    public String getId() {
        return this.id;
    }

    public void setId(String vnsDeviceId) {
        this.id = vnsDeviceId;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
