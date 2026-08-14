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

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.api.response.WebhookFilterResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ListWebhookFiltersCmdTest {
    @Mock
    WebhookApiService webhookApiService;

    @Test
    public void executeSetsResponseNameCorrectly() {
        ListWebhookFiltersCmd cmd = new ListWebhookFiltersCmd();
        cmd.webhookApiService = webhookApiService;

        ListResponse<WebhookFilterResponse> response = new ListResponse<>();
        Mockito.when(webhookApiService.listWebhookFilters(cmd)).thenReturn(response);

        cmd.execute();

        Assert.assertNotNull(cmd.getResponseObject());
    }

    @Test(expected = ServerApiException.class)
    public void executeThrowsExceptionWhenServiceFails() {
        ListWebhookFiltersCmd cmd = new ListWebhookFiltersCmd();
        cmd.webhookApiService = webhookApiService;

        Mockito.doThrow(new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Service failure")).when(webhookApiService).listWebhookFilters(cmd);

        cmd.execute();
    }

    @Test
    public void getIdReturnsCorrectValue() {
        ListWebhookFiltersCmd cmd = new ListWebhookFiltersCmd();
        ReflectionTestUtils.setField(cmd, "id", 123L);

        Assert.assertEquals(Long.valueOf(123L), cmd.getId());
    }

    @Test
    public void getWebhookIdReturnsCorrectValue() {
        ListWebhookFiltersCmd cmd = new ListWebhookFiltersCmd();
        ReflectionTestUtils.setField(cmd, "webhookId", 456L);

        Assert.assertEquals(Long.valueOf(456L), cmd.getWebhookId());
    }
}
