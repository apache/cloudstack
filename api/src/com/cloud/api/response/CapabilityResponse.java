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

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapabilityResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME) @Param(description="the capability name")
    private String name;
    
    @SerializedName(ApiConstants.VALUE) @Param(description="the capability value")
    private String value;

    @SerializedName(ApiConstants.CAN_CHOOSE_SERVICE_CAPABILITY) @Param(description="can this service capability value can be choosable while creatine network offerings")
    private boolean canChoose;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getCanChoose() {
        return canChoose;
    }

    public void setCanChoose(boolean choosable) {
        this.canChoose = choosable;
    }
}
