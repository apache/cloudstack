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
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

@APICommand(name = "listWebhooks",
        description = "Lists Webhooks",
        responseObject = WebhookResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {Webhook.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.20.0")
public class ListWebhooksCmd extends BaseListProjectAndAccountResourcesCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = WebhookResponse.class,
            description = "The ID of the Webhook")
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "The state of the Webhook")
    private String state;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the Webhook")
    private String name;

    @Parameter(name = ApiConstants.SCOPE,
        type = CommandType.STRING,
        description = "The scope of the Webhook",
        authorized = {RoleType.Admin, RoleType.DomainAdmin})
    private String scope;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public String getScope() {
        return scope;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException {
        ListResponse<WebhookResponse> response = webhookApiService.listWebhooks(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
