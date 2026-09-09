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
package org.apache.cloudstack.cluster;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.host.HostLoad;
import com.cloud.host.HostLoadService;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.vm.VirtualMachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class WeightedTest {

    private static final long CLUSTER_ID = 1L;
    private static final long CPU_TOTAL = 192L * 2400L;
    private static final long MEMORY_TOTAL = 1_132_000L * 1024L * 1024L;
    private static final int CPU_OVERCOMMIT = 10;
    private static final int MEMORY_OVERCOMMIT = 4;

    @Mock
    private HostLoadService hostLoadService;

    @Mock
    private ClusterDetailsDao clusterDetailsDao;

    @InjectMocks
    private Weighted weighted = new Weighted();

    private Cluster cluster;
    private final Map<Long, Ternary<Long, Long, Long>> cpu = new HashMap<>();
    private final Map<Long, Ternary<Long, Long, Long>> memory = new HashMap<>();

    @Before
    public void setUp() {
        cluster = Mockito.mock(Cluster.class);
        Mockito.lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.lenient().when(clusterDetailsDao.findDetail(CLUSTER_ID, "cpuOvercommitRatio"))
                .thenReturn(new ClusterDetailsVO(CLUSTER_ID, "cpuOvercommitRatio", String.valueOf(CPU_OVERCOMMIT)));
        Mockito.lenient().when(clusterDetailsDao.findDetail(CLUSTER_ID, "memoryOvercommitRatio"))
                .thenReturn(new ClusterDetailsVO(CLUSTER_ID, "memoryOvercommitRatio", String.valueOf(MEMORY_OVERCOMMIT)));
    }

    private void host(long id, double cpuAllocatedFraction, double memoryAllocatedFraction, HostLoad load) {
        cpu.put(id, new Ternary<>((long) (CPU_TOTAL * CPU_OVERCOMMIT * cpuAllocatedFraction), 0L, CPU_TOTAL));
        memory.put(id, new Ternary<>((long) (MEMORY_TOTAL * MEMORY_OVERCOMMIT * memoryAllocatedFraction), 0L, MEMORY_TOTAL));
        Mockito.lenient().when(hostLoadService.getLoad(id)).thenReturn(load);
    }

    @Test
    public void testAnEvenClusterHasNoImbalance() {
        host(1L, 0.4, 0.4, new HostLoad(0.4, 0.4, 10));
        host(2L, 0.4, 0.4, new HostLoad(0.4, 0.4, 10));

        assertEquals(0.0, weighted.imbalanceOf(weighted.blendByHost(cluster, cpu, memory).values()), 1e-9);
    }

    @Test
    public void testCpuLoadImbalanceIsSeenWhenMemoryIsEven() {
        // the case a memory-only metric misses entirely: memory even, CPU load three fold apart
        host(1L, 0.30, 0.40, new HostLoad(0.90, 0.40, 10));
        host(2L, 0.30, 0.40, new HostLoad(0.30, 0.40, 10));

        assertTrue("balancing on memory alone would call this cluster even",
                weighted.imbalanceOf(weighted.blendByHost(cluster, cpu, memory).values()) > 0.15);
    }

    @Test
    public void testMemoryImbalanceIsSeenWhenCpuIsEven() {
        host(1L, 0.30, 0.90, new HostLoad(0.30, 0.90, 10));
        host(2L, 0.30, 0.10, new HostLoad(0.30, 0.10, 10));

        assertTrue(weighted.imbalanceOf(weighted.blendByHost(cluster, cpu, memory).values()) > 0.15);
    }

    @Test
    public void testAllocationBeyondPhysicalSizeStillDiscriminates() {
        // at a factor of 10 both hosts are past their physical CPU; they must not both read as full
        host(1L, 0.20, 0.20, HostLoad.UNKNOWN);
        host(2L, 0.80, 0.20, HostLoad.UNKNOWN);

        Map<Long, Double> blended = weighted.blendByHost(cluster, cpu, memory);
        assertTrue(blended.get(2L) > blended.get(1L));
    }

    @Test
    public void testUnmeasuredHostsFallBackToAllocation() {
        host(1L, 0.20, 0.20, HostLoad.UNKNOWN);
        host(2L, 0.60, 0.60, HostLoad.UNKNOWN);

        Map<Long, Double> blended = weighted.blendByHost(cluster, cpu, memory);
        assertEquals(0.20, blended.get(1L), 0.01);
        assertEquals(0.60, blended.get(2L), 0.01);
    }

    @Test
    public void testMovingAVmOffTheBusyHostIsAnImprovement() throws ConfigurationException {
        host(1L, 0.60, 0.60, new HostLoad(0.80, 0.60, 10));
        host(2L, 0.20, 0.20, new HostLoad(0.20, 0.20, 10));

        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getHostId()).thenReturn(1L);
        Host dest = Mockito.mock(Host.class);
        Mockito.when(dest.getId()).thenReturn(2L);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(8);
        Mockito.when(offering.getSpeed()).thenReturn(2400);
        Mockito.when(offering.getRamSize()).thenReturn(32768);

        Ternary<Double, Double, Double> metrics = weighted.getMetrics(cluster, vm, offering, dest,
                cpu, memory, false, null, new double[0], new HashMap<>());

        assertTrue("moving away from the busier host should even the cluster out", metrics.first() > 0);
        assertTrue("and should be considered worth doing", metrics.third() > metrics.second());
    }

    @Test
    public void testMovingAVmOntoTheBusyHostIsNotAnImprovement() throws ConfigurationException {
        host(1L, 0.60, 0.60, new HostLoad(0.80, 0.60, 10));
        host(2L, 0.20, 0.20, new HostLoad(0.20, 0.20, 10));

        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getHostId()).thenReturn(2L);
        Host dest = Mockito.mock(Host.class);
        Mockito.when(dest.getId()).thenReturn(1L);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(8);
        Mockito.when(offering.getSpeed()).thenReturn(2400);
        Mockito.when(offering.getRamSize()).thenReturn(32768);

        Ternary<Double, Double, Double> metrics = weighted.getMetrics(cluster, vm, offering, dest,
                cpu, memory, false, null, new double[0], new HashMap<>());

        assertTrue("piling onto the busier host makes the cluster less even", metrics.first() < 0);
        assertEquals("and must not be considered worth doing", 0.0, metrics.third(), 1e-9);
    }

    @Test
    public void testStorageMotionCostsMore() throws ConfigurationException {
        host(1L, 0.60, 0.60, new HostLoad(0.80, 0.60, 10));
        host(2L, 0.20, 0.20, new HostLoad(0.20, 0.20, 10));

        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getHostId()).thenReturn(1L);
        Host dest = Mockito.mock(Host.class);
        Mockito.when(dest.getId()).thenReturn(2L);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(8);
        Mockito.when(offering.getSpeed()).thenReturn(2400);
        Mockito.when(offering.getRamSize()).thenReturn(32768);

        double withoutStorage = weighted.getMetrics(cluster, vm, offering, dest, cpu, memory, false,
                null, new double[0], new HashMap<>()).second();
        double withStorage = weighted.getMetrics(cluster, vm, offering, dest, cpu, memory, true,
                null, new double[0], new HashMap<>()).second();

        assertTrue("a migration that has to move storage should cost more", withStorage > withoutStorage);
    }
}
