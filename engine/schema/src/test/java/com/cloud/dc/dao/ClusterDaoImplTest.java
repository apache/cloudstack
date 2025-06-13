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
package com.cloud.dc.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDaoImplTest {
    @Spy
    @InjectMocks
    ClusterDaoImpl clusterDao = new ClusterDaoImpl();

    private GenericSearchBuilder<ClusterVO, Long> genericSearchBuilder;

    @Before
    public void setUp() {
        genericSearchBuilder = mock(SearchBuilder.class);
        ClusterVO entityVO = mock(ClusterVO.class);
        when(genericSearchBuilder.entity()).thenReturn(entityVO);
        doReturn(genericSearchBuilder).when(clusterDao).createSearchBuilder(Long.class);
    }

    @Test
    public void testListAllIds() {
        List<Long> mockIds = Arrays.asList(1L, 2L, 3L);
        doReturn(mockIds).when(clusterDao).customSearch(any(), isNull());
        List<Long> result = clusterDao.listAllIds();
        verify(clusterDao).customSearch(genericSearchBuilder.create(), null);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1L), result.get(0));
        assertEquals(Long.valueOf(2L), result.get(1));
        assertEquals(Long.valueOf(3L), result.get(2));
    }

    @Test
    public void testListAllIdsEmptyResult() {
        doReturn(Collections.emptyList()).when(clusterDao).customSearch(any(), isNull());
        List<Long> result = clusterDao.listAllIds();
        verify(clusterDao).customSearch(genericSearchBuilder.create(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listDistinctHypervisorsArchAcrossClusters_WithZone() {
        Long zoneId = 123L;
        ClusterVO cluster1 = mock(ClusterVO.class);
        when(cluster1.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);
        when(cluster1.getArch()).thenReturn(CPU.CPUArch.amd64);
        ClusterVO cluster2 = mock(ClusterVO.class);
        when(cluster2.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(cluster2.getArch()).thenReturn(CPU.CPUArch.arm64);
        List<ClusterVO> dummyHosts = Arrays.asList(cluster1, cluster2);
        doReturn(dummyHosts).when(clusterDao).search(any(SearchCriteria.class), isNull());
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> result = clusterDao.listDistinctHypervisorsArchAcrossClusters(zoneId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Hypervisor.HypervisorType.XenServer, result.get(0).first());
        assertEquals(CPU.CPUArch.amd64, result.get(0).second());
        assertEquals(Hypervisor.HypervisorType.KVM, result.get(1).first());
        assertEquals(CPU.CPUArch.arm64, result.get(1).second());
    }

    @Test
    public void listDistinctHypervisorsArchAcrossClusters_WithoutZone() {
        Long zoneId = null;
        ClusterVO cluster = mock(ClusterVO.class);
        when(cluster.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        when(cluster.getArch()).thenReturn(CPU.CPUArch.amd64);
        List<ClusterVO> dummyHosts = Collections.singletonList(cluster);
        doReturn(dummyHosts).when(clusterDao).search(any(SearchCriteria.class), isNull());
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> result = clusterDao.listDistinctHypervisorsArchAcrossClusters(zoneId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Hypervisor.HypervisorType.VMware, result.get(0).first());
        assertEquals(CPU.CPUArch.amd64, result.get(0).second());
    }
}
