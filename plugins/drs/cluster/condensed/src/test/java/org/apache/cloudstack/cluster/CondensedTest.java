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
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.Ternary;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CondensedTest {

    @InjectMocks
    Condensed condensed;

    VirtualMachine vm1, vm2, vm3;

    Host destHost;

    ServiceOfferingVO serviceOffering;

    long clusterId = 1L;

    Map<Long, List<VirtualMachine>> hostVmMap;

    List<Long> cpuList, memoryList;

    Map<Long, Long> hostCpuUsedMap, hostMemoryUsedMap;


    private AutoCloseable closeable;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        closeable = MockitoAnnotations.openMocks(this);

        vm1 = Mockito.mock(VirtualMachine.class);
        vm2 = Mockito.mock(VirtualMachine.class);
        vm3 = Mockito.mock(VirtualMachine.class); // vm to migrate

        destHost = Mockito.mock(Host.class);
        hostVmMap = new HashMap<>();
        hostVmMap.put(1L, Collections.singletonList(vm1));
        hostVmMap.put(2L, Arrays.asList(vm2, vm3));

        serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(vm3.getHostId()).thenReturn(2L);

        Mockito.when(destHost.getId()).thenReturn(1L);

        Mockito.when(serviceOffering.getCpu()).thenReturn(1);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(512);

        overrideDefaultConfigValue(ClusterDrsImbalanceThreshold, "_defaultValue", "0.5");

        cpuList = Arrays.asList(1L, 2L);
        memoryList = Arrays.asList(512L, 2048L);

        hostCpuUsedMap = new HashMap<>();
        hostCpuUsedMap.put(1L, 1000L);
        hostCpuUsedMap.put(2L, 2000L);

        hostMemoryUsedMap = new HashMap<>();
        hostMemoryUsedMap.put(1L, 512L * 1024L * 1024L);
        hostMemoryUsedMap.put(2L, 2048L * 1024L * 1024L);
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey,
                                            final String name,
                                            final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    /**
     * <p>needsDrs tests
     * <p>Scenarios to test for needsDrs
     * <p>1. cluster with cpu metric
     * <p>2. cluster with memory metric
     * <p>3. cluster with "unknown" metric
     * <p>
     * <p>CPU imbalance = 0.333
     * <p>Memory imbalance = 0.6
     */

    /*
     1. cluster with cpu metric
     0.3333 < 0.5 -> True
    */
    @Test
    public void needsDrsWithCpu() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "cpu");
        assertTrue(condensed.needsDrs(clusterId, cpuList, memoryList));
    }

    /*
     2. cluster with memory metric
     0.6 < 0.5 -> False
    */
    @Test
    public void needsDrsWithMemory() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "memory");
        assertFalse(condensed.needsDrs(clusterId, cpuList, memoryList));
    }

    /* 3. cluster with "unknown" metric */
    @Test
    public void needsDrsWithUnknown() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "unknown");
        assertThrows(ConfigurationException.class, () -> condensed.needsDrs(clusterId, cpuList, memoryList));
    }

    /**
     * getMetrics tests
     * <p>Scenarios to test for getMetrics
     * <p>1. cluster with cpu metric
     * <p>2. cluster with memory metric
     * <p>3. cluster with default metric
     * <p>
     * <p>Pre
     * <p>CPU imbalance = 0.333333
     * <p>Memory imbalance = 0.6
     * <p>
     * <p>Post
     * <p>CPU imbalance = 0.3333
     * <p>Memory imbalance = 0.2
     * <p>
     * <p>Cost 512.0
     * <p>Benefit  (0.2-0.6) * 8192 = -3276.8
     */

    /*
     1. cluster with cpu metric
     improvement = 0.3333 - 0.3333 = 0.0
    */
    @Test
    public void getMetricsWithCpu() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "cpu");
        Ternary<Double, Double, Double> result = condensed.getMetrics(clusterId, vm3, serviceOffering, destHost,
                hostCpuUsedMap, hostMemoryUsedMap, false);
        assertEquals(0.0, result.first(), 0.0);
        assertEquals(0, result.second(), 0.0);
        assertEquals(1, result.third(), 0.0);
    }

    /*
     2. cluster with memory metric
     improvement = 0.2 - 0.6 = -0.4
    */
    @Test
    public void getMetricsWithMemory() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "memory");
        Ternary<Double, Double, Double> result = condensed.getMetrics(clusterId, vm3, serviceOffering, destHost,
                hostCpuUsedMap, hostMemoryUsedMap, false);
        assertEquals(-0.4, result.first(), 0.01);
        assertEquals(0, result.second(), 0.0);
        assertEquals(1, result.third(), 0.0);
    }

    /*
     3. cluster with default metric
     improvement = 0.3333 + 0.2 - 0.3333 - 0.6 = -0.4
    */
    @Test
    public void getMetricsWithDefault() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "both");
        Ternary<Double, Double, Double> result = condensed.getMetrics(clusterId, vm3, serviceOffering, destHost,
                hostCpuUsedMap, hostMemoryUsedMap, false);
        assertEquals(-0.4, result.first(), 0.0001);
        assertEquals(0, result.second(), 0.0);
        assertEquals(1, result.third(), 0.0);
    }
}
