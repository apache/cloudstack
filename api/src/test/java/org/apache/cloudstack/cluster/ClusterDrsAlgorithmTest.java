/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import junit.framework.TestCase;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsAlgorithm.getMetricValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsAlgorithmTest extends TestCase {

    @Test
    public void testGetMetricValue() {
        List<Ternary<Boolean, String, Double>> testData = List.of(
                new Ternary<>(true, "free", 0.4),
                new Ternary<>(false, "free", 40.0),
                new Ternary<>(true, "used", 0.3),
                new Ternary<>(false, "used", 30.0)
        );

        long used = 30;
        long free = 40;
        long total = 100;

        for (Ternary<Boolean, String, Double> data : testData) {
            boolean useRatio = data.first();
            String metricType = data.second();
            double expectedValue = data.third();

            try (MockedStatic<ClusterDrsAlgorithm> ignored = Mockito.mockStatic(ClusterDrsAlgorithm.class)) {
                when(ClusterDrsAlgorithm.getDrsMetricUseRatio(1L)).thenReturn(useRatio);
                when(ClusterDrsAlgorithm.getDrsMetricType(1L)).thenReturn(metricType);
                when(ClusterDrsAlgorithm.getMetricValue(anyLong(), anyLong(), anyLong(), anyLong(), any())).thenCallRealMethod();

                assertEquals(expectedValue, getMetricValue(1, used, free, total, null));
            }
        }
    }

    @Test
    public void testGetMetricValueWithSkipThreshold() {
        List<Ternary<Boolean, String, Double>> testData = List.of(
                new Ternary<>(true, "free", 0.15),
                new Ternary<>(false, "free", 15.0),
                new Ternary<>(true, "used", null),
                new Ternary<>(false, "used", null)
        );

        long used = 80;
        long free = 15;
        long total = 100;

        for (Ternary<Boolean, String, Double> data : testData) {
            boolean useRatio = data.first();
            String metricType = data.second();
            Double expectedValue = data.third();
            float skipThreshold = metricType.equals("free") ? 0.1f : 0.7f;

            try (MockedStatic<ClusterDrsAlgorithm> ignored = Mockito.mockStatic(ClusterDrsAlgorithm.class)) {
                when(ClusterDrsAlgorithm.getDrsMetricUseRatio(1L)).thenReturn(useRatio);
                when(ClusterDrsAlgorithm.getDrsMetricType(1L)).thenReturn(metricType);
                when(ClusterDrsAlgorithm.getMetricValue(anyLong(), anyLong(), anyLong(), anyLong(), anyFloat())).thenCallRealMethod();

                assertEquals(expectedValue, ClusterDrsAlgorithm.getMetricValue(1L, used, free, total, skipThreshold));
            }
        }
    }

    @Test
    public void testGetClusterImbalanceUsesWorseOfCpuAndMemoryForBothMetric() throws Exception {
        Field defaultValueField = ConfigKey.class.getDeclaredField("_defaultValue");
        defaultValueField.setAccessible(true);
        Object originalMetric = defaultValueField.get(ClusterDrsService.ClusterDrsMetric);
        try {
            List<Ternary<Long, Long, Long>> cpuList = List.of(
                    new Ternary<>(80L, 0L, 100L), new Ternary<>(20L, 0L, 100L));
            List<Ternary<Long, Long, Long>> memoryList = List.of(
                    new Ternary<>(50L, 0L, 100L), new Ternary<>(50L, 0L, 100L));

            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, "cpu");
            double cpu = ClusterDrsAlgorithm.getClusterImbalance(1L, cpuList, memoryList, null);
            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, "memory");
            double memory = ClusterDrsAlgorithm.getClusterImbalance(1L, cpuList, memoryList, null);
            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, "both");
            double both = ClusterDrsAlgorithm.getClusterImbalance(1L, cpuList, memoryList, null);

            // "both" must return the worse (max) of the per-resource imbalances
            assertEquals(Math.max(cpu, memory), both, 0.0001);
        } finally {
            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, originalMetric);
        }
    }

    @Test
    public void testGetImbalancePostMigrationForBothMetric() throws Exception {
        Field defaultValueField = ConfigKey.class.getDeclaredField("_defaultValue");
        defaultValueField.setAccessible(true);
        Object originalMetric = defaultValueField.get(ClusterDrsService.ClusterDrsMetric);
        try {
            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, "both");

            VirtualMachine vm = Mockito.mock(VirtualMachine.class);
            Mockito.when(vm.getHostId()).thenReturn(1L);
            Host destHost = Mockito.mock(Host.class);
            Mockito.when(destHost.getId()).thenReturn(2L);
            ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
            Mockito.when(serviceOffering.getCpu()).thenReturn(2);
            Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);
            Mockito.when(serviceOffering.getRamSize()).thenReturn(2048);

            Map<Long, Ternary<Long, Long, Long>> hostCpuMap = Map.of(
                    1L, new Ternary<>(80L, 0L, 100L), 2L, new Ternary<>(20L, 0L, 100L));
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap = Map.of(
                    1L, new Ternary<>(50L, 0L, 100L), 2L, new Ternary<>(50L, 0L, 100L));

            ClusterDrsAlgorithm algorithm = new TestAlgorithm();
            // the "both" branch evaluates the cpu and memory maps directly, so the base array and index map are unused
            Double imbalance = algorithm.getImbalancePostMigration(vm, destHost, 1L, serviceOffering, null, null,
                    hostCpuMap, hostMemoryMap);

            assertNotNull(imbalance);
        } finally {
            defaultValueField.set(ClusterDrsService.ClusterDrsMetric, originalMetric);
        }
    }

    private static class TestAlgorithm extends AdapterBase implements ClusterDrsAlgorithm {
        @Override
        public boolean needsDrs(Cluster cluster, List<Ternary<Long, Long, Long>> cpuList,
                List<Ternary<Long, Long, Long>> memoryList) {
            return false;
        }

        @Override
        public Ternary<Double, Double, Double> getMetrics(Cluster cluster, VirtualMachine vm, ServiceOffering serviceOffering,
                Host destHost, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
                Map<Long, Ternary<Long, Long, Long>> hostMemoryMap, Boolean requiresStorageMotion, Double preImbalance,
                double[] baseMetricsArray, Map<Long, Integer> hostIdToIndexMap) {
            return null;
        }
    }
}
