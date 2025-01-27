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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.WebhookApiService;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteWebhookDeliveryCmdTest {

    @Mock
    WebhookApiService webhookApiService;

    private Object getCommandMethodValue(Object obj, String methodName) {
        Object result = null;
        try {
            Method method = obj.getClass().getMethod(methodName);
            result = method.invoke(obj);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Assert.fail(String.format("Failed to get method %s value", methodName));
        }
        return result;
    }

    private void runStringMemberTest(String memberName) {
        String methodName = "get" + memberName.substring(0, 1).toUpperCase() + memberName.substring(1);
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        ReflectionTestUtils.setField(cmd, memberName, null);
        Assert.assertNull(getCommandMethodValue(cmd, methodName));
        String value = UUID.randomUUID().toString();
        ReflectionTestUtils.setField(cmd, memberName, value);
        Assert.assertEquals(value, getCommandMethodValue(cmd, methodName));
    }

    private void runLongMemberTest(String memberName) {
        String methodName = "get" + memberName.substring(0, 1).toUpperCase() + memberName.substring(1);
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        ReflectionTestUtils.setField(cmd, memberName, null);
        Assert.assertNull(getCommandMethodValue(cmd, methodName));
        Long value = 100L;
        ReflectionTestUtils.setField(cmd, memberName, value);
        Assert.assertEquals(value, getCommandMethodValue(cmd, methodName));
    }

    private void runBooleanMemberTest(String memberName) {
        String methodName = "is" + memberName.substring(0, 1).toUpperCase() + memberName.substring(1);
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        ReflectionTestUtils.setField(cmd, memberName, null);
        Assert.assertNull(getCommandMethodValue(cmd, methodName));
        Boolean value = true;
        ReflectionTestUtils.setField(cmd, memberName, value);
        Assert.assertEquals(value, getCommandMethodValue(cmd, methodName));
    }

    @Test
    public void testGetId() {
        runLongMemberTest("id");
    }

    @Test
    public void testGetWebhookId() {
        runLongMemberTest("webhookId");
    }

    @Test
    public void testGetPayloadUrl() {
        runStringMemberTest("payloadUrl");
    }

    @Test
    public void testGetSecretKey() {
        runStringMemberTest("secretKey");
    }

    @Test
    public void testIsSslVerification() {
        runBooleanMemberTest("sslVerification");
    }

    @Test
    public void testGetPayload() {
        runStringMemberTest("payload");
    }

    @Test
    public void testGetEntityOwnerId() {
        Account account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        Assert.assertEquals(account.getId(), cmd.getEntityOwnerId());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteNullResponse() {
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        cmd.webhookApiService = webhookApiService;
        Mockito.when(webhookApiService.executeWebhookDelivery(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteCRE() {
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        cmd.webhookApiService = webhookApiService;
        Mockito.when(webhookApiService.executeWebhookDelivery(cmd)).thenThrow(CloudRuntimeException.class);
        cmd.execute();
    }

    @Test
    public void testExecute() {
        ExecuteWebhookDeliveryCmd cmd = new ExecuteWebhookDeliveryCmd();
        cmd.webhookApiService = webhookApiService;
        WebhookDeliveryResponse response = new WebhookDeliveryResponse();
        Mockito.when(webhookApiService.executeWebhookDelivery(cmd)).thenReturn(response);
        cmd.execute();
        Assert.assertEquals(cmd.getCommandName(), response.getResponseName());
    }
}
