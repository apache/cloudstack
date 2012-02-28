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
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class AlertResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the alert")
    private IdentityProxy id = new IdentityProxy("alert");

    @SerializedName(ApiConstants.TYPE) @Param(description="One of the following alert types: " +
      "MEMORY = 0, CPU = 1, STORAGE = 2, STORAGE_ALLOCATED = 3, PUBLIC_IP = 4, PRIVATE_IP = 5, HOST = 6, USERVM = 7, " +
      "DOMAIN_ROUTER = 8, CONSOLE_PROXY = 9, ROUTING = 10: lost connection to default route (to the gateway), " +
      "STORAGE_MISC = 11: lost connection to default route (to the gateway), " +
      "USAGE_SERVER = 12: lost connection to default route (to the gateway), " +
      "MANAGMENT_NODE = 13: lost connection to default route (to the gateway), " +
      "DOMAIN_ROUTER_MIGRATE = 14, CONSOLE_PROXY_MIGRATE = 15, USERVM_MIGRATE = 16, VLAN = 17, SSVM = 18, " +
      "USAGE_SERVER_RESULT = 19")
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
