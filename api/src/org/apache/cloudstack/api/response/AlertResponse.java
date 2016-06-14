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

import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.alert.Alert;
import com.cloud.serializer.Param;

@EntityReference(value = Alert.class)
@SuppressWarnings("unused")
public class AlertResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the alert")
    private String id;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "One of the following alert types: "
        + "MEMORY = 0, CPU = 1, STORAGE = 2, STORAGE_ALLOCATED = 3, PUBLIC_IP = 4, PRIVATE_IP = 5, SECONDARY_STORAGE = 6, "
        + "HOST = 7, USERVM = 8, DOMAIN_ROUTER = 9, CONSOLE_PROXY = 10, "
        + "ROUTING = 11: lost connection to default route (to the gateway), "
        + "STORAGE_MISC = 12, USAGE_SERVER = 13, MANAGMENT_NODE = 14, DOMAIN_ROUTER_MIGRATE = 15, CONSOLE_PROXY_MIGRATE = 16, "
        + "USERVM_MIGRATE = 17, VLAN = 18, SSVM = 19, USAGE_SERVER_RESULT = 20, STORAGE_DELETE = 21, UPDATE_RESOURCE_COUNT = 22, "
        + "USAGE_SANITY_RESULT = 23, DIRECT_ATTACHED_PUBLIC_IP = 24, LOCAL_STORAGE = 25, RESOURCE_LIMIT_EXCEEDED = 26, "
        + "SYNC = 27, UPLOAD_FAILED = 28, OOBM_AUTH_ERROR = 29")
    private Short alertType;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the alert", since = "4.3")
    private String alertName;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "description of the alert")
    private String description;

    @SerializedName(ApiConstants.SENT)
    @Param(description = "the date and time the alert was sent")
    private Date lastSent;

    public void setId(String id) {
        this.id = id;
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

    public void setName(String name) {
        this.alertName = name;
    }
}
