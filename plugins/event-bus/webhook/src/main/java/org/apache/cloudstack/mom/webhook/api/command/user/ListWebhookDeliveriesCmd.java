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

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.WebhookDelivery;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

@APICommand(name = "listWebhookDeliveries",
        description = "Lists Webhook deliveries",
        responseObject = WebhookResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {WebhookDelivery.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.20.0")
public class ListWebhookDeliveriesCmd extends BaseListCmd {

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

    @Parameter(name = ApiConstants.START_DATE,
            type = CommandType.DATE,
            description = "The start date range for the Webhook delivery " +
                    "(use format \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\"). " +
                    "All deliveries having start date equal to or after the specified date will be listed.")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE,
            type = CommandType.DATE,
            description = "The end date range for the Webhook delivery " +
                    "(use format \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\"). " +
                    "All deliveries having end date equal to or before the specified date will be listed.")
    private Date endDate;

    @Parameter(name = ApiConstants.EVENT_TYPE,
            type = CommandType.STRING,
            description = "The event type of the Webhook delivery")
    private String eventType;

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

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getEventType() {
        return eventType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException {
        ListResponse<WebhookDeliveryResponse> response = webhookApiService.listWebhookDeliveries(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
