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

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

public class NiciraNvpDeviceResponse extends BaseResponse {
    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_ID) @Param(description="device id of the Nicire Nvp")
    private IdentityProxy id = new IdentityProxy("external_nicira_nvp_devices");
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this Nirica Nvp belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");
    
    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private String providerName;
    
    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_NAME) @Param(description="device name")
    private String deviceName;

    public void setId(long nvpDeviceId) {
        this.id.setValue(nvpDeviceId);
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }     
    
}
