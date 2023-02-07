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
@PrepareForTest(SynchronizeTungstenFabricDataCmd.class)
public class SynchronizeTungstenFabricDataCmdTest {

    @Mock
    TungstenService tungstenService;

    SynchronizeTungstenFabricDataCmd synchronizeTungstenFabricDataCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        synchronizeTungstenFabricDataCmd = new SynchronizeTungstenFabricDataCmd();
        synchronizeTungstenFabricDataCmd.tungstenService = tungstenService;
        Whitebox.setInternalState(synchronizeTungstenFabricDataCmd, "tungstenProviderId", 1L);
    }

    @Test
    public void executeTest() throws Exception {
        SuccessResponse successResponse = Mockito.mock(SuccessResponse.class);
        Mockito.when(tungstenService.synchronizeTungstenData(ArgumentMatchers.anyLong())).thenReturn(true);
        PowerMockito.whenNew(SuccessResponse.class).withAnyArguments().thenReturn(successResponse);
        synchronizeTungstenFabricDataCmd.execute();
        Assert.assertEquals(successResponse, synchronizeTungstenFabricDataCmd.getResponseObject());
    }
}
