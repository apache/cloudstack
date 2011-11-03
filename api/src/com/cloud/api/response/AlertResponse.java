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

import java.util.Date;
import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class AlertResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the alert")
    private IdentityProxy id = new IdentityProxy("alert");

    @SerializedName(ApiConstants.TYPE) @Param(description="the alert type")
    private Short alertType;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of the alert")
    private String description;

    @SerializedName(ApiConstants.SENT) @Param(description="the date and time the alert was sent")
    private Date lastSent;

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setAlertType(Short alertType) {
        this.alertType = alertType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastSent(Date lastSent) {
        this.lastSent = lastSent;
    }
}
