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
import org.junit.After;
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

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class ApplyTungstenFabricTagCmdTest {

    @Mock
    TungstenService tungstenService;

    ApplyTungstenFabricTagCmd applyTungstenFabricTagCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        applyTungstenFabricTagCmd = new ApplyTungstenFabricTagCmd();
        applyTungstenFabricTagCmd.tungstenService = tungstenService;
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "zoneId", 1L);
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "networkUuids", Arrays.asList("test"));
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "vmUuids", Arrays.asList("test"));
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "nicUuids", Arrays.asList("test"));
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "policyUuid", "test");
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "applicationPolicySetUuid", "test");
        ReflectionTestUtils.setField(applyTungstenFabricTagCmd, "tagUuid", "test");
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void executeTest() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricTagResponse tungstenFabricTagResponse = Mockito.mock(TungstenFabricTagResponse.class);
        Mockito.when(tungstenService.applyTungstenTag(ArgumentMatchers.anyLong(), ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList(), ArgumentMatchers.anyList(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(tungstenFabricTagResponse);
        applyTungstenFabricTagCmd.execute();
        Assert.assertEquals(tungstenFabricTagResponse, applyTungstenFabricTagCmd.getResponseObject());
    }
}
