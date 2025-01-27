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
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createWebhook",
        description = "Creates a Webhook",
        responseObject = WebhookResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {Webhook.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.20.0")
public class CreateWebhookCmd extends BaseCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING, required = true, description = "Name for the Webhook")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = BaseCmd.CommandType.STRING, description = "Description for the Webhook")
    private String description;

    @Parameter(name = ApiConstants.STATE, type = BaseCmd.CommandType.STRING, description = "State of the Webhook")
    private String state;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.ACCOUNT, type = BaseCmd.CommandType.STRING, description = "An optional account for the" +
            " Webhook. Must be used with domainId.")
    private String accountName;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.DOMAIN_ID, type = BaseCmd.CommandType.UUID, entityType = DomainResponse.class,
            description = "an optional domainId for the Webhook. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.PROJECT_ID, type = BaseCmd.CommandType.UUID, entityType = ProjectResponse.class,
            description = "Project for the Webhook")
    private Long projectId;

    @Parameter(name = ApiConstants.PAYLOAD_URL,
            type = BaseCmd.CommandType.STRING,
            required = true,
            description = "Payload URL of the Webhook")
    private String payloadUrl;

    @Parameter(name = ApiConstants.SECRET_KEY, type = BaseCmd.CommandType.STRING, description = "Secret key of the Webhook")
    private String secretKey;

    @Parameter(name = ApiConstants.SSL_VERIFICATION, type = BaseCmd.CommandType.BOOLEAN, description = "If set to true then SSL verification will be done for the Webhook otherwise not")
    private Boolean sslVerification;

    @Parameter(name = ApiConstants.SCOPE, type = BaseCmd.CommandType.STRING, description = "Scope of the Webhook",
        authorized = {RoleType.Admin, RoleType.DomainAdmin})
    private String scope;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getState() {
        return state;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getPayloadUrl() {
        return payloadUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isSslVerification() {
        return Boolean.TRUE.equals(sslVerification);
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
            WebhookResponse response = webhookApiService.createWebhook(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create webhook");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
