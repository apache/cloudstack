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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.TungstenProvider;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

public class CreateTungstenFabricProviderCmdTest {

    @Mock
    TungstenProviderService tungstenProviderService;

    CreateTungstenFabricProviderCmd createTungstenFabricProviderCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        createTungstenFabricProviderCmd = new CreateTungstenFabricProviderCmd();
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "tungstenProviderService", tungstenProviderService);
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "zoneId", 1L);
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "name", "test");
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "hostname", "test");
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "port", "test");
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "gateway", "test");
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "vrouterPort", "test");
        Whitebox.setInternalState(createTungstenFabricProviderCmd, "introspectPort", "test");
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricProviderResponse tungstenFabricProviderResponse =
                Mockito.mock(TungstenFabricProviderResponse.class);
        TungstenProvider tungstenProvider = Mockito.mock(TungstenProvider.class);
        Mockito.when(tungstenProviderService.addProvider(ArgumentMatchers.any())).thenReturn(tungstenProvider);
        Mockito.when(tungstenProviderService.createTungstenProviderResponse(tungstenProvider)).thenReturn(tungstenFabricProviderResponse);
        createTungstenFabricProviderCmd.execute();
        Assert.assertEquals(tungstenFabricProviderResponse, createTungstenFabricProviderCmd.getResponseObject());
    }
}
