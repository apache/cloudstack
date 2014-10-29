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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = PhysicalNetworkServiceProvider.class)
@SuppressWarnings("unused")
public class ProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the provider name")
    private String name;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network this belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.DEST_PHYSICAL_NETWORK_ID)
    @Param(description = "the destination physical network")
    private String destinationPhysicalNetworkId;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the network provider")
    private String state;

    @SerializedName(ApiConstants.ID)
    @Param(description = "uuid of the network provider")
    private String id;

    @SerializedName(ApiConstants.SERVICE_LIST)
    @Param(description = "services for this provider")
    private List<String> services;

    @SerializedName(ApiConstants.CAN_ENABLE_INDIVIDUAL_SERVICE)
    @Param(description = "true if individual services can be enabled/disabled")
    private Boolean canEnableIndividualServices;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getphysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setDestinationPhysicalNetworkId(String destPhysicalNetworkId) {
        this.destinationPhysicalNetworkId = destPhysicalNetworkId;
    }

    public String getDestinationPhysicalNetworkId() {
        return destinationPhysicalNetworkId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }

    public void setId(String uuid) {
        this.id = uuid;
    }

    public String getId() {
        return this.id;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getServices() {
        return services;
    }

    public Boolean getCanEnableIndividualServices() {
        return canEnableIndividualServices;
    }

    public void setCanEnableIndividualServices(Boolean canEnableIndividualServices) {
        this.canEnableIndividualServices = canEnableIndividualServices;
    }
}
