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
import com.cloud.network.element.TungstenProviderVO;
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
@PrepareForTest(ListTungstenFabricNicCmd.class)
public class ListTungstenFabricNicCmdTest {

    @Mock
    TungstenService tungstenService;

    @Mock
    ConfigurationService configService;

    ListTungstenFabricNicCmd listTungstenFabricNicCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        listTungstenFabricNicCmd = new ListTungstenFabricNicCmd();
        listTungstenFabricNicCmd.tungstenService = tungstenService;
        listTungstenFabricNicCmd._configService = configService;
        Mockito.when(configService.getDefaultPageSize()).thenReturn(-1L);
        listTungstenFabricNicCmd.configure();
        Whitebox.setInternalState(listTungstenFabricNicCmd, "nicUuid", "test");
        Whitebox.setInternalState(listTungstenFabricNicCmd, "page", 1);
        Whitebox.setInternalState(listTungstenFabricNicCmd, "pageSize", 10);
    }

    @Test
    public void executeTest() throws Exception {
        Whitebox.setInternalState(listTungstenFabricNicCmd, "zoneId", 1L);
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        ListResponse<BaseResponse> responseList = Mockito.mock(ListResponse.class);
        Mockito.when(tungstenService.listTungstenNic(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        PowerMockito.whenNew(ListResponse.class).withAnyArguments().thenReturn(responseList);
        listTungstenFabricNicCmd.execute();
        Assert.assertEquals(responseList, listTungstenFabricNicCmd.getResponseObject());
    }

    @Test
    public void executeAllZoneTest() throws Exception {
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        ListResponse<BaseResponse> responseList = Mockito.mock(ListResponse.class);
        TungstenProviderVO tungstenProviderVO = Mockito.mock(TungstenProviderVO.class);
        List<TungstenProviderVO> tungstenProviderVOList = Arrays.asList(tungstenProviderVO);
        Mockito.when(tungstenService.getTungstenProviders()).thenReturn(tungstenProviderVOList);
        Mockito.when(tungstenService.listTungstenNic(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        PowerMockito.whenNew(ListResponse.class).withAnyArguments().thenReturn(responseList);
        listTungstenFabricNicCmd.execute();
        Assert.assertEquals(responseList, listTungstenFabricNicCmd.getResponseObject());
    }
}
