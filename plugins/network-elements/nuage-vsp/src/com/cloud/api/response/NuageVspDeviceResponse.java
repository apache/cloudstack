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

import com.cloud.api.commands.VspConstants;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = NuageVspDeviceVO.class)
public class NuageVspDeviceResponse extends BaseResponse {
    @SerializedName(VspConstants.NUAGE_VSP_DEVICE_ID)
    @Param(description = "the device id of the Nuage VSD")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the ID of the physical network to which this Nuage VSP belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "the service provider name corresponding to this Nuage VSP device")
    private String providerName;

    @SerializedName(VspConstants.NUAGE_VSP_DEVICE_NAME)
    @Param(description = "the name of the Nuage VSP device")
    private String deviceName;

    @SerializedName(VspConstants.NUAGE_VSP_API_PORT)
    @Param(description = "the port to communicate to Nuage VSD")
    private int port;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the hostname of the Nuage VSD")
    private String hostName;

    @SerializedName(VspConstants.NUAGE_VSP_API_VERSION)
    @Param(description = "the version of the API to use to communicate to Nuage VSD")
    private String apiVersion;

    @SerializedName(VspConstants.NUAGE_VSP_API_RETRY_COUNT)
    @Param(description = "the number of retries on failure to communicate to Nuage VSD")
    private int apiRetryCount;

    @SerializedName(VspConstants.NUAGE_VSP_API_RETRY_INTERVAL)
    @Param(description = "the time to wait after failure before retrying to communicate to Nuage VSD")
    private long apiRetryInterval;

    public void setId(String vspDetailsId) {
        this.id = vspDetailsId;
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

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setApiRetryCount(int apiRetryCount) {
        this.apiRetryCount = apiRetryCount;
    }

    public void setApiRetryInterval(long apiRetryInterval) {
        this.apiRetryInterval = apiRetryInterval;
    }

}
