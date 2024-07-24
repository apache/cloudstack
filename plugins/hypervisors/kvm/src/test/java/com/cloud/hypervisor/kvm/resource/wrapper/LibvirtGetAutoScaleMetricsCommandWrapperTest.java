/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.router.VirtualRouterAutoScale;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.VirtualRouterAutoScaleCounter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtGetAutoScaleMetricsCommandWrapperTest {

    @Spy
    LibvirtGetAutoScaleMetricsCommandWrapper libvirtGetAutoScaleMetricsCommandWrapperSpy = Mockito.spy(LibvirtGetAutoScaleMetricsCommandWrapper.class);

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    GetAutoScaleMetricsCommand getAutoScaleMetricsCommandMock;

    final static long[] vpcStats = { 1000L, 2000L };
    final static long[] networkStats = { 3000L, 4000L };
    final static long[] lbStats = { 5L };

    @Before
    public void init() {
        List<AutoScaleMetrics> metrics = new ArrayList<>();
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS, 1L, 2L, 3L, 4));

        Mockito.when(getAutoScaleMetricsCommandMock.getMetrics()).thenReturn(metrics);
    }

    @Test
    public void validateVpcStats() {

        Mockito.when(getAutoScaleMetricsCommandMock.isForVpc()).thenReturn(true);
        Mockito.when(libvirtComputingResourceMock.getVPCNetworkStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(vpcStats);
        Mockito.when(libvirtComputingResourceMock.getNetworkLbStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(lbStats);

        Answer answer = libvirtGetAutoScaleMetricsCommandWrapperSpy.execute(getAutoScaleMetricsCommandMock, libvirtComputingResourceMock);
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(3, values.size());
        for (AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS)) {
                assertEquals(Double.valueOf(lbStats[0]), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(vpcStats[0]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(vpcStats[1]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            }
        }

        Mockito.verify(libvirtComputingResourceMock, Mockito.never()).getNetworkStats(Mockito.any(), Mockito.any());
    }

    @Test
    public void validateNetworkStats() {

        Mockito.when(getAutoScaleMetricsCommandMock.isForVpc()).thenReturn(false);
        Mockito.when(libvirtComputingResourceMock.getNetworkStats(Mockito.any(), Mockito.any())).thenReturn(networkStats);
        Mockito.when(libvirtComputingResourceMock.getNetworkLbStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(lbStats);

        Answer answer = libvirtGetAutoScaleMetricsCommandWrapperSpy.execute(getAutoScaleMetricsCommandMock, libvirtComputingResourceMock);
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(3, values.size());
        for (AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS)) {
                assertEquals(Double.valueOf(lbStats[0]), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(networkStats[0]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(networkStats[1]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            }
        }

        Mockito.verify(libvirtComputingResourceMock, Mockito.never()).getVPCNetworkStats(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
