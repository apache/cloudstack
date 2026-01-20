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
package org.apache.cloudstack.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.network.netris.NetrisProvider;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.service.NetrisProviderService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

public class AddNetrisProviderCmdTest {

    @Mock
    private NetrisProviderService netrisProviderService;

    @Mock
    private CallContext callContext;

    @Mock
    Object _responseObject;

    private MockedStatic<CallContext> callContextMockedStatic;

    @InjectMocks
    private AddNetrisProviderCmd cmd;

    private AutoCloseable closeable;

    private static final long ZONE_ID = 1L;
    private static final String NAME = "test-provider";
    private static final String URL = "http://domain.provider.dev";
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-password";
    private static final String SITE_NAME = "test-site";
    private static final String TENANT_NAME = "test-tenant";
    private static final String NETRIS_TAG = "test-tag";

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        callContextMockedStatic = Mockito.mockStatic(CallContext.class);
        callContextMockedStatic.when(CallContext::current).thenReturn(callContext);

        // Set private fields using reflection
        setPrivateField("zoneId", ZONE_ID);
        setPrivateField("name", NAME);
        setPrivateField("url", URL);
        setPrivateField("username", USERNAME);
        setPrivateField("password", PASSWORD);
        setPrivateField("siteName", SITE_NAME);
        setPrivateField("tenantName", TENANT_NAME);
        setPrivateField("netrisTag", NETRIS_TAG);
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = AddNetrisProviderCmd.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(cmd, value);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        callContextMockedStatic.close();
    }

    @Test
    public void testExecuteSuccess() throws ConcurrentOperationException {
        NetrisProvider provider = Mockito.mock(NetrisProvider.class);
        NetrisProviderResponse response = Mockito.mock(NetrisProviderResponse.class);
        Mockito.when(netrisProviderService.addProvider(cmd)).thenReturn(provider);
        Mockito.when(netrisProviderService.createNetrisProviderResponse(provider)).thenReturn(response);
        cmd.execute();
        Mockito.verify(netrisProviderService).addProvider(cmd);
        Mockito.verify(netrisProviderService).createNetrisProviderResponse(provider);
        Mockito.verify(response).setResponseName(cmd.getCommandName());
        Assert.assertEquals(response, cmd.getResponseObject());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteFailure() throws ConcurrentOperationException {
        NetrisProvider provider = Mockito.mock(NetrisProvider.class);
        Mockito.when(netrisProviderService.addProvider(cmd)).thenReturn(provider);
        Mockito.when(netrisProviderService.createNetrisProviderResponse(provider)).thenReturn(null);
        cmd.execute();
    }

    @Test
    public void testGetEntityOwnerId() {
        long expectedAccountId = 123L;
        Mockito.when(callContext.getCallingAccount()).thenReturn(Mockito.mock(com.cloud.user.Account.class));
        Mockito.when(callContext.getCallingAccount().getId()).thenReturn(expectedAccountId);
        long actualAccountId = cmd.getEntityOwnerId();
        Assert.assertEquals(expectedAccountId, actualAccountId);
    }
}
