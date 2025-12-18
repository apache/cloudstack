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

package org.apache.cloudstack.mom.webhook.api.command.user;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.WebhookDelivery;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

import com.cloud.utils.exception.CloudRuntimeException;


@APICommand(name = "executeWebhookDelivery",
        description = "Executes a Webhook delivery",
        responseObject = WebhookDeliveryResponse.class,
        entityType = {WebhookDelivery.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.20.0")
public class ExecuteWebhookDeliveryCmd extends BaseCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = WebhookDeliveryResponse.class,
            description = "The ID of the Webhook delivery for redelivery")
    private Long id;

    @Parameter(name = ApiConstants.WEBHOOK_ID, type = CommandType.UUID,
            entityType = WebhookResponse.class,
            description = "The ID of the Webhook")
    private Long webhookId;

    @Parameter(name = ApiConstants.PAYLOAD_URL,
            type = BaseCmd.CommandType.STRING,
            description = "Payload URL of the Webhook delivery")
    private String payloadUrl;

    @Parameter(name = ApiConstants.SECRET_KEY, type = BaseCmd.CommandType.STRING, description = "Secret key of the Webhook delivery")
    private String secretKey;

    @Parameter(name = ApiConstants.SSL_VERIFICATION, type = BaseCmd.CommandType.BOOLEAN, description = "If set to true then SSL verification will be done for the Webhook delivery otherwise not")
    private Boolean sslVerification;

    @Parameter(name = ApiConstants.PAYLOAD,
            type = BaseCmd.CommandType.STRING,
            description = "Payload of the Webhook delivery")
    private String payload;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }

    public Long getWebhookId() {
        return webhookId;
    }

    public String getPayloadUrl() {
        return payloadUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Boolean isSslVerification() {
        return sslVerification;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            WebhookDeliveryResponse response = webhookApiService.executeWebhookDelivery(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to test Webhook delivery");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }

    }
}
