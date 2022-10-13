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
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

public class RemoveTungstenFabricPolicyCmdTest {

    @Mock
    TungstenService tungstenService;

    RemoveTungstenFabricPolicyCmd removeTungstenFabricPolicyCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        removeTungstenFabricPolicyCmd = new RemoveTungstenFabricPolicyCmd();
        removeTungstenFabricPolicyCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(removeTungstenFabricPolicyCmd, "zoneId", 1L);
        Whitebox.setInternalState(removeTungstenFabricPolicyCmd, "networkUuid", "test");
        Whitebox.setInternalState(removeTungstenFabricPolicyCmd, "policyUuid", "test");
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricPolicyResponse tungstenFabricPolicyResponse = Mockito.mock(TungstenFabricPolicyResponse.class);
        Mockito.when(tungstenService.removeTungstenPolicy(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(tungstenFabricPolicyResponse);
        removeTungstenFabricPolicyCmd.execute();
        Assert.assertEquals(tungstenFabricPolicyResponse, removeTungstenFabricPolicyCmd.getResponseObject());
    }
}
