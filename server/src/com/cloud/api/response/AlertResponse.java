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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AlertResponse extends BaseResponse {
    @SerializedName("type") @Param(description="the alert type")
    private Short alertType;

    @SerializedName("description") @Param(description="description of the alert")
    private String description;

    @SerializedName("sent") @Param(description="the date and time the alert was sent")
    private Date lastSent;

    public Short getAlertType() {
        return alertType;
    }

    public void setAlertType(Short alertType) {
        this.alertType = alertType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastSent() {
        return lastSent;
    }

    public void setLastSent(Date lastSent) {
        this.lastSent = lastSent;
    }
}
