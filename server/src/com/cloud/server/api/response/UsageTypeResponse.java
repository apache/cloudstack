/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class UsageTypeResponse extends BaseResponse {

    @SerializedName("usagetypeid") @Param(description="usage type")
    private Integer usageType;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of usage type")
    private String description;

    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

    public UsageTypeResponse(Integer usageType, String description){
    	this.usageType = usageType;
    	this.description = description;
    	setObjectName("usagetype");
    }

}
