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

import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.serializer.Param;

@EntityReference(value = NiciraNvpDeviceVO.class)
public class NiciraNvpDeviceResponse extends BaseResponse {
    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_ID)
    @Param(description = "device id of the Nicire Nvp")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network to which this Nirica Nvp belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "name of the provider")
    private String providerName;

    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_NAME)
    @Param(description = "device name")
    private String deviceName;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the controller Ip address")
    private String hostName;

    @SerializedName(ApiConstants.NICIRA_NVP_TRANSPORT_ZONE_UUID)
    @Param(description = "the transport zone Uuid")
    private String transportZoneUuid;

    @SerializedName(ApiConstants.NICIRA_NVP_GATEWAYSERVICE_UUID)
    @Param(description = "this L3 gateway service Uuid")
    private String l3GatewayServiceUuid;

    public void setId(String nvpDeviceId) {
        this.id = nvpDeviceId;
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

    public void setTransportZoneUuid(final String transportZoneUuid) {
        this.transportZoneUuid = transportZoneUuid;
    }

    public void setL3GatewayServiceUuid(final String l3GatewayServiceUuid) {
        this.l3GatewayServiceUuid = l3GatewayServiceUuid;
    }

}
