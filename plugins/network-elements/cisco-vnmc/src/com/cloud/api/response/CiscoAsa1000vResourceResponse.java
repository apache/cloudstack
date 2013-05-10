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
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.network.cisco.CiscoAsa1000vDevice;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = CiscoAsa1000vDevice.class)
public class CiscoAsa1000vResourceResponse extends BaseResponse {
    public static final String RESOURCE_NAME = "resourcename";

    @SerializedName(ApiConstants.RESOURCE_ID) @Parameter(description="resource id of the Cisco ASA 1000v appliance")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) 
    @Parameter(description="the physical network to which this ASA 1000v belongs to", entityType = PhysicalNetworkResponse.class)
    private Long physicalNetworkId ;

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @SerializedName(ApiConstants.HOST_NAME)
    @Parameter(description="management ip address of ASA 1000v")
    private String managementIp;

    public String getManagementIp() {
        return managementIp;
    }

    @SerializedName(ApiConstants.ASA_INSIDE_PORT_PROFILE)
    @Parameter(description="management ip address of ASA 1000v")
    private String inPortProfile;

    public String getInPortProfile() {
        return inPortProfile;
    }

    @SerializedName(ApiConstants.NETWORK_ID)
    @Parameter(description="the guest network to which ASA 1000v is associated", entityType = NetworkResponse.class)
    private Long guestNetworkId;

    public Long getGuestNetworkId() {
        return guestNetworkId;
    }

    public void setId(String ciscoAsa1000vResourceId) {
        this.id = ciscoAsa1000vResourceId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public void setInPortProfile(String inPortProfile) {
        this.inPortProfile = inPortProfile;
    }     

    public void setGuestNetworkId(Long guestNetworkId) {
        this.guestNetworkId = guestNetworkId;
    }
}
