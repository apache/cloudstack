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
package com.cloud.host.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class HostDaoImplTest {

    @Spy
    HostDaoImpl hostDao = new HostDaoImpl();

    @Mock
    private SearchBuilder<HostVO> mockSearchBuilder;
    @Mock
    private SearchCriteria<HostVO> mockSearchCriteria;

    @Test
    public void testCountUpAndEnabledHostsInZone() {
        long testZoneId = 100L;
        hostDao.HostTypeCountSearch = mockSearchBuilder;
        Mockito.when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        Mockito.doNothing().when(mockSearchCriteria).setParameters(Mockito.anyString(), Mockito.any());
        int expected = 5;
        Mockito.doReturn(expected).when(hostDao).getCount(mockSearchCriteria);
        Integer count = hostDao.countUpAndEnabledHostsInZone(testZoneId);
        Assert.assertSame(expected, count);
        Mockito.verify(mockSearchCriteria).setParameters("type", Host.Type.Routing);
        Mockito.verify(mockSearchCriteria).setParameters("resourceState", ResourceState.Enabled);
        Mockito.verify(mockSearchCriteria).setParameters("zoneId", testZoneId);
        Mockito.verify(hostDao).getCount(mockSearchCriteria);
    }

    @Test
    public void testCountAllHostsAndCPUSocketsByType() {
        Host.Type type = Host.Type.Routing;
        GenericDaoBase.SumCount mockSumCount = new GenericDaoBase.SumCount();
        mockSumCount.count = 10;
        mockSumCount.sum = 20;
        HostVO host = Mockito.mock(HostVO.class);
        GenericSearchBuilder<HostVO, GenericDaoBase.SumCount> sb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(host);
        Mockito.doReturn(sb).when(hostDao).createSearchBuilder(GenericDaoBase.SumCount.class);
        SearchCriteria<GenericDaoBase.SumCount> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(List.of(mockSumCount)).when(hostDao).customSearch(Mockito.any(SearchCriteria.class), Mockito.any());
        Pair<Integer, Integer> result = hostDao.countAllHostsAndCPUSocketsByType(type);
        Assert.assertEquals(10, result.first().intValue());
        Assert.assertEquals(20, result.second().intValue());
        Mockito.verify(sc).setParameters("type", type);
    }

    @Test
    public void testIsHostUp() {
        long testHostId = 101L;
        List<Status> statuses = List.of(Status.Up);
        HostVO host = Mockito.mock(HostVO.class);
        GenericSearchBuilder<HostVO, Status> sb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(host);
        SearchCriteria<Status> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(sb).when(hostDao).createSearchBuilder(Status.class);
        Mockito.doReturn(statuses).when(hostDao).customSearch(Mockito.any(SearchCriteria.class), Mockito.any());
        boolean result = hostDao.isHostUp(testHostId);
        Assert.assertTrue("Host should be up", result);
        Mockito.verify(sc).setParameters("id", testHostId);
        Mockito.verify(hostDao).customSearch(sc, null);
    }

    @Test
    public void testFindHostIdsByZoneClusterResourceStateTypeAndHypervisorType() {
        Long zoneId = 1L;
        Long clusterId = 2L;
        Long msId = 1L;
        List<ResourceState> resourceStates = List.of(ResourceState.Enabled);
        List<Host.Type> types = List.of(Host.Type.Routing);
        List<Hypervisor.HypervisorType> hypervisorTypes = List.of(Hypervisor.HypervisorType.KVM);
        List<Long> mockResults = List.of(1001L, 1002L); // Mocked result
        HostVO host = Mockito.mock(HostVO.class);
        GenericSearchBuilder<HostVO, Long> sb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(host);
        SearchCriteria<Long> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.when(sb.and()).thenReturn(sb);
        Mockito.doReturn(sb).when(hostDao).createSearchBuilder(Long.class);
        Mockito.doReturn(mockResults).when(hostDao).customSearch(Mockito.any(SearchCriteria.class), Mockito.any());
        List<Long> hostIds = hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(
                zoneId, clusterId, msId, resourceStates, types, hypervisorTypes);
        Assert.assertEquals(mockResults, hostIds);
        Mockito.verify(sc).setParameters("zoneId", zoneId);
        Mockito.verify(sc).setParameters("clusterId", clusterId);
        Mockito.verify(sc).setParameters("msId", msId);
        Mockito.verify(sc).setParameters("resourceState", resourceStates.toArray());
        Mockito.verify(sc).setParameters("type", types.toArray());
        Mockito.verify(sc).setParameters("hypervisorTypes", hypervisorTypes.toArray());
    }

    @Test
    public void testListDistinctHypervisorTypes() {
        Long zoneId = 1L;
        List<Hypervisor.HypervisorType> mockResults = List.of(Hypervisor.HypervisorType.KVM, Hypervisor.HypervisorType.XenServer);
        HostVO host = Mockito.mock(HostVO.class);
        GenericSearchBuilder<HostVO, Hypervisor.HypervisorType> sb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(host);
        SearchCriteria<Hypervisor.HypervisorType> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(sb).when(hostDao).createSearchBuilder(Hypervisor.HypervisorType.class);
        Mockito.doReturn(mockResults).when(hostDao).customSearch(Mockito.any(SearchCriteria.class), Mockito.any());
        List<Hypervisor.HypervisorType> hypervisorTypes = hostDao.listDistinctHypervisorTypes(zoneId);
        Assert.assertEquals(mockResults, hypervisorTypes);
        Mockito.verify(sc).setParameters("zoneId", zoneId);
        Mockito.verify(sc).setParameters("type", Host.Type.Routing);
    }

    @Test
    public void testListByIds() {
        List<Long> ids = List.of(101L, 102L);
        List<HostVO> mockResults = List.of(Mockito.mock(HostVO.class), Mockito.mock(HostVO.class));
        hostDao.IdsSearch = mockSearchBuilder;
        Mockito.when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        Mockito.doReturn(mockResults).when(hostDao).search(Mockito.any(SearchCriteria.class), Mockito.any());
        List<HostVO> hosts = hostDao.listByIds(ids);
        Assert.assertEquals(mockResults, hosts);
        Mockito.verify(mockSearchCriteria).setParameters("id", ids.toArray());
        Mockito.verify(hostDao).search(mockSearchCriteria, null);
    }

    @Test
    public void testListIdsBy() {
        Host.Type type = Host.Type.Routing;
        Status status = Status.Up;
        ResourceState resourceState = ResourceState.Enabled;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        Long zoneId = 1L, podId = 2L, clusterId = 3L;
        List<Long> mockResults = List.of(1001L, 1002L);
        HostVO host = Mockito.mock(HostVO.class);
        GenericSearchBuilder<HostVO, Long> sb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(host);
        SearchCriteria<Long> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(sb).when(hostDao).createSearchBuilder(Long.class);
        Mockito.doReturn(mockResults).when(hostDao).customSearch(Mockito.any(SearchCriteria.class), Mockito.any());
        List<Long> hostIds = hostDao.listIdsBy(type, status, resourceState, hypervisorType, zoneId, podId, clusterId);
        Assert.assertEquals(mockResults, hostIds);
        Mockito.verify(sc).setParameters("type", type);
        Mockito.verify(sc).setParameters("status", status);
        Mockito.verify(sc).setParameters("resourceState", resourceState);
        Mockito.verify(sc).setParameters("hypervisorType", hypervisorType);
        Mockito.verify(sc).setParameters("zoneId", zoneId);
        Mockito.verify(sc).setParameters("podId", podId);
        Mockito.verify(sc).setParameters("clusterId", clusterId);
    }
}
