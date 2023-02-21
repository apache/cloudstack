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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import java.util.List;

public class AddTungstenFabricNetworkGatewayToLogicalRouterCmdTest {

    @Mock
    TungstenService tungstenService;

    AddTungstenFabricNetworkGatewayToLogicalRouterCmd addTungstenFabricNetworkGatewayToLogicalRouterCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        addTungstenFabricNetworkGatewayToLogicalRouterCmd = new AddTungstenFabricNetworkGatewayToLogicalRouterCmd();
        addTungstenFabricNetworkGatewayToLogicalRouterCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(addTungstenFabricNetworkGatewayToLogicalRouterCmd, "zoneId", 1L);
        Whitebox.setInternalState(addTungstenFabricNetworkGatewayToLogicalRouterCmd, "networkUuid", "005f0dea-0196" +
                "-11ec-a1ed-b42e99f6e187");
        Whitebox.setInternalState(addTungstenFabricNetworkGatewayToLogicalRouterCmd, "logicalRouterUuid", "125f0dea" +
                "-0196-11ec-a1ed-b42e99f6e187");
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        String network = "123";
        List<String> networkList = List.of(network);
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        Mockito.when(tungstenService.listConnectedNetworkFromLogicalRouter(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(networkList);
        Mockito.when(tungstenService.addNetworkGatewayToLogicalRouter(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(baseResponse);
        addTungstenFabricNetworkGatewayToLogicalRouterCmd.execute();
        Assert.assertEquals(baseResponse, addTungstenFabricNetworkGatewayToLogicalRouterCmd.getResponseObject());
    }
}
