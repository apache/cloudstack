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
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.network.cisco.CiscoVnmcController;
import com.google.gson.annotations.SerializedName;
@EntityReference(value = CiscoVnmcController.class)
public class CiscoVnmcResourceResponse extends BaseResponse {
    public static final String RESOURCE_NAME = "resourcename";

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Parameter(description="resource id of the Cisco VNMC controller")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) 
    @Parameter(description="the physical network to which this VNMC belongs to", entityType = PhysicalNetworkResponse.class)
    private Long physicalNetworkId;

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getResourceName() {
        return resourceName;
    }

    @SerializedName(ApiConstants.PROVIDER) @Parameter(description="name of the provider")
    private String providerName;

    @SerializedName(RESOURCE_NAME) 
    @Parameter(description="Cisco VNMC resource name")
    private String resourceName;

    public void setId(String ciscoVnmcResourceId) {
        this.id = ciscoVnmcResourceId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }     

}
