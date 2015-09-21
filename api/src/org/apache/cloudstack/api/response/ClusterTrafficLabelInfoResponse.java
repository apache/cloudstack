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
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class ClusterTrafficLabelInfoResponse extends BaseResponse {

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_TRAFFIC_ID)
    @Param(description = "id of the physical network traffic")
    private String physicalNetworkTrafficId;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network this traffic belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "the trafficType of the physical network traffic")
    private String trafficType;

    @SerializedName(ApiConstants.NETWORK_LABEL)
    @Param(description = "The network name label of the physical device dedicated to this traffic at cluster")
    private String networkLabel;

    @Override
    public String getObjectId() {
        return this.physicalNetworkTrafficId;
    }

    public void setPhysicalNetworkTrafficId(String physicalNetworkTrafficId) {
        this.physicalNetworkTrafficId = physicalNetworkTrafficId;
    }

    public String getPhysicalNetworkTrafficId() {
        return physicalNetworkTrafficId;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setNetworkLabel(String networkLabel) {
        this.networkLabel = networkLabel;
    }

    public String getNetworkLabel() {
        return networkLabel;
    }
}
