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

package org.apache.cloudstack.mom.webhook;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.mom.webhook.api.command.user.CreateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ExecuteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhookDeliveriesCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhooksCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.UpdateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;

import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface WebhookApiService extends PluggableService {

    ListResponse<WebhookResponse> listWebhooks(ListWebhooksCmd cmd);
    WebhookResponse createWebhook(CreateWebhookCmd cmd) throws CloudRuntimeException;
    boolean deleteWebhook(DeleteWebhookCmd cmd) throws CloudRuntimeException;
    WebhookResponse updateWebhook(UpdateWebhookCmd cmd) throws CloudRuntimeException;
    WebhookResponse createWebhookResponse(long webhookId);
    ListResponse<WebhookDeliveryResponse> listWebhookDeliveries(ListWebhookDeliveriesCmd cmd);
    int deleteWebhookDelivery(DeleteWebhookDeliveryCmd cmd) throws CloudRuntimeException;
    WebhookDeliveryResponse executeWebhookDelivery(ExecuteWebhookDeliveryCmd cmd) throws CloudRuntimeException;
}
