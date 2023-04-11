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
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;

public class RemoveTungstenFabricTagCmdTest {

    @Mock
    TungstenService tungstenService;

    RemoveTungstenFabricTagCmd removeTungstenFabricTagCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        removeTungstenFabricTagCmd = new RemoveTungstenFabricTagCmd();
        removeTungstenFabricTagCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "zoneId", 1L);
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "networkUuids", Arrays.asList("test"));
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "vmUuids", Arrays.asList("test"));
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "nicUuids", Arrays.asList("test"));
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "policyUuid", "test");
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "applicationPolicySetUuid", "test");
        Whitebox.setInternalState(removeTungstenFabricTagCmd, "tagUuid", "test");
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricTagResponse tungstenFabricTagResponse = Mockito.mock(TungstenFabricTagResponse.class);
        Mockito.when(tungstenService.removeTungstenTag(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(tungstenFabricTagResponse);
        removeTungstenFabricTagCmd.execute();
        Assert.assertEquals(tungstenFabricTagResponse, removeTungstenFabricTagCmd.getResponseObject());
    }
}
