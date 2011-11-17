/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ServiceResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME) @Param(description="the service name")
    private String name;
    
    @SerializedName(ApiConstants.PROVIDER) @Param(description="the service provider name")
    private List<ProviderResponse> providers;
    
    @SerializedName("capability") @Param(description="the list of capabilities", responseObject = CapabilityResponse.class)
    private List<CapabilityResponse> capabilities;

    public void setName(String name) {
        this.name = name;
    }

    public void setCapabilities(List<CapabilityResponse> capabilities) {
        this.capabilities = capabilities;
    }

    public void setProviders(List<ProviderResponse> providers) {
        this.providers = providers;
    }
}