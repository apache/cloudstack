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
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CreateTungstenFabricFirewallRuleCmdTest {

    @Mock
    TungstenService tungstenService;

    CreateTungstenFabricFirewallRuleCmd createTungstenFabricFirewallRuleCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        createTungstenFabricFirewallRuleCmd = new CreateTungstenFabricFirewallRuleCmd();
        createTungstenFabricFirewallRuleCmd.tungstenService = tungstenService;
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "zoneId", 1L);
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "firewallPolicyUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "name", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "action", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "serviceGroupUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "srcTagUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "srcAddressGroupUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "srcNetworkUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "direction", "oneway");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "destTagUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "destAddressGroupUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "destNetworkUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "tagTypeUuid", "test");
        ReflectionTestUtils.setField(createTungstenFabricFirewallRuleCmd, "sequence", 1);
    }

    public void tearDown() throws Exception {
        closeable.close();
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
