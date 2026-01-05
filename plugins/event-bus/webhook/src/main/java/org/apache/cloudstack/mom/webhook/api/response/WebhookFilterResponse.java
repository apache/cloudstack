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
import org.apache.cloudstack.mom.webhook.WebhookFilter;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {WebhookFilter.class})
public class WebhookFilterResponse  extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Webhook filter")
    private String id;

    @SerializedName(ApiConstants.WEBHOOK_ID)
    @Param(description = "The ID of the Webhook")
    private String webhookId;

    @SerializedName(ApiConstants.WEBHOOK_NAME)
    @Param(description = "The name of the Webhook")
    private String webhookName;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The type of the Webhook filter")
    private String type;

    @SerializedName(ApiConstants.MODE)
    @Param(description = "The type of the Webhook filter")
    private String mode;

    @SerializedName(ApiConstants.MATCH_TYPE)
    @Param(description = "The type of the Webhook filter")
    private String matchType;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "The type of the Webhook filter")
    private String value;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The type of the Webhook filter")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
