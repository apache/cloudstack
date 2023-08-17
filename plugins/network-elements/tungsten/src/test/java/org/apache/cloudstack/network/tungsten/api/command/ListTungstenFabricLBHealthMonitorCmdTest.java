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
public class ListTungstenFabricLBHealthMonitorCmdTest {

    @Mock
    TungstenService tungstenService;

    ListTungstenFabricLBHealthMonitorCmd listTungstenFabricLBHealthMonitorCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        listTungstenFabricLBHealthMonitorCmd = new ListTungstenFabricLBHealthMonitorCmd();
        listTungstenFabricLBHealthMonitorCmd.tungstenService = tungstenService;
        ReflectionTestUtils.setField(listTungstenFabricLBHealthMonitorCmd, "lbID", 1L);
        ReflectionTestUtils.setField(listTungstenFabricLBHealthMonitorCmd, "page", 1);
        ReflectionTestUtils.setField(listTungstenFabricLBHealthMonitorCmd, "pageSize", 10);
        ReflectionTestUtils.setField(listTungstenFabricLBHealthMonitorCmd, "s_maxPageSize", -1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void executeTest() throws Exception {
        BaseResponse baseResponse = Mockito.mock(BaseResponse.class);
        List<BaseResponse> baseResponseList = Arrays.asList(baseResponse);
        Mockito.when(tungstenService.listTungstenFabricLBHealthMonitor(ArgumentMatchers.anyLong())).thenReturn(baseResponseList);
        listTungstenFabricLBHealthMonitorCmd.execute();
        ListResponse<BaseResponse> responseList = (ListResponse<BaseResponse>) listTungstenFabricLBHealthMonitorCmd.getResponseObject();
        Assert.assertEquals(baseResponseList, responseList.getResponses());
        Assert.assertEquals(Integer.valueOf(1), responseList.getCount());
    }
}
