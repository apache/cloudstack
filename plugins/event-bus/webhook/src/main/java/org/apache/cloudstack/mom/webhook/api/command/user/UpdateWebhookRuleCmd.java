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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.WebhookRule;
import org.apache.cloudstack.mom.webhook.api.response.WebhookRuleResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "updateWebhookRule",
        description = "Update a Webhook rule",
        responseObject = SuccessResponse.class,
        entityType = {WebhookRule.class},
        authorized = {RoleType.Admin})
public class UpdateWebhookRuleCmd extends BaseCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = WebhookRuleResponse.class,
            description = "The ID of the Webhook rule",
            required = true)
    private Long id;
    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING, description = "Name for the Webhook rule")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = BaseCmd.CommandType.STRING, description = "Description for the Webhook rule")
    private String description;

    @Parameter(name = ApiConstants.STATE, type = BaseCmd.CommandType.STRING, description = "State of the Webhook rule")
    private String state;

    @Parameter(name = ApiConstants.PAYLOAD_URL,
            type = BaseCmd.CommandType.STRING,
            description = "Payload URL of the Webhook rule")
    private String payloadUrl;

    @Parameter(name = ApiConstants.SECRET_KEY, type = BaseCmd.CommandType.STRING, description = "Secret key of the Webhook rule")
    private String secretKey;

    @Parameter(name = ApiConstants.SSL_VERIFICATION, type = BaseCmd.CommandType.BOOLEAN, description = "If set to true then SSL verification will be done for the Webhook rule otherwise not")
    private Boolean sslVerification;

    @Parameter(name = ApiConstants.SCOPE, type = BaseCmd.CommandType.STRING, description = "Scope of the Webhook rule",
            authorized = {RoleType.Admin, RoleType.DomainAdmin})
    private String scope;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getState() {
        return state;
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

    public String getScope() {
        return scope;
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
            WebhookRuleResponse response = webhookApiService.updateWebhookRule(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Webhook rule");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
