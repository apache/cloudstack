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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.network.cisco.CiscoAsa1000vDevice;

@EntityReference(value = CiscoAsa1000vDevice.class)
public class CiscoAsa1000vResourceResponse extends BaseResponse {

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Parameter(description = "Resource ID of the Cisco ASA 1000v appliance")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Parameter(description = "The physical Network to which this ASA 1000v belongs to", entityType = PhysicalNetworkResponse.class)
    private Long physicalNetworkId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Parameter(description = "Management IP address of ASA 1000v")
    private String managementIp;

    @SerializedName(ApiConstants.ASA_INSIDE_PORT_PROFILE)
    @Parameter(description = "Port profile associated with inside interface of ASA 1000v")
    private String inPortProfile;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Parameter(description = "The guest network to which ASA 1000v is associated", entityType = NetworkResponse.class)
    private Long guestNetworkId;

    public String getId() {
        return id;
    }

    public void setId(String ciscoAsa1000vResourceId) {
        this.id = ciscoAsa1000vResourceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getInPortProfile() {
        return inPortProfile;
    }

    public void setInPortProfile(String inPortProfile) {
        this.inPortProfile = inPortProfile;
    }

    public Long getGuestNetworkId() {
        return guestNetworkId;
    }

    public void setGuestNetworkId(Long guestNetworkId) {
        this.guestNetworkId = guestNetworkId;
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }
}
