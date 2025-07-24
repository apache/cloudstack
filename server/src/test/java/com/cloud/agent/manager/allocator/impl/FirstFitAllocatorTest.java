/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.agent.manager.allocator.impl;

import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FirstFitAllocatorTest {
    private static final double TOLERANCE = 0.0001;
    private FirstFitAllocator allocator;
    private CapacityManager capacityMgr;
    private ServiceOfferingDetailsDao offeringDetailsDao;
    private ResourceManager resourceMgr;

    private DeploymentPlan plan;
    private ServiceOffering offering;
    private DeploymentPlanner.ExcludeList avoid;
    private Account account;

    private Host host1;
    private Host host2;
    ConfigurationDao configDao;

    @Before
    public void setUp() {
        allocator = new FirstFitAllocator();
        capacityMgr = mock(CapacityManager.class);
        offeringDetailsDao = mock(ServiceOfferingDetailsDao.class);
        resourceMgr = mock(ResourceManager.class);
        configDao = mock(ConfigurationDao.class);

        allocator._capacityMgr = capacityMgr;
        allocator._serviceOfferingDetailsDao = offeringDetailsDao;
        allocator._resourceMgr = resourceMgr;
        allocator._configDao = configDao;

        plan = mock(DeploymentPlan.class);
        offering = mock(ServiceOffering.class);
        avoid = mock(DeploymentPlanner.ExcludeList.class);
        account = mock(Account.class);

        host1 = mock(Host.class);
        host2 = mock(Host.class);

        when(plan.getDataCenterId()).thenReturn(1L);
        when(offering.getCpu()).thenReturn(2);
        when(offering.getSpeed()).thenReturn(1000);
        when(offering.getRamSize()).thenReturn(2048);
        when(offering.getId()).thenReturn(123L);
        when(offering.getHostTag()).thenReturn(null);
    }

    @Test
    public void testConfigure() throws Exception {
        when(configDao.getConfiguration(anyMap())).thenReturn(new HashMap<>());
        assertTrue(allocator._checkHvm);
        assertTrue(allocator.configure("test", new HashMap<>()));
    }

    @Test
    public void testAllocateTo_SuccessfulMatch() {
        List<Host> inputHosts = Arrays.asList(host1, host2);

        // All hosts are allowed
        when(avoid.shouldAvoid(host1)).thenReturn(false);
        when(avoid.shouldAvoid(host2)).thenReturn(false);

        // No GPU requirement
        when(offeringDetailsDao.findDetail(eq(123L), anyString())).thenReturn(null);

        // CPU capability and capacity is met
        when(capacityMgr.checkIfHostReachMaxGuestLimit(any())).thenReturn(false);
        when(capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(eq(host1), eq(offering), eq(true)))
                .thenReturn(new Pair<>(true, true));
        when(capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(eq(host2), eq(offering), eq(true)))
                .thenReturn(new Pair<>(true, false));

        List<Host> result = allocator.allocateTo(plan, offering, null, avoid, inputHosts, 2, true, account);

        // Only host1 should be returned
        assertEquals(1, result.size());
        assertTrue(result.contains(host1));
        assertFalse(result.contains(host2));
    }

    @Test
    public void testAllocateTo_AvoidSetAndGuestLimit() {
        List<Host> inputHosts = Arrays.asList(host1, host2);

        when(avoid.shouldAvoid(host1)).thenReturn(true); // Avoided
        when(avoid.shouldAvoid(host2)).thenReturn(false);

        when(capacityMgr.checkIfHostReachMaxGuestLimit(host2)).thenReturn(true); // Reached limit

        List<Host> result = allocator.allocateTo(plan, offering, null, avoid, inputHosts, 2, true, account);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllocateTo_GPUNotAvailable() {
        List<Host> inputHosts = Arrays.asList(host1);
        when(avoid.shouldAvoid(host1)).thenReturn(false);

        // GPU required but not available
        var vgpuDetail = mock(com.cloud.service.ServiceOfferingDetailsVO.class);
        var pciDetail = mock(com.cloud.service.ServiceOfferingDetailsVO.class);
        when(offeringDetailsDao.findDetail(eq(123L), eq("vgpuType"))).thenReturn(vgpuDetail);
        when(offeringDetailsDao.findDetail(eq(123L), eq("pciDevice"))).thenReturn(pciDetail);
        when(pciDetail.getValue()).thenReturn("NVIDIA");
        when(vgpuDetail.getValue()).thenReturn("GRID");

        when(resourceMgr.isGPUDeviceAvailable(eq(host1), eq("NVIDIA"), eq("GRID"))).thenReturn(false);

        List<Host> result = allocator.allocateTo(plan, offering, null, avoid, inputHosts, 1, true, account);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testHostByCombinedCapacityOrder() {
        // Test scenario 1: Default capacity usage (0.5 weight)
        List<CapacityVO> mockCapacity = getHostCapacities();
        Map<Long, Double> hostByCombinedCapacity = FirstFitAllocator.getHostByCombinedCapacities(mockCapacity, 0.5);

        // Verify host ordering and capacity values
        Long firstHostId = hostByCombinedCapacity.keySet().iterator().next();
        Assert.assertEquals("Host with ID 1 should be first in ordering", Long.valueOf(1L), firstHostId);
        Assert.assertEquals("Host 1 combined capacity should match expected value",
                0.9609375, hostByCombinedCapacity.get(1L), TOLERANCE);
        Assert.assertEquals("Host 2 combined capacity should match expected value",
                0.9296875, hostByCombinedCapacity.get(2L), TOLERANCE);

        // Test scenario 2: Modified capacity usage (0.7 weight)
        when(mockCapacity.get(0).getUsedCapacity()).thenReturn(1500L);
        hostByCombinedCapacity = FirstFitAllocator.getHostByCombinedCapacities(mockCapacity, 0.7);

        // Verify new ordering after capacity change
        firstHostId = hostByCombinedCapacity.keySet().iterator().next();
        Assert.assertEquals("Host with ID 2 should be first after capacity change", Long.valueOf(2L), firstHostId);
        Assert.assertEquals("Host 2 combined capacity should match expected value after change",
                0.9515625, hostByCombinedCapacity.get(2L), TOLERANCE);
        Assert.assertEquals("Host 1 combined capacity should match expected value after change",
                0.9484375, hostByCombinedCapacity.get(1L), TOLERANCE);
    }

    List<CapacityVO> getHostCapacities() {
        CapacityVO cpuCapacity1 = mock(CapacityVO.class);
        when(cpuCapacity1.getHostOrPoolId()).thenReturn(1L);
        when(cpuCapacity1.getTotalCapacity()).thenReturn(32000L);
        when(cpuCapacity1.getReservedCapacity()).thenReturn(0L);
        when(cpuCapacity1.getUsedCapacity()).thenReturn(500L);
        when(cpuCapacity1.getCapacityType()).thenReturn(CapacityVO.CAPACITY_TYPE_CPU);

        CapacityVO cpuCapacity2 = mock(CapacityVO.class);
        when(cpuCapacity2.getHostOrPoolId()).thenReturn(2L);
        when(cpuCapacity2.getTotalCapacity()).thenReturn(32000L);
        when(cpuCapacity2.getReservedCapacity()).thenReturn(0L);
        when(cpuCapacity2.getUsedCapacity()).thenReturn(500L);
        when(cpuCapacity2.getCapacityType()).thenReturn(CapacityVO.CAPACITY_TYPE_CPU);

        CapacityVO memCapacity1 = mock(CapacityVO.class);
        when(memCapacity1.getHostOrPoolId()).thenReturn(1L);
        when(memCapacity1.getTotalCapacity()).thenReturn(8589934592L);
        when(memCapacity1.getReservedCapacity()).thenReturn(0L);
        when(memCapacity1.getUsedCapacity()).thenReturn(536870912L);
        when(memCapacity1.getCapacityType()).thenReturn(CapacityVO.CAPACITY_TYPE_MEMORY);

        CapacityVO memCapacity2 = mock(CapacityVO.class);
        when(memCapacity2.getHostOrPoolId()).thenReturn(2L);
        when(memCapacity2.getTotalCapacity()).thenReturn(8589934592L);
        when(memCapacity2.getReservedCapacity()).thenReturn(0L);
        when(memCapacity2.getUsedCapacity()).thenReturn(1073741824L);
        when(memCapacity1.getCapacityType()).thenReturn(CapacityVO.CAPACITY_TYPE_MEMORY);
        return Arrays.asList(cpuCapacity1, memCapacity1, cpuCapacity2, memCapacity2);
    }
}
