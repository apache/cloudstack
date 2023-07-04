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

import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.host.Host;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Ternary;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetric;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsThreshold;
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

    HostJoinVO host1, host2;

    long clusterId = 1L;

    Map<Long, List<VirtualMachine>> hostVmMap;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    @Mock
    private HostJoinDao hostJoinDao;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        vm1 = Mockito.mock(VirtualMachine.class);
        vm2 = Mockito.mock(VirtualMachine.class);
        vm3 = Mockito.mock(VirtualMachine.class); // vm to migrate

        destHost = Mockito.mock(Host.class);
        host1 = Mockito.mock(HostJoinVO.class); // Dest host
        host2 = Mockito.mock(HostJoinVO.class);

        hostVmMap = new HashMap<>();
        hostVmMap.put(1L, Collections.singletonList(vm1));
        hostVmMap.put(2L, Arrays.asList(vm2, vm3));

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(vm3.getHostId()).thenReturn(2L);

        Mockito.when(hostJoinDao.searchByIds(1L, 2L)).thenReturn(Arrays.asList(host1, host2));

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.when(host1.getCpuUsedCapacity()).thenReturn(1L);
        Mockito.when(host1.getMemUsedCapacity()).thenReturn(512L);

        Mockito.when(host2.getId()).thenReturn(2L);
        Mockito.when(host2.getCpuUsedCapacity()).thenReturn(2L);
        Mockito.when(host2.getMemUsedCapacity()).thenReturn(2048L);

        Mockito.when(destHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getTotalMemory()).thenReturn(8192L);

        Mockito.when(vm3.getId()).thenReturn(3L);
        Mockito.when(vm3.getHostId()).thenReturn(2L);
        Mockito.when(vm3.getServiceOfferingId()).thenReturn(1L);

        Mockito.when(serviceOffering.getCpu()).thenReturn(1);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(512);

        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(3L, 1L)).thenReturn(serviceOffering);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Test
    public void needsDrs() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
        // Scenarios to test for needsDrs
        // 1. cluster with cpu metric
        // 2. cluster with memory metric
        // 3. cluster with "both" metric
        // 4. cluster with "either" metric
        // 5. cluster with "unknown" metric

        // CPU imbalance = 0.333
        // Memory imbalance = 0.6
        overrideDefaultConfigValue(ClusterDrsThreshold, "_defaultValue", "0.5");

        // 1. cluster with cpu metric
        // 0.3333 < 0.5 -> True
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "cpu");
        assertTrue(condensed.needsDrs(1L, hostVmMap));

        // 2. cluster with memory metric
        // 0.6 < 0.5 -> False
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "memory");
        assertFalse(condensed.needsDrs(1L, hostVmMap));

        // 3. cluster with "both" metric
        // 0.3333 < 0.5 && 0.6 < 0.5 -> False
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "both");
        assertFalse(condensed.needsDrs(1L, hostVmMap));

        // 4. cluster with "either" metric
        // 0.3333 < 0.5 || 0.5 < 0.5 -> True
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "either");
        assertTrue(condensed.needsDrs(1L, hostVmMap));

        // 5. cluster with "unknown" metric
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "unknown");
        assertThrows(ConfigurationException.class, () -> condensed.needsDrs(1L, hostVmMap));
    }

    @Test
    public void getMetrics() throws NoSuchFieldException, IllegalAccessException {
        // Scenarios to test for getMetrics
        // 1. cluster with cpu metric
        // 2. cluster with memory metric
        // 3. cluster with default metric


        // Pre
        // CPU imbalance = 0.333333
        // Memory imbalance = 0.6
        // Post
        // CPU imbalance = 0.3333
        // Memory imbalance = 0.2
        //
        // Cost 512.0
        // Benefit  (0.2-0.6) * 8192 = -3276.8
        overrideDefaultConfigValue(ClusterDrsThreshold, "_defaultValue", "0.5");

        // 1. cluster with cpu metric
        // improvement = 0.3333 - 0.3333 = 0.0
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "cpu");
        Ternary<Double, Double, Double> result = condensed.getMetrics(clusterId, hostVmMap, vm3, destHost, false);
//        Ternary<Double, Double, Double> expected = new Ternary<Double, Double, Double>(-0.33, 0.0, -1 / 3.0););
        assertEquals(0.0, result.first(), 0.0);
        assertEquals(512.0, result.second(), 0.0);
        assertEquals(-3276.8, result.third(), 0.01);

        // 2. cluster with memory metric
        // improvement = 0.2 - 0.6 = -0.4
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "memory");
        result = condensed.getMetrics(clusterId, hostVmMap, vm3, destHost, false);
        assertEquals(-0.4, result.first(), 0.01);
        assertEquals(512.0, result.second(), 0.0);
        assertEquals(-3276.8, result.third(), 0.01);

        // 3. cluster with default metric
        // improvement = 0.3333 + 0.2 - 0.3333 - 0.6 = -0.4
        overrideDefaultConfigValue(ClusterDrsMetric, "_defaultValue", "both");
        result = condensed.getMetrics(clusterId, hostVmMap, vm3, destHost, false);
        assertEquals(-0.4, result.first(), 0.0001);
        assertEquals(512.0, result.second(), 0.0);
        assertEquals(-3276.8, result.third(), 0.01);
    }
}
