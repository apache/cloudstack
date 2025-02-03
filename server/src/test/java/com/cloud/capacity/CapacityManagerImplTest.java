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
package com.cloud.capacity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VmDetailConstants;

@RunWith(MockitoJUnitRunner.class)
public class CapacityManagerImplTest {
    @Mock
    ClusterDetailsDao clusterDetailsDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;

    @Spy
    @InjectMocks
    CapacityManagerImpl capacityManager = new CapacityManagerImpl();


    private Host host;
    private ServiceOffering offering;
    private static final long CLUSTER_ID = 1L;
    private static final int OFFERING_CPU = 4;
    private static final int OFFERING_CPU_SPEED = 2000;
    private static final int OFFERING_MEMORY = 4096;

    @Before
    public void setUp() {
        host = mock(Host.class);
        offering = mock(ServiceOffering.class);
        when(host.getClusterId()).thenReturn(CLUSTER_ID);
        when(offering.getCpu()).thenReturn(OFFERING_CPU);
        when(offering.getSpeed()).thenReturn(OFFERING_CPU_SPEED);
        when(offering.getRamSize()).thenReturn(OFFERING_MEMORY);
    }

    @Test
    public void testGetClusterValues() {
        long clusterId = 1L;
        String cpuOvercommit = "2.0";
        String memoryOvercommit = "1.0";
        Map<String, String> clusterDetails = new HashMap<>();
        clusterDetails.put(VmDetailConstants.CPU_OVER_COMMIT_RATIO, cpuOvercommit);
        clusterDetails.put(VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, memoryOvercommit);

        when(clusterDetailsDao.findDetails(eq(clusterId), anyList())).thenReturn(clusterDetails);

        Pair<String, String> result = capacityManager.getClusterValues(clusterId);

        assertEquals(cpuOvercommit, result.first());
        assertEquals(memoryOvercommit, result.second());
        verify(clusterDetailsDao).findDetails(eq(clusterId), anyList());
    }

    @Test
    public void testGetServiceOfferingsMap() {
        Long offering1Id = 1L;
        ServiceOfferingVO offering1 = mock(ServiceOfferingVO.class);
        when(offering1.getId()).thenReturn(offering1Id);
        Long offering2Id = 2L;
        ServiceOfferingVO offering2 = mock(ServiceOfferingVO.class);
        when(offering2.getId()).thenReturn(offering2Id);
        when(serviceOfferingDao.listAllIncludingRemoved()).thenReturn(List.of(offering1, offering2));
        Map<Long, ServiceOfferingVO> result = capacityManager.getServiceOfferingsMap();
        assertEquals(2, result.size());
        assertEquals(offering1, result.get(offering1Id));
        assertEquals(offering2, result.get(offering2Id));
        verify(serviceOfferingDao).listAllIncludingRemoved();
    }

    @Test
    public void testGetServiceOfferingsMapEmptyList() {
        when(serviceOfferingDao.listAllIncludingRemoved()).thenReturn(Collections.emptyList());
        Map<Long, ServiceOfferingVO> result = capacityManager.getServiceOfferingsMap();
        assertTrue(result.isEmpty());
        verify(serviceOfferingDao).listAllIncludingRemoved();
    }

    @Test
    public void testCheckIfHostHasCpuCapabilityAndCapacity() {
        Float cpuOvercommit = 2.0f;
        Float memoryOvercommit = 1.5f;
        Pair<String, String> clusterDetails = new Pair<>(String.valueOf(cpuOvercommit), String.valueOf(memoryOvercommit));
        doReturn(clusterDetails).when(capacityManager).getClusterValues(CLUSTER_ID);
        doReturn(true).when(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU),
                eq(OFFERING_CPU_SPEED));
        doReturn(true).when(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)), eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
        Pair<Boolean, Boolean> result = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, false);
        assertTrue(result.first());
        assertTrue(result.second());
        verify(capacityManager).getClusterValues(CLUSTER_ID);
        verify(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU), eq(OFFERING_CPU_SPEED));
        verify(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)),
                eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
    }

    @Test
    public void testCheckIfHostHasNoCpuCapabilityButHasCapacity() {
        Float cpuOvercommit = 1.5f;
        Float memoryOvercommit = 1.2f;
        Pair<String, String> clusterDetails = new Pair<>(String.valueOf(cpuOvercommit), String.valueOf(memoryOvercommit));
        doReturn(clusterDetails).when(capacityManager).getClusterValues(CLUSTER_ID);
        doReturn(false).when(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU),
                eq(OFFERING_CPU_SPEED));
        doReturn(true).when(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)), eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
        Pair<Boolean, Boolean> result = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, false);
        assertFalse(result.first());
        assertTrue(result.second());
        verify(capacityManager).getClusterValues(CLUSTER_ID);
        verify(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU), eq(OFFERING_CPU_SPEED));
        verify(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)),
                eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
    }

    @Test
    public void testCheckIfHostHasCpuCapabilityButNoCapacity() {
        Float cpuOvercommit = 2.0f;
        Float memoryOvercommit = 1.5f;
        Pair<String, String> clusterDetails = new Pair<>(String.valueOf(cpuOvercommit), String.valueOf(memoryOvercommit));
        doReturn(clusterDetails).when(capacityManager).getClusterValues(CLUSTER_ID);
        doReturn(true).when(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU),
                eq(OFFERING_CPU_SPEED));
        doReturn(false).when(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)), eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
        Pair<Boolean, Boolean> result = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, false);
        assertTrue(result.first());
        assertFalse(result.second());
        verify(capacityManager).getClusterValues(CLUSTER_ID);
        verify(capacityManager).checkIfHostHasCpuCapability(any(Host.class), eq(OFFERING_CPU), eq(OFFERING_CPU_SPEED));
        verify(capacityManager).checkIfHostHasCapacity(eq(host), eq(OFFERING_CPU * OFFERING_CPU_SPEED),
                eq(ByteScaleUtils.mebibytesToBytes(OFFERING_MEMORY)),
                eq(false), eq(cpuOvercommit), eq(memoryOvercommit), eq(false));
    }
}
