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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.service.NetrisProviderService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class ListNetrisProvidersCmdTest {

    @Mock
    private NetrisProviderService netrisProviderService;

    @InjectMocks
    private ListNetrisProvidersCmd cmd;

    private AutoCloseable closeable;

    private static final long ZONE_ID = 1L;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        setPrivateField("zoneId", ZONE_ID);
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = ListNetrisProvidersCmd.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(cmd, value);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testExecuteSuccess() throws ConcurrentOperationException {
        // Setup
        NetrisProviderResponse providerResponse = Mockito.mock(NetrisProviderResponse.class);
        List<BaseResponse> providerList = Arrays.asList(providerResponse);
        Mockito.when(netrisProviderService.listNetrisProviders(ZONE_ID)).thenReturn(providerList);

        // Execute
        cmd.execute();

        // Verify
        Mockito.verify(netrisProviderService).listNetrisProviders(ZONE_ID);
        Assert.assertTrue(cmd.getResponseObject() instanceof ListResponse);
        ListResponse<BaseResponse> response = (ListResponse<BaseResponse>) cmd.getResponseObject();
        Assert.assertEquals(cmd.getCommandName(), response.getResponseName());
    }

    @Test
    public void testExecuteWithoutZoneId() throws ConcurrentOperationException, Exception {
        // Setup
        setPrivateField("zoneId", null);
        NetrisProviderResponse providerResponse = Mockito.mock(NetrisProviderResponse.class);
        List<BaseResponse> providerList = Arrays.asList(providerResponse);
        Mockito.when(netrisProviderService.listNetrisProviders(null)).thenReturn(providerList);

        // Execute
        cmd.execute();

        // Verify
        Mockito.verify(netrisProviderService).listNetrisProviders(null);
        Assert.assertTrue(cmd.getResponseObject() instanceof ListResponse);
        ListResponse<BaseResponse> response = (ListResponse<BaseResponse>) cmd.getResponseObject();
        Assert.assertEquals(cmd.getCommandName(), response.getResponseName());
    }

    @Test
    public void testGetEntityOwnerId() {
        Assert.assertEquals(0L, cmd.getEntityOwnerId());
    }
}
