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
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.vm.VirtualMachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class WeightedTest {

    private static final long CLUSTER_ID = 1L;
    private static final long CPU_TOTAL = 192L * 2400L;
    private static final long MEMORY_TOTAL = 1_132_000L * 1024L * 1024L;
    private static final int CPU_OVERCOMMIT = 10;
    private static final int MEMORY_OVERCOMMIT = 4;

    @Mock
    private ClusterDetailsDao clusterDetailsDao;

    @InjectMocks
    private Weighted weighted = new Weighted();

    private Cluster cluster;
    private final Map<Long, Ternary<Long, Long, Long>> cpu = new HashMap<>();
    private final Map<Long, Ternary<Long, Long, Long>> memory = new HashMap<>();
    private final Map<Long, HostLoad> load = new HashMap<>();

    @Before
    public void setUp() {
        cluster = Mockito.mock(Cluster.class);
        Mockito.lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.lenient().when(clusterDetailsDao.findDetail(CLUSTER_ID, "cpuOvercommitRatio"))
                .thenReturn(new ClusterDetailsVO(CLUSTER_ID, "cpuOvercommitRatio", String.valueOf(CPU_OVERCOMMIT)));
        Mockito.lenient().when(clusterDetailsDao.findDetail(CLUSTER_ID, "memoryOvercommitRatio"))
                .thenReturn(new ClusterDetailsVO(CLUSTER_ID, "memoryOvercommitRatio", String.valueOf(MEMORY_OVERCOMMIT)));
    }

    private void host(long id, double cpuAllocatedFraction, double memoryAllocatedFraction, HostLoad hostLoad) {
        host(id, cpuAllocatedFraction, memoryAllocatedFraction, hostLoad, 0L, 0L);
    }

    private void host(long id, double cpuAllocatedFraction, double memoryAllocatedFraction, HostLoad hostLoad,
            long reservedCpu, long reservedMemory) {
        cpu.put(id, new Ternary<>((long) (CPU_TOTAL * CPU_OVERCOMMIT * cpuAllocatedFraction), reservedCpu, CPU_TOTAL));
        memory.put(id, new Ternary<>((long) (MEMORY_TOTAL * MEMORY_OVERCOMMIT * memoryAllocatedFraction),
                reservedMemory, MEMORY_TOTAL));
        load.put(id, hostLoad);
    }

    /** DRS calls prepare once per plan; every test must do the same. */
    private Map<Long, Double> blend() {
        weighted.prepare(cluster, cpu, memory, load);
        return weighted.blendByHost(cluster, cpu, memory);
    }

    @Test
    public void testAnEvenClusterHasNoImbalance() {
        host(1L, 0.4, 0.4, new HostLoad(0.4, 0.4, 10));
        host(2L, 0.4, 0.4, new HostLoad(0.4, 0.4, 10));

        assertEquals(0.0, weighted.imbalanceOf(blend().values()), 1e-9);
    }

    @Test
    public void testCpuLoadImbalanceIsSeenWhenMemoryIsEven() {
        // the case a memory-only metric misses entirely: memory even, CPU load three fold apart
        host(1L, 0.30, 0.40, new HostLoad(0.90, 0.40, 10));
        host(2L, 0.30, 0.40, new HostLoad(0.30, 0.40, 10));

        assertTrue("balancing on memory alone would call this cluster even",
                weighted.imbalanceOf(blend().values()) > 0.15);
    }

    @Test
    public void testMemoryImbalanceIsSeenWhenCpuIsEven() {
        host(1L, 0.30, 0.90, new HostLoad(0.30, 0.90, 10));
        host(2L, 0.30, 0.10, new HostLoad(0.30, 0.10, 10));

        assertTrue(weighted.imbalanceOf(blend().values()) > 0.15);
    }

    @Test
    public void testAllocationBeyondPhysicalSizeStillDiscriminates() {
        // at a factor of 10 both hosts are past their physical CPU; they must not both read as full
        host(1L, 0.20, 0.20, HostLoad.UNKNOWN);
        host(2L, 0.80, 0.20, HostLoad.UNKNOWN);

        Map<Long, Double> blended = blend();
        assertTrue(blended.get(2L) > blended.get(1L));
    }

    @Test
    public void testUnmeasuredHostsFallBackToAllocation() {
        host(1L, 0.20, 0.20, HostLoad.UNKNOWN);
        host(2L, 0.60, 0.60, HostLoad.UNKNOWN);

        Map<Long, Double> blended = blend();
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

        weighted.prepare(cluster, cpu, memory, load);
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

        weighted.prepare(cluster, cpu, memory, load);
        Ternary<Double, Double, Double> metrics = weighted.getMetrics(cluster, vm, offering, dest,
                cpu, memory, false, null, new double[0], new HashMap<>());

        assertTrue("piling onto the busier host makes the cluster less even", metrics.first() < 0);
        // the caller migrates when benefit > cost
        assertFalse("and must not be considered worth doing", metrics.third() > metrics.second());
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

        weighted.prepare(cluster, cpu, memory, load);
        double withoutStorage = weighted.getMetrics(cluster, vm, offering, dest, cpu, memory, false,
                null, new double[0], new HashMap<>()).second();
        double withStorage = weighted.getMetrics(cluster, vm, offering, dest, cpu, memory, true,
                null, new double[0], new HashMap<>()).second();

        assertTrue("a migration that has to move storage should cost more", withStorage > withoutStorage);
    }

    @Test
    public void testReservedCapacityDoesNotMakeAHostLookFuller() {
        // reserved is taken off the overcommitted total, not multiplied by the ratio. Getting this
        // backwards made a host with reserved capacity read far fuller than an identical one
        // without, and DRS would evacuate it for no reason.
        host(1L, 0.10, 0.10, HostLoad.UNKNOWN, 0L, 0L);
        host(2L, 0.10, 0.10, HostLoad.UNKNOWN, CPU_TOTAL / 10, MEMORY_TOTAL / 10);

        Map<Long, Double> blended = blend();

        assertEquals("a little reserved capacity should barely move the figure",
                blended.get(1L), blended.get(2L), 0.02);
    }

    @Test
    public void testAllocationBeyondTheOvercommittedTotalIsNotFlattened() {
        // hosts past what they can hand out must stay distinguishable, not both read as full
        host(1L, 1.20, 0.20, HostLoad.UNKNOWN);
        host(2L, 2.40, 0.20, HostLoad.UNKNOWN);

        Map<Long, Double> blended = blend();

        assertTrue("two oversubscribed hosts must not read identically",
                blended.get(2L) > blended.get(1L));
    }

    @Test
    public void testUtilisationIsDroppedWhenAnyHostCannotBeMeasured() {
        // one host with broken telemetry must not read as loaded simply because it is measured on a
        // different basis to its peers - that would evacuate whichever host stopped reporting
        host(1L, 0.50, 0.50, new HostLoad(0.10, 0.10, 10));
        host(2L, 0.50, 0.50, HostLoad.UNKNOWN);

        Map<Long, Double> blended = blend();

        assertEquals("identical hosts must read identically when one cannot be measured",
                blended.get(1L), blended.get(2L), 1e-9);
        assertEquals(0.0, weighted.imbalanceOf(blended.values()), 1e-9);
    }

    @Test
    public void testUtilisationIsUsedWhenEveryHostIsMeasured() {
        host(1L, 0.50, 0.50, new HostLoad(0.90, 0.50, 10));
        host(2L, 0.50, 0.50, new HostLoad(0.10, 0.50, 10));

        assertTrue("with every host measured, load must separate them",
                weighted.imbalanceOf(blend().values()) > 0.05);
    }

    @Test
    public void testStorageMotionMustEarnMoreThanAPlainMigration() throws ConfigurationException {
        host(1L, 0.52, 0.50, new HostLoad(0.52, 0.50, 10));
        host(2L, 0.48, 0.50, new HostLoad(0.48, 0.50, 10));

        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getHostId()).thenReturn(1L);
        Host dest = Mockito.mock(Host.class);
        Mockito.when(dest.getId()).thenReturn(2L);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(1);
        Mockito.when(offering.getSpeed()).thenReturn(500);
        Mockito.when(offering.getRamSize()).thenReturn(512);

        weighted.prepare(cluster, cpu, memory, load);
        Ternary<Double, Double, Double> plain = weighted.getMetrics(cluster, vm, offering, dest,
                cpu, memory, false, null, new double[0], new HashMap<>());
        weighted.prepare(cluster, cpu, memory, load);
        Ternary<Double, Double, Double> withStorage = weighted.getMetrics(cluster, vm, offering, dest,
                cpu, memory, true, null, new double[0], new HashMap<>());

        // the caller migrates when benefit > cost
        assertTrue("a small gain is worth taking without moving storage",
                plain.third() > plain.second());
        assertFalse("the same small gain is not worth moving storage for",
                withStorage.third() > withStorage.second());
    }
}
