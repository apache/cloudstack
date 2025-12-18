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

import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.ListResponse;
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
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ListTungstenFabricVmCmdTest {

    @Mock
    TungstenService tungstenService;

    ListTungstenFabricVmCmd listTungstenFabricVmCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        listTungstenFabricVmCmd = new ListTungstenFabricVmCmd();
        listTungstenFabricVmCmd.tungstenService = tungstenService;
        ReflectionTestUtils.setField(listTungstenFabricVmCmd, "vmUuid", "test");
        ReflectionTestUtils.setField(listTungstenFabricVmCmd, "page", 1);
        ReflectionTestUtils.setField(listTungstenFabricVmCmd, "pageSize", 10);
        ReflectionTestUtils.setField(listTungstenFabricVmCmd, "s_maxPageSize", -1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void executeTest() throws Exception {
        ReflectionTestUtils.setField(listTungstenFabricVmCmd, "zoneId", 1L);
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        Mockito.when(tungstenService.listTungstenVm(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        listTungstenFabricVmCmd.execute();
        ListResponse<BaseResponse> responseList = (ListResponse<BaseResponse>) listTungstenFabricVmCmd.getResponseObject();
        Assert.assertEquals(baseResponseList, responseList.getResponses());
        Assert.assertEquals(Integer.valueOf(1), responseList.getCount());
    }

    @Test
    public void executeAllZoneTest() throws Exception {
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        TungstenProviderVO tungstenProviderVO = Mockito.mock(TungstenProviderVO.class);
        List<TungstenProviderVO> tungstenProviderVOList = Arrays.asList(tungstenProviderVO);
        Mockito.when(tungstenService.getTungstenProviders()).thenReturn(tungstenProviderVOList);
        Mockito.when(tungstenService.listTungstenVm(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(baseResponseList);
        listTungstenFabricVmCmd.execute();
        ListResponse<BaseResponse> responseList = (ListResponse<BaseResponse>) listTungstenFabricVmCmd.getResponseObject();
        Assert.assertEquals(baseResponseList, responseList.getResponses());
        Assert.assertEquals(Integer.valueOf(1), responseList.getCount());
    }
}
