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

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class UpdateTungstenFabricLBHealthMonitorCmdTest {
    @Mock
    EntityManager entityManager;
    @Mock
    TungstenService tungstenService;

    UpdateTungstenFabricLBHealthMonitorCmd updateTungstenFabricLBHealthMonitorCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        updateTungstenFabricLBHealthMonitorCmd = new UpdateTungstenFabricLBHealthMonitorCmd();
        updateTungstenFabricLBHealthMonitorCmd.tungstenService = tungstenService;
        updateTungstenFabricLBHealthMonitorCmd._entityMgr = entityManager;
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "lbId", 1L);
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "type", "HTTP");
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "retry", 1);
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "timeout", 1);
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "interval", 1);
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "httpMethod", "GET");
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "expectedCode", "test");
        ReflectionTestUtils.setField(updateTungstenFabricLBHealthMonitorCmd, "urlPath", "test");
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
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
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO =
                Mockito.mock(TungstenFabricLBHealthMonitorVO.class);
        tungstenFabricLBHealthMonitorVO.setType("test");
        ReflectionTestUtils.setField(tungstenFabricLBHealthMonitorVO, "id", 1L);
        ReflectionTestUtils.setField(tungstenFabricLBHealthMonitorVO, "uuid", "test");
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
        updateTungstenFabricLBHealthMonitorCmd.execute();
        TungstenFabricLBHealthMonitorResponse tungstenFabricLBHealthMonitorResponse =
                (TungstenFabricLBHealthMonitorResponse) updateTungstenFabricLBHealthMonitorCmd.getResponseObject();

        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getType(), tungstenFabricLBHealthMonitorResponse.getType());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getId(), tungstenFabricLBHealthMonitorResponse.getId());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getUuid(), tungstenFabricLBHealthMonitorResponse.getUuid());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getRetry(), tungstenFabricLBHealthMonitorResponse.getRetry());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getTimeout(), tungstenFabricLBHealthMonitorResponse.getTimeout());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getInterval(), tungstenFabricLBHealthMonitorResponse.getInterval());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getHttpMethod(), tungstenFabricLBHealthMonitorResponse.getHttpMethod());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getExpectedCode(), tungstenFabricLBHealthMonitorResponse.getExpectedCode());
        Assert.assertEquals(tungstenFabricLBHealthMonitorVO.getUrlPath(), tungstenFabricLBHealthMonitorResponse.getUrlPath());

        Assert.assertEquals(dataCenter.getId(), tungstenFabricLBHealthMonitorResponse.getZoneId());
        Assert.assertEquals(dataCenter.getName(), tungstenFabricLBHealthMonitorResponse.getZoneName());

    }
}
