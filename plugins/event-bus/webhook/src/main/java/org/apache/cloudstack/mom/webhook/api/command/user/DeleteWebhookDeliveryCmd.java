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
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.WebhookDelivery;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteWebhookDelivery",
        description = "Deletes Webhook delivery",
        responseObject = SuccessResponse.class,
        entityType = {WebhookDelivery.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.20.0")
public class DeleteWebhookDeliveryCmd extends BaseCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID,
            entityType = WebhookDeliveryResponse.class,
            description = "The ID of the Webhook delivery")
    private Long id;

    @Parameter(name = ApiConstants.WEBHOOK_ID, type = BaseCmd.CommandType.UUID,
            entityType = WebhookResponse.class,
            description = "The ID of the Webhook")
    private Long webhookId;

    @Parameter(name = ApiConstants.MANAGEMENT_SERVER_ID, type = BaseCmd.CommandType.UUID,
            entityType = ManagementServerResponse.class,
            description = "The ID of the management server",
            authorized = {RoleType.Admin})
    private Long managementServerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public Long getWebhookId() {
        return webhookId;
    }

    public Long getManagementServerId() {
        return managementServerId;
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
            if (!webhookApiService.deleteWebhookDelivery(this)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete webhook delivery");
            }
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
