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

package org.apache.cloudstack.mom.webhook.api.response;


import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.mom.webhook.WebhookDelivery;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {WebhookDelivery.class})
public class WebhookDeliveryResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Webhook delivery")
    private String id;

    @SerializedName(ApiConstants.EVENT_ID)
    @Param(description = "The ID of the event")
    private String eventId;

    @SerializedName(ApiConstants.EVENT_TYPE)
    @Param(description = "The type of the event")
    private String eventType;

    @SerializedName(ApiConstants.WEBHOOK_ID)
    @Param(description = "The ID of the Webhook")
    private String webhookId;

    @SerializedName(ApiConstants.WEBHOOK_NAME)
    @Param(description = "The name of the Webhook")
    private String webhookName;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "The ID of the management server which executed delivery")
    private String managementServerId;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_NAME)
    @Param(description = "The name of the management server which executed delivery")
    private String managementServerName;

    @SerializedName(ApiConstants.HEADERS)
    @Param(description = "The headers of the webhook delivery")
    private String headers;

    @SerializedName(ApiConstants.PAYLOAD)
    @Param(description = "The payload of the webhook delivery")
    private String payload;

    @SerializedName(ApiConstants.SUCCESS)
    @Param(description = "Whether Webhook delivery succeeded or not")
    private boolean success;

    @SerializedName(ApiConstants.RESPONSE)
    @Param(description = "The response of the webhook delivery")
    private String response;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "The start time of the Webhook delivery")
    private Date startTime;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "The end time of the Webhook delivery")
    private Date endTime;

    public void setId(String id) {
        this.id = id;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public void setManagementServerId(String managementServerId) {
        this.managementServerId = managementServerId;
    }

    public void setManagementServerName(String managementServerName) {
        this.managementServerName = managementServerName;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
