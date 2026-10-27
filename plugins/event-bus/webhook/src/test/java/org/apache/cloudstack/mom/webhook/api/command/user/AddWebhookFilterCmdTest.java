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

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.api.response.WebhookFilterResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class AddWebhookFilterCmdTest {
    @Mock
    WebhookApiService webhookApiService;

    @Test
    public void executeAddsWebhookFilterSuccessfully() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        cmd.webhookApiService = webhookApiService;

        WebhookFilterResponse response = Mockito.mock(WebhookFilterResponse.class);
        Mockito.when(webhookApiService.addWebhookFilter(cmd)).thenReturn(response);

        cmd.execute();

        Mockito.verify(webhookApiService, Mockito.times(1)).addWebhookFilter(cmd);
        Assert.assertNotNull(cmd.getResponseObject());
        Assert.assertEquals(response, cmd.getResponseObject());
    }

    @Test(expected = ServerApiException.class)
    public void executeThrowsExceptionWhenServiceReturnsNull() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        cmd.webhookApiService = webhookApiService;

        Mockito.when(webhookApiService.addWebhookFilter(cmd)).thenReturn(null);

        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void executeThrowsExceptionWhenServiceFails() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        cmd.webhookApiService = webhookApiService;

        Mockito.doThrow(new CloudRuntimeException("Service failure")).when(webhookApiService).addWebhookFilter(cmd);

        cmd.execute();
    }

    @Test
    public void getEntityOwnerIdReturnsCorrectOwnerId() {
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(123L);
        CallContext.register(Mockito.mock(User.class), account);

        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();

        Assert.assertEquals(123L, cmd.getEntityOwnerId());
    }

    @Test
    public void getModeReturnsCorrectValue() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        ReflectionTestUtils.setField(cmd, "mode", "Include");

        Assert.assertEquals("Include", cmd.getMode());
    }

    @Test
    public void getMatchTypeReturnsCorrectValue() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        ReflectionTestUtils.setField(cmd, "matchType", "Exact");

        Assert.assertEquals("Exact", cmd.getMatchType());
    }

    @Test
    public void getValueReturnsCorrectValue() {
        AddWebhookFilterCmd cmd = new AddWebhookFilterCmd();
        ReflectionTestUtils.setField(cmd, "value", "testValue");

        Assert.assertEquals("testValue", cmd.getValue());
    }
}
