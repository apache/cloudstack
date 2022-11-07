//
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
//

package org.apache.cloudstack.api.agent.test;

import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.network.router.VirtualRouterAutoScale;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.VirtualRouterAutoScaleCounter;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GetAutoScaleMetricsAnswerTest {
    private GetAutoScaleMetricsCommand command;

    private static final String privateIp = "privateip";
    private static final String publicIP = "publicIP";
    private static final Integer port = 8080;

    List<AutoScaleMetrics> metrics = new ArrayList<>();

    @Before
    public void setUp() {
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS, 1L, 2L, 3L, 4));

        command = new GetAutoScaleMetricsCommand(privateIp, true, publicIP, port, metrics);
    }

    @Test
    public void testFalseAnswer() {
        GetAutoScaleMetricsAnswer answer = new GetAutoScaleMetricsAnswer(command, false);

        assertFalse(answer.getResult());
        assertEquals(0, answer.getValues().size());
    }

    @Test
    public void testTrueAnswer() {
        GetAutoScaleMetricsAnswer answer = new GetAutoScaleMetricsAnswer(command, true);

        assertTrue(answer.getResult());
        assertEquals(0, answer.getValues().size());
    }

    @Test
    public void testAnswerWithValue() {
        GetAutoScaleMetricsAnswer answer = new GetAutoScaleMetricsAnswer(command, true, new ArrayList<>());

        assertTrue(answer.getResult());
        assertEquals(0, answer.getValues().size());

        answer.addValue(new AutoScaleMetricsValue(metrics.get(0), VirtualRouterAutoScale.AutoScaleValueType.INSTANT_VM, Double.valueOf(1)));
        assertEquals(1, answer.getValues().size());
        assertEquals(metrics.get(0), answer.getValues().get(0).getMetrics());
        assertEquals(Double.valueOf(1), answer.getValues().get(0).getValue());

        answer.addValue(new AutoScaleMetricsValue(metrics.get(1), VirtualRouterAutoScale.AutoScaleValueType.INSTANT_VM, Double.valueOf(2)));
        assertEquals(2, answer.getValues().size());
        assertEquals(metrics.get(1), answer.getValues().get(1).getMetrics());
        assertEquals(Double.valueOf(2), answer.getValues().get(1).getValue());

        answer.addValue(new AutoScaleMetricsValue(metrics.get(2), VirtualRouterAutoScale.AutoScaleValueType.INSTANT_VM, Double.valueOf(3)));
        assertEquals(3, answer.getValues().size());
        assertEquals(metrics.get(2), answer.getValues().get(2).getMetrics());
        assertEquals(Double.valueOf(3), answer.getValues().get(2).getValue());
    }
}
