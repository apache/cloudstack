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
@PrepareForTest(ListTungstenFabricTagTypeCmd.class)
public class ListTungstenFabricTagTypeCmdTest {

    @Mock
    TungstenService tungstenService;

    @Mock
    ConfigurationService configService;

    ListTungstenFabricTagTypeCmd listTungstenFabricTagTypeCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        listTungstenFabricTagTypeCmd = new ListTungstenFabricTagTypeCmd();
        listTungstenFabricTagTypeCmd.tungstenService = tungstenService;
        listTungstenFabricTagTypeCmd._configService = configService;
        Mockito.when(configService.getDefaultPageSize()).thenReturn(-1L);
        listTungstenFabricTagTypeCmd.configure();
        Whitebox.setInternalState(listTungstenFabricTagTypeCmd, "tagTypeUuid", "test");
        Whitebox.setInternalState(listTungstenFabricTagTypeCmd, "page", 1);
        Whitebox.setInternalState(listTungstenFabricTagTypeCmd, "pageSize", 10);
    }

    @Test
    public void executeTest() throws Exception {
        Whitebox.setInternalState(listTungstenFabricTagTypeCmd, "zoneId", 1L);
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        ListResponse<BaseResponse> responseList = Mockito.mock(ListResponse.class);
        Mockito.when(tungstenService.listTungstenTagTypes(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        PowerMockito.whenNew(ListResponse.class).withAnyArguments().thenReturn(responseList);
        listTungstenFabricTagTypeCmd.execute();
        Assert.assertEquals(responseList, listTungstenFabricTagTypeCmd.getResponseObject());
    }

    @Test
    public void executeAllZoneTest() throws Exception {
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        ListResponse<BaseResponse> responseList = Mockito.mock(ListResponse.class);
        TungstenProviderVO tungstenProviderVO = Mockito.mock(TungstenProviderVO.class);
        List<TungstenProviderVO> tungstenProviderVOList = Arrays.asList(tungstenProviderVO);
        Mockito.when(tungstenService.getTungstenProviders()).thenReturn(tungstenProviderVOList);
        Mockito.when(tungstenService.listTungstenTagTypes(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        PowerMockito.whenNew(ListResponse.class).withAnyArguments().thenReturn(responseList);
        listTungstenFabricTagTypeCmd.execute();
        Assert.assertEquals(responseList, listTungstenFabricTagTypeCmd.getResponseObject());
    }
}
