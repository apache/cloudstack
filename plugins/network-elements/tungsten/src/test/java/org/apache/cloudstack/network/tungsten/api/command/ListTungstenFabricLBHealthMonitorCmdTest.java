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

import com.cloud.configuration.ConfigurationService;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.ListResponse;
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

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ListTungstenFabricLBHealthMonitorCmd.class)
public class ListTungstenFabricLBHealthMonitorCmdTest {

    @Mock
    TungstenService tungstenService;

    @Mock
    ConfigurationService configService;

    ListTungstenFabricLBHealthMonitorCmd listTungstenFabricLBHealthMonitorCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        listTungstenFabricLBHealthMonitorCmd = new ListTungstenFabricLBHealthMonitorCmd();
        listTungstenFabricLBHealthMonitorCmd.tungstenService = tungstenService;
        listTungstenFabricLBHealthMonitorCmd._configService = configService;
        Mockito.when(configService.getDefaultPageSize()).thenReturn(-1L);
        listTungstenFabricLBHealthMonitorCmd.configure();
        Whitebox.setInternalState(listTungstenFabricLBHealthMonitorCmd, "lbID", 1L);
        Whitebox.setInternalState(listTungstenFabricLBHealthMonitorCmd, "page", 1);
        Whitebox.setInternalState(listTungstenFabricLBHealthMonitorCmd, "pageSize", 10);
    }

    @Test
    public void executeTest() throws Exception {
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        ListResponse<BaseResponse> responseList = Mockito.mock(ListResponse.class);
        Mockito.when(tungstenService.listTungstenFabricLBHealthMonitor(ArgumentMatchers.anyLong())).thenReturn(baseResponseList);
        PowerMockito.whenNew(ListResponse.class).withAnyArguments().thenReturn(responseList);
        listTungstenFabricLBHealthMonitorCmd.execute();
        Assert.assertEquals(responseList, listTungstenFabricLBHealthMonitorCmd.getResponseObject());
    }
}
