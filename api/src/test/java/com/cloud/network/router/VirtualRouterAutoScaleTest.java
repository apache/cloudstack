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
package com.cloud.network.router;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.cloud.network.router.VirtualRouterAutoScale.VirtualRouterAutoScaleCounter;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleValueType;

public class VirtualRouterAutoScaleTest {

    private void testVirtualRouterAutoScaleCounter(VirtualRouterAutoScaleCounter counter, String value) {
        VirtualRouterAutoScaleCounter counterFromValue = VirtualRouterAutoScaleCounter.fromValue(value);
        assertEquals(counter, counterFromValue);
        if (counterFromValue != null) {
            assertEquals(value, counterFromValue.getValue());
            assertEquals(value, counterFromValue.toString());
        }
    }

    @Test
    public void testVirtualRouterAutoScaleCounters() {
        testVirtualRouterAutoScaleCounter(VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS, "public.network.received.average.mbps");
        testVirtualRouterAutoScaleCounter(VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS, "public.network.transmit.average.mbps");
        testVirtualRouterAutoScaleCounter(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, "virtual.network.lb.average.connections");
        testVirtualRouterAutoScaleCounter(null, "invalid");
    }

    @Test
    public void testAutoScaleMetrics() {
        AutoScaleMetrics metrics = new AutoScaleMetrics(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4);

        assertEquals(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, metrics.getCounter());
        assertEquals(1L, (long) metrics.getPolicyId());
        assertEquals(2L, (long) metrics.getConditionId());
        assertEquals(3L, (long) metrics.getCounterId());
        assertEquals(4, (int) metrics.getDuration());
    }

    @Test
    public void testAutoScaleMetricsValue() {
        AutoScaleMetrics metrics = new AutoScaleMetrics(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4);

        AutoScaleMetricsValue value = new AutoScaleMetricsValue(metrics, AutoScaleValueType.INSTANT_VM, 123.45);

        assertEquals(metrics, value.getMetrics());
        assertEquals(AutoScaleValueType.INSTANT_VM, value.getType());
        assertEquals(123.45, (double) value.getValue(), 0);
    }
}
