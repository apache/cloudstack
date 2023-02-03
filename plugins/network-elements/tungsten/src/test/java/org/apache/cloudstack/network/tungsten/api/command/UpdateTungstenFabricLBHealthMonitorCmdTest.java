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

import com.cloud.dc.DataCenter;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricLBHealthMonitorResponse;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
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

import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UpdateTungstenFabricLBHealthMonitorCmd.class)
public class UpdateTungstenFabricLBHealthMonitorCmdTest {
    @Mock
    EntityManager entityManager;
    @Mock
    TungstenService tungstenService;

    UpdateTungstenFabricLBHealthMonitorCmd updateTungstenFabricLBHealthMonitorCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        updateTungstenFabricLBHealthMonitorCmd = new UpdateTungstenFabricLBHealthMonitorCmd();
        updateTungstenFabricLBHealthMonitorCmd.tungstenService = tungstenService;
        updateTungstenFabricLBHealthMonitorCmd._entityMgr = entityManager;
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "lbId", 1L);
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "type", "HTTP");
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "retry", 1);
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "timeout", 1);
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "interval", 1);
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "httpMethod", "GET");
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "expectedCode", "test");
        Whitebox.setInternalState(updateTungstenFabricLBHealthMonitorCmd, "urlPath", "test");
    }

    @Test
    public void createTest() throws ResourceAllocationException {
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO =
                Mockito.mock(TungstenFabricLBHealthMonitorVO.class);
        Mockito.when(tungstenService.updateTungstenFabricLBHealthMonitor(ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(tungstenFabricLBHealthMonitorVO);
        updateTungstenFabricLBHealthMonitorCmd.create();
        Assert.assertEquals(Optional.of(tungstenFabricLBHealthMonitorVO.getId()),
                Optional.ofNullable(updateTungstenFabricLBHealthMonitorCmd.getEntityId()));
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getUuid(),
                updateTungstenFabricLBHealthMonitorCmd.getEntityUuid());
    }

    @Test
    public void executeTest() throws Exception {
        updateTungstenFabricLBHealthMonitorCmd.setEntityId(1L);
        TungstenFabricLBHealthMonitorResponse tungstenFabricLBHealthMonitorResponse =
                Mockito.mock(TungstenFabricLBHealthMonitorResponse.class);
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO =
                Mockito.mock(TungstenFabricLBHealthMonitorVO.class);
        tungstenFabricLBHealthMonitorVO.setType("test");
        Whitebox.setInternalState(tungstenFabricLBHealthMonitorVO, "id", 1L);
        Whitebox.setInternalState(tungstenFabricLBHealthMonitorVO, "uuid", "test");
        tungstenFabricLBHealthMonitorVO.setRetry(1);
        tungstenFabricLBHealthMonitorVO.setTimeout(1);
        tungstenFabricLBHealthMonitorVO.setInterval(1);
        tungstenFabricLBHealthMonitorVO.setHttpMethod("test");
        tungstenFabricLBHealthMonitorVO.setExpectedCode("test");
        tungstenFabricLBHealthMonitorVO.setUrlPath("test");
        LoadBalancer loadBalancer = Mockito.mock(LoadBalancer.class);
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);

        Mockito.when(entityManager.findById(ArgumentMatchers.eq(TungstenFabricLBHealthMonitorVO.class),
                ArgumentMatchers.anyLong())).thenReturn(tungstenFabricLBHealthMonitorVO);
        Mockito.when(entityManager.findById(ArgumentMatchers.eq(LoadBalancer.class), ArgumentMatchers.anyLong())).thenReturn(loadBalancer);
        Mockito.when(entityManager.findById(ArgumentMatchers.eq(Network.class), ArgumentMatchers.anyLong())).thenReturn(network);
        Mockito.when(entityManager.findById(ArgumentMatchers.eq(DataCenter.class), ArgumentMatchers.anyLong())).thenReturn(dataCenter);
        Mockito.when(tungstenService.applyLBHealthMonitor(ArgumentMatchers.anyLong())).thenReturn(true);
        PowerMockito.whenNew(TungstenFabricLBHealthMonitorResponse.class).withAnyArguments().thenReturn(tungstenFabricLBHealthMonitorResponse);
        updateTungstenFabricLBHealthMonitorCmd.execute();
        Assert.assertEquals(tungstenFabricLBHealthMonitorResponse,
                updateTungstenFabricLBHealthMonitorCmd.getResponseObject());
    }
}
