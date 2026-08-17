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
package com.cloud.api.response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Test;

import com.cloud.api.ApiDBUtils;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.User;

public class ApiResponseSerializerTest {

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        setManagementServer(null);
    }

    private void overrideDefaultConfigValue(ConfigKey configKey, String value) throws NoSuchFieldException, IllegalAccessException {
        Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }

    private void setManagementServer(ManagementServer ms) throws Exception {
        Field smsField = ApiDBUtils.class.getDeclaredField("s_ms");
        smsField.setAccessible(true);
        smsField.set(null, ms);
    }

    private void registerAuthenticatedCallContext() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(Account.ACCOUNT_ID_SYSTEM + 1);
        CallContext.register(mock(User.class), account);
    }

    @Test
    public void toXmlSerializedStringIncludesVersionWhenExposedAndAuthenticated() throws Exception {
        overrideDefaultConfigValue(ManagementServer.exposeCloudStackVersionInApiXmlResponse, "true");
        ManagementServer ms = mock(ManagementServer.class);
        when(ms.getVersion()).thenReturn("1.2.3-test");
        setManagementServer(ms);
        registerAuthenticatedCallContext();

        String xml = ApiResponseSerializer.toSerializedString(new SuccessResponse("success"), "xml");

        assertTrue(xml.contains("cloud-stack-version=\"1.2.3-test\""));
    }

    @Test
    public void toXmlSerializedStringOmitsVersionWhenNotExposed() throws Exception {
        overrideDefaultConfigValue(ManagementServer.exposeCloudStackVersionInApiXmlResponse, "false");
        registerAuthenticatedCallContext();

        String xml = ApiResponseSerializer.toSerializedString(new SuccessResponse("success"), "xml");

        assertFalse(xml.contains("cloud-stack-version="));
    }

    @Test
    public void toXmlSerializedStringOmitsVersionForSystemAccountEvenWhenExposed() throws Exception {
        overrideDefaultConfigValue(ManagementServer.exposeCloudStackVersionInApiXmlResponse, "true");
        ManagementServer ms = mock(ManagementServer.class);
        when(ms.getVersion()).thenReturn("1.2.3-test");
        setManagementServer(ms);
        Account systemAccount = mock(Account.class);
        when(systemAccount.getId()).thenReturn(Account.ACCOUNT_ID_SYSTEM);
        CallContext.register(mock(User.class), systemAccount);

        String xml = ApiResponseSerializer.toSerializedString(new SuccessResponse("success"), "xml");

        assertFalse(xml.contains("cloud-stack-version="));
    }
}
