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

import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DeleteTungstenFabricFirewallRuleCmd.class)
public class DeleteTungstenFabricFirewallRuleCmdTest {

    @Mock
    TungstenService tungstenService;

    DeleteTungstenFabricFirewallRuleCmd deleteTungstenFabricFirewallRuleCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        deleteTungstenFabricFirewallRuleCmd = new DeleteTungstenFabricFirewallRuleCmd();
        deleteTungstenFabricFirewallRuleCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(deleteTungstenFabricFirewallRuleCmd, "zoneId", 1L);
        Whitebox.setInternalState(deleteTungstenFabricFirewallRuleCmd, "firewallRuleUuid", "test");
    }

    @Test
    public void executeTest() throws Exception {
        SuccessResponse successResponse = Mockito.mock(SuccessResponse.class);
        Mockito.when(tungstenService.deleteTungstenFirewallRule(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(true);
        PowerMockito.whenNew(SuccessResponse.class).withAnyArguments().thenReturn(successResponse);
        deleteTungstenFabricFirewallRuleCmd.execute();
        Assert.assertEquals(successResponse, deleteTungstenFabricFirewallRuleCmd.getResponseObject());
    }
}
