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
import org.apache.cloudstack.mom.webhook.WebhookFilter;
import org.apache.cloudstack.mom.webhook.api.response.WebhookFilterResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addWebhookFilter",
        description = "Adds a Webhook filter",
        responseObject = WebhookResponse.class,
        entityType = {WebhookFilter.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.23.0")
public class AddWebhookFilterCmd extends BaseCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.WEBHOOK_ID, type = CommandType.UUID, required = true,
            entityType = WebhookResponse.class, description = "ID for the Webhook")
    private Long id;

    @Parameter(name = ApiConstants.MODE, type = BaseCmd.CommandType.STRING,
            description = "Mode for the Webhook filter - Include or Exclude")
    private String mode;

    @Parameter(name = ApiConstants.MATCH_TYPE, type = BaseCmd.CommandType.STRING,
            description = "Match type for the Webhook filter - Exact, Prefix, Suffix or Contains")
    private String matchType;

    @Parameter(name = ApiConstants.VALUE, type = BaseCmd.CommandType.STRING, required = true,
            description = "Value for the Webhook which that will be matched")
    private String value;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////



    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getType() {
        return WebhookFilter.Type.EventType.name();
    }

    public String getMode() {
        return mode;
    }

    public String getMatchType() {
        return matchType;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            WebhookFilterResponse response = webhookApiService.addWebhookFilter(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add webhook filter");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
