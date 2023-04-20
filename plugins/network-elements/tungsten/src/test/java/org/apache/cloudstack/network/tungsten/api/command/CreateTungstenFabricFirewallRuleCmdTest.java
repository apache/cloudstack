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
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

public class CreateTungstenFabricFirewallRuleCmdTest {

    @Mock
    TungstenService tungstenService;

    CreateTungstenFabricFirewallRuleCmd createTungstenFabricFirewallRuleCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        createTungstenFabricFirewallRuleCmd = new CreateTungstenFabricFirewallRuleCmd();
        createTungstenFabricFirewallRuleCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "zoneId", 1L);
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "firewallPolicyUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "name", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "action", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "serviceGroupUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "srcTagUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "srcAddressGroupUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "srcNetworkUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "direction", "oneway");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "destTagUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "destAddressGroupUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "destNetworkUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "tagTypeUuid", "test");
        Whitebox.setInternalState(createTungstenFabricFirewallRuleCmd, "sequence", 1);
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricFirewallRuleResponse tungstenFabricFirewallRuleResponse =
                Mockito.mock(TungstenFabricFirewallRuleResponse.class);
        Mockito.when(tungstenService.createTungstenFirewallRule(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt())).thenReturn(tungstenFabricFirewallRuleResponse);
        createTungstenFabricFirewallRuleCmd.execute();
        Assert.assertEquals(tungstenFabricFirewallRuleResponse,
                createTungstenFabricFirewallRuleCmd.getResponseObject());
    }
}
