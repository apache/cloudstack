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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.nsx.NsxProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNsxControllerCmd;
import org.apache.cloudstack.api.response.NsxControllerResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxProviderServiceImplTest {
    @Mock
    NsxProviderDao nsxProviderDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    ResourceManager resourceManager;
    @Mock
    HostDetailsDao hostDetailsDao;

    NsxProviderServiceImpl nsxProviderService;

    @Before
    public void setup() {
        nsxProviderService = new NsxProviderServiceImpl();
        nsxProviderService.resourceManager = resourceManager;
        nsxProviderService.nsxProviderDao = nsxProviderDao;
        nsxProviderService.hostDetailsDao = hostDetailsDao;
        nsxProviderService.dataCenterDao = dataCenterDao;
        nsxProviderService.networkDao = networkDao;
        nsxProviderService.physicalNetworkDao = physicalNetworkDao;
    }

    @Test
    public void testAddProvider() {
        AddNsxControllerCmd cmd = mock(AddNsxControllerCmd.class);
        when(cmd.getZoneId()).thenReturn(1L);
        when(cmd.getName()).thenReturn("NsxController");
        when(cmd.getHostname()).thenReturn("192.168.0.100");
        when(cmd.getPort()).thenReturn("443");
        when(cmd.getUsername()).thenReturn("admin");
        when(cmd.getPassword()).thenReturn("password");
        when(cmd.getEdgeCluster()).thenReturn("EdgeCluster");
        when(cmd.getTier0Gateway()).thenReturn("Tier0-GW01");
        when(cmd.getTransportZone()).thenReturn("Overlay");
        when(resourceManager.addHost(anyLong(), any(ServerResource.class), any(Host.Type.class), anyMap())).thenReturn(mock(Host.class));
        try {
            NsxProvider provider = nsxProviderService.addProvider(cmd);
            Assert.assertNotNull(provider);
        } catch (CloudRuntimeException e) {
            e.printStackTrace();
            fail("Failed to add NSX controller due to internal error.");
        }
    }

    @Test
    public void testCreateNsxControllerResponse() {
        NsxProvider nsxProvider = mock(NsxProvider.class);
        DataCenterVO zone = mock(DataCenterVO.class);
        String uuid = UUID.randomUUID().toString();
        when(dataCenterDao.findById(anyLong())).thenReturn(zone);
        when(zone.getUuid()).thenReturn(UUID.randomUUID().toString());
        when(zone.getName()).thenReturn("ZoneNSX");
        when(nsxProvider.getProviderName()).thenReturn("NSXController");
        when(nsxProvider.getUuid()).thenReturn(uuid);
        when(nsxProvider.getHostname()).thenReturn("hostname");
        when(nsxProvider.getPort()).thenReturn("443");
        when(nsxProvider.getTier0Gateway()).thenReturn("Tier0Gw");
        when(nsxProvider.getEdgeCluster()).thenReturn("EdgeCluster");
        when(nsxProvider.getTransportZone()).thenReturn("Overlay");

        NsxControllerResponse response = nsxProviderService.createNsxControllerResponse(nsxProvider);

        assertEquals("EdgeCluster", response.getEdgeCluster());
        assertEquals("Tier0Gw", response.getTier0Gateway());
        assertEquals("Overlay", response.getTransportZone());
        assertEquals("ZoneNSX", response.getZoneName());
    }

    @Test
    public void testListNsxControllers() {
        NsxProviderVO nsxProviderVO = Mockito.mock(NsxProviderVO.class);

        when(nsxProviderVO.getZoneId()).thenReturn(1L);
        when(dataCenterDao.findById(1L)).thenReturn(mock(DataCenterVO.class));
        when(nsxProviderDao.findByZoneId(anyLong())).thenReturn(nsxProviderVO);

        List<BaseResponse> baseResponseList = nsxProviderService.listNsxProviders(1L);
        assertEquals(1, baseResponseList.size());
    }

    @Test
    public void testDeleteNsxController() {
        NsxProviderVO nsxProviderVO = Mockito.mock(NsxProviderVO.class);
        PhysicalNetworkVO physicalNetworkVO = mock(PhysicalNetworkVO.class);
        List<PhysicalNetworkVO> physicalNetworkVOList = List.of(physicalNetworkVO);
        NetworkVO networkVO = mock(NetworkVO.class);
        List<NetworkVO> networkVOList = List.of(networkVO);

        when(nsxProviderVO.getZoneId()).thenReturn(1L);
        when(physicalNetworkVO.getId()).thenReturn(2L);
        when(physicalNetworkDao.listByZone(1L)).thenReturn(physicalNetworkVOList);
        when(nsxProviderDao.findById(anyLong())).thenReturn(nsxProviderVO);
        when(networkDao.listByPhysicalNetwork(anyLong())).thenReturn(networkVOList);

        assertTrue(nsxProviderService.deleteNsxController(1L));
    }

    @Test
    public void testNetworkStateValidation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        List<NetworkVO> networkVOList = List.of(networkVO);
        when(networkVO.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.NSX);
        when(networkVO.getState()).thenReturn(Network.State.Allocated);

        NsxProviderServiceImpl nsxProviderService = new NsxProviderServiceImpl();

        assertThrows(CloudRuntimeException.class, () -> nsxProviderService.validateNetworkState(networkVOList));
    }
}
