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

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME) @Param(description="the provider name")
    private String name;
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network this belongs to")
    private Long physicalNetworkId;

    @SerializedName(ApiConstants.DEST_PHYSICAL_NETWORK_ID) @Param(description="the destination physical network")
    private Long destinationPhysicalNetworkId;
    
    @SerializedName(ApiConstants.STATE) @Param(description="state of the network provider")
    private String state;

    @SerializedName(ApiConstants.ID) @Param(description="id of the network provider")
    private Long id;
    
    
    public void setName(String name) {
        this.name = name;
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public long getphysicalNetworkId() {
        return physicalNetworkId;
    }
    
    public void setDestinationPhysicalNetworkId(long destPhysicalNetworkId) {
        this.destinationPhysicalNetworkId = destPhysicalNetworkId;
    }

    public long getDestinationPhysicalNetworkId() {
        return destinationPhysicalNetworkId;
    }

    public void setState(String state) {
        this.state = state;
    }
    
    public String getState() {
        return this.state;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return this.id;
    }
    
}
