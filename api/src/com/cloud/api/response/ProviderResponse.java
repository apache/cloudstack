/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME) @Param(description="the provider name")
    private String name;
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network this belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");

    @SerializedName(ApiConstants.DEST_PHYSICAL_NETWORK_ID) @Param(description="the destination physical network")
    private IdentityProxy destinationPhysicalNetworkId = new IdentityProxy("physical_network");
    
    @SerializedName(ApiConstants.STATE) @Param(description="state of the network provider")
    private String state;

    @SerializedName(ApiConstants.ID) @Param(description="uuid of the network provider")
    private String id;
    
    @SerializedName(ApiConstants.SERVICE_LIST) @Param(description="services for this provider")
    private List<String> services;
    
    @SerializedName(ApiConstants.CAN_ENABLE_INDIVIDUAL_SERVICE) @Param(description="true if individual services can be enabled/disabled")
    private Boolean canEnableIndividualServices;
    
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
    }

    public long getphysicalNetworkId() {
        return physicalNetworkId.getValue();
    }
    
    public void setDestinationPhysicalNetworkId(long destPhysicalNetworkId) {
        this.destinationPhysicalNetworkId.setValue(destPhysicalNetworkId);
    }

    public long getDestinationPhysicalNetworkId() {
        return destinationPhysicalNetworkId.getValue();
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
