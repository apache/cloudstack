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
package org.apache.cloudstack.network.tungsten.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricProviderCmd;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TungstenProviderServiceTest {

    @Mock
    DataCenterDao dcDao;
    @Mock
    DataCenterVO zone;
    @Mock
    Host host;
    @Mock
    ResourceManager resourceMgr;
    @Mock
    DomainDao domainDao;
    @Mock
    ProjectDao projectDao;
    @Mock
    TungstenProviderDao tungstenProviderDao;
    @Mock
    HostDetailsDao hostDetailsDao;
    @Mock
    MessageBus messageBus;

    TungstenProviderServiceImpl tungstenProviderService;

    AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        tungstenProviderService = new TungstenProviderServiceImpl();
        tungstenProviderService.zoneDao = dcDao;
        tungstenProviderService.resourceMgr = resourceMgr;
        tungstenProviderService.domainDao = domainDao;
        tungstenProviderService.projectDao = projectDao;
        tungstenProviderService.tungstenProviderDao = tungstenProviderDao;
        tungstenProviderService.hostDetailsDao = hostDetailsDao;
        tungstenProviderService.messageBus = messageBus;

        when(dcDao.findById(anyLong())).thenReturn(zone);
        when(zone.getName()).thenReturn("ZoneName");
        when(resourceMgr.addHost(anyLong(), any(), any(), anyMap())).thenReturn(host);
        when(host.getId()).thenReturn(1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void addTungstenProviderTest() {
        CreateTungstenFabricProviderCmd cmd = Mockito.mock(CreateTungstenFabricProviderCmd.class);
        when(cmd.getZoneId()).thenReturn(1L);
        when(cmd.getName()).thenReturn("TungstenProviderName");
        when(cmd.getHostname()).thenReturn("192.168.0.100");
        when(cmd.getPort()).thenReturn("8082");
        when(cmd.getGateway()).thenReturn("192.168.0.101");
        when(cmd.getVrouterPort()).thenReturn("9091");
        when(cmd.getIntrospectPort()).thenReturn("8085");

        try {
            TungstenProvider tungstenProvider = tungstenProviderService.addProvider(cmd);
            assertNotNull(tungstenProvider);
        } catch (CloudRuntimeException e) {
            e.printStackTrace();
            fail("Failed to add Tungsten-Fabric provider due to internal error.");
        }
    }

    @Test
    public void listTungstenProviderWithZoneIdTest() {
        TungstenProviderVO tungstenProviderVO = Mockito.mock(TungstenProviderVO.class);

        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);

        List<BaseResponse> baseResponseList = tungstenProviderService.listTungstenProvider(1L);
        assertEquals(1, baseResponseList.size());
    }

    @Test
    public void listTungstenProviderWithoutZoneIdTest() {
        TungstenProviderVO tungstenProviderVO1 = Mockito.mock(TungstenProviderVO.class);
        TungstenProviderVO tungstenProviderVO2 = Mockito.mock(TungstenProviderVO.class);

        when(tungstenProviderDao.listAll()).thenReturn(Arrays.asList(tungstenProviderVO1, tungstenProviderVO2));

        List<BaseResponse> baseResponseList = tungstenProviderService.listTungstenProvider(null);
        assertEquals(2, baseResponseList.size());
    }
}
