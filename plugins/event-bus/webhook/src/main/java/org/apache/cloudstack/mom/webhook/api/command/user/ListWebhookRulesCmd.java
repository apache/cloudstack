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
import org.apache.cloudstack.mom.webhook.WebhookRule;
import org.apache.cloudstack.mom.webhook.api.response.WebhookRuleResponse;

@APICommand(name = "listWebhooks",
        description = "Lists Webhook rules",
        responseObject = WebhookRuleResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        entityType = {WebhookRule.class})
public class ListWebhookRulesCmd extends BaseListProjectAndAccountResourcesCmd {

    @Inject
    WebhookApiService webhookApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = WebhookRuleResponse.class,
            description = "The ID of the Webhook rules")
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "The state of the Webhook rule")
    private String state;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the Webhook rule")
    private String name;

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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException {
        ListResponse<WebhookRuleResponse> response = webhookApiService.listWebhookRules(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
