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
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestOSResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the OS type")
    private IdentityProxy id = new IdentityProxy("guest_os");

    @SerializedName(ApiConstants.OS_CATEGORY_ID) @Param(description="the ID of the OS category")
    private IdentityProxy osCategoryId = new IdentityProxy("guest_os_category");

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="the name/description of the OS type")
    private String description;

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public Long getOsCategoryId() {
        return osCategoryId.getValue();
    }

    public void setOsCategoryId(Long osCategoryId) {
        this.osCategoryId.setValue(osCategoryId);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
