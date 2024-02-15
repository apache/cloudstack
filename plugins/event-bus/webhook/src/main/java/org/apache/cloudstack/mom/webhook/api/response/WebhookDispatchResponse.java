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
import org.apache.cloudstack.mom.webhook.WebhookDispatch;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {WebhookDispatch.class})
public class WebhookDispatchResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Webhook dispatch")
    private String id;

    @SerializedName(ApiConstants.EVENT_ID)
    @Param(description = "The ID of the event")
    private String eventId;

    @SerializedName(ApiConstants.EVENT_TYPE)
    @Param(description = "The type of the event")
    private String eventType;

    @SerializedName(ApiConstants.WEBHOOK_RULE_ID)
    @Param(description = "The ID of the Webhook rule")
    private String webhookRuleId;

    @SerializedName(ApiConstants.WEBHOOK_RULE_NAME)
    @Param(description = "The name of the Webhook rule")
    private String webhookRuleName;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "The ID of the management server which executed dispatch")
    private String managementServerId;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_NAME)
    @Param(description = "The name of the management server which executed dispatch")
    private String managementServerName;

    @SerializedName(ApiConstants.PAYLOAD)
    @Param(description = "The payload of the webhook dispatch")
    private String payload;

    @SerializedName(ApiConstants.SUCCESS)
    @Param(description = "Whether Webhook dispatch succeeded or not")
    private boolean success;

    @SerializedName(ApiConstants.RESPONSE)
    @Param(description = "The response of the webhook dispatch")
    private String response;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "The start time of the Webhook dispatch")
    private Date startTime;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "The end time of the Webhook dispatch")
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

    public void setWebhookRuleId(String webhookRuleId) {
        this.webhookRuleId = webhookRuleId;
    }

    public void setWebhookRuleName(String webhookRuleName) {
        this.webhookRuleName = webhookRuleName;
    }

    public void setManagementServerId(String managementServerId) {
        this.managementServerId = managementServerId;
    }

    public void setManagementServerName(String managementServerName) {
        this.managementServerName = managementServerName;
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
