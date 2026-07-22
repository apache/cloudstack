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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.cache.LazyCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.event.UsageEventVO;
import com.cloud.resource.ResourceState;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class CapacityManagerImplTest {
    @Mock
    ClusterDetailsDao clusterDetailsDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;
    @Mock
    CapacityDao capacityDao;
    @Mock
    HostDao hostDao;
    @Mock
    VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    CapacityManagerImpl capacityManager = new CapacityManagerImpl();


    private Host host;
    private ServiceOffering offering;
    private static final long CLUSTER_ID = 1L;
    private static final long HOST_ID = 100L;
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

    @Test
    public void testUpdateCapacityForHostMixedStaticAndDynamic() throws Exception {
        HostVO testHost = mock(HostVO.class);
        when(testHost.getId()).thenReturn(HOST_ID);
        when(testHost.getClusterId()).thenReturn(CLUSTER_ID);
        when(testHost.getResourceState()).thenReturn(ResourceState.Enabled);
        when(testHost.getCpus()).thenReturn(16);
        when(testHost.getSpeed()).thenReturn(2000L);
        when(testHost.getTotalMemory()).thenReturn(32L * 1024 * 1024 * 1024);

        Pair<String, String> clusterOvercommit = new Pair<>("2.0", "1.5");
        doReturn(clusterOvercommit).when(capacityManager).getClusterValues(CLUSTER_ID);
        java.lang.reflect.Field cacheField = CapacityManagerImpl.class.getDeclaredField("clusterValuesCache");
        cacheField.setAccessible(true);
        cacheField.set(capacityManager, new LazyCache<>(128, 60, capacityManager::getClusterValues));

        long staticVmId = 1L;
        long dynamicVmId = 2L;
        long staticOfferingId = 10L;
        long dynamicOfferingId = 20L;

        VMInstanceVO staticVm = mock(VMInstanceVO.class);
        when(staticVm.getId()).thenReturn(staticVmId);
        when(staticVm.getServiceOfferingId()).thenReturn(staticOfferingId);

        VMInstanceVO dynamicVm = mock(VMInstanceVO.class);
        when(dynamicVm.getId()).thenReturn(dynamicVmId);
        when(dynamicVm.getServiceOfferingId()).thenReturn(dynamicOfferingId);

        when(vmInstanceDao.listIdServiceOfferingForUpVmsByHostId(HOST_ID))
                .thenReturn(new ArrayList<>(List.of(staticVm, dynamicVm)));
        when(vmInstanceDao.listIdServiceOfferingForVmsMigratingFromHost(HOST_ID))
                .thenReturn(Collections.emptyList());
        when(vmInstanceDao.listByLastHostId(HOST_ID))
                .thenReturn(Collections.emptyList());

        ServiceOfferingVO staticOffering = mock(ServiceOfferingVO.class);
        when(staticOffering.isDynamic()).thenReturn(false);
        when(staticOffering.getCpu()).thenReturn(2);
        when(staticOffering.getSpeed()).thenReturn(2000);
        when(staticOffering.getRamSize()).thenReturn(2048);

        ServiceOfferingVO dynamicOffering = mock(ServiceOfferingVO.class);
        when(dynamicOffering.isDynamic()).thenReturn(true);

        doReturn(staticOffering).when(capacityManager).getServiceOffering(staticOfferingId);
        doReturn(dynamicOffering).when(capacityManager).getServiceOffering(dynamicOfferingId);

        Map<Long, Map<String, String>> batchDetails = new HashMap<>();
        batchDetails.put(staticVmId, Map.of(
                VmDetailConstants.CPU_OVER_COMMIT_RATIO, "2.0",
                VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, "1.5"));
        batchDetails.put(dynamicVmId, Map.of(
                VmDetailConstants.CPU_OVER_COMMIT_RATIO, "2.0",
                VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, "1.5",
                UsageEventVO.DynamicParameters.cpuNumber.name(), "4",
                UsageEventVO.DynamicParameters.cpuSpeed.name(), "2500",
                UsageEventVO.DynamicParameters.memory.name(), "4096"));
        doReturn(batchDetails).when(capacityManager).batchGetVmDetailsForCapacityCalculation(anyList());

        CapacityVO cpuCapVO = new CapacityVO(HOST_ID, 1L, 1L, CLUSTER_ID, 0, 16 * 2000L, Capacity.CAPACITY_TYPE_CPU);
        cpuCapVO.setReservedCapacity(0);
        CapacityVO memCapVO = new CapacityVO(HOST_ID, 1L, 1L, CLUSTER_ID, 0, 32L * 1024 * 1024 * 1024, Capacity.CAPACITY_TYPE_MEMORY);
        memCapVO.setReservedCapacity(0);
        CapacityVO cpuCoreCapVO = new CapacityVO(HOST_ID, 1L, 1L, CLUSTER_ID, 0, 16L, CapacityVO.CAPACITY_TYPE_CPU_CORE);
        cpuCoreCapVO.setReservedCapacity(0);

        when(capacityDao.listByHostIdTypes(eq(HOST_ID), anyList()))
                .thenReturn(List.of(cpuCapVO, memCapVO, cpuCoreCapVO));
        when(capacityDao.update(anyLong(), any(CapacityVO.class))).thenReturn(true);

        capacityManager.updateCapacityForHost(testHost);

        // static VM: cpu = (2 * 2000 / 2.0) * 2.0 = 4000, mem = (2048 * 1024 * 1024 / 1.5) * 1.5 = 2048MB
        // dynamic VM: cpu = (4 * 2500 / 2.0) * 2.0 = 10000, mem = (4096 * 1024 * 1024 / 1.5) * 1.5 = 4096MB
        long expectedUsedCpu = 4000 + 10000;
        long expectedUsedMem = (2048L + 4096L) * 1024 * 1024;
        long expectedUsedCpuCore = 2 + 4;

        assertEquals(expectedUsedCpu, cpuCapVO.getUsedCapacity());
        assertEquals(expectedUsedMem, memCapVO.getUsedCapacity());
        assertEquals(expectedUsedCpuCore, cpuCoreCapVO.getUsedCapacity());
        assertEquals(0, cpuCapVO.getReservedCapacity());
        assertEquals(0, memCapVO.getReservedCapacity());

        verify(capacityManager).batchGetVmDetailsForCapacityCalculation(anyList());
        verify(capacityManager, never()).getVmDetailsForCapacityCalculation(anyLong());
    }
}
