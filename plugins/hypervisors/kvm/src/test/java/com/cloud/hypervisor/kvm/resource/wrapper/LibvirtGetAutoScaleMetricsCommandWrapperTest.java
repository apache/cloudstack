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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.VirtualRouterAutoScaleCounter;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LibvirtComputingResource.class)
public class LibvirtGetAutoScaleMetricsCommandWrapperTest extends TestCase {

    @Spy
    LibvirtGetAutoScaleMetricsCommandWrapper libvirtGetAutoScaleMetricsCommandWrapperSpy = Mockito.spy(LibvirtGetAutoScaleMetricsCommandWrapper.class);

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    GetAutoScaleMetricsCommand getAutoScaleMetricsCommandMock;

    final static long[] vpcStats = { 1L, 2L };
    final static long[] networkStats = { 3L, 4L };
    final static long[] lbStats = { 5L };

    @Before
    public void init() {
        PowerMockito.mockStatic(LibvirtComputingResource.class);

        List<AutoScaleMetrics> metrics = new ArrayList<>();
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.LbAverageConnections, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NetworkReceive, 1L, 2L, 3L, 4));
        metrics.add(new AutoScaleMetrics(VirtualRouterAutoScaleCounter.NetworkTransmit, 1L, 2L, 3L, 4));

        Mockito.when(getAutoScaleMetricsCommandMock.getMetrics()).thenReturn(metrics);
    }

    @Test
    public void validateVpcStats() {

        Mockito.when(getAutoScaleMetricsCommandMock.isForVpc()).thenReturn(true);
        PowerMockito.when(libvirtComputingResourceMock.getVPCNetworkStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(vpcStats);
        PowerMockito.when(libvirtComputingResourceMock.getNetworkLbStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(lbStats);

        Answer answer = libvirtGetAutoScaleMetricsCommandWrapperSpy.execute(getAutoScaleMetricsCommandMock, libvirtComputingResourceMock);
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(values.size(), 3);
        for (AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.LbAverageConnections)) {
                assertEquals(value.getValue(), Double.valueOf(lbStats[0]));
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NetworkTransmit)) {
                assertEquals(value.getValue(), Double.valueOf(vpcStats[0]));
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NetworkReceive)) {
                assertEquals(value.getValue(), Double.valueOf(vpcStats[1]));
            }
        }

        Mockito.verify(libvirtComputingResourceMock, Mockito.never()).getNetworkStats(Mockito.any(), Mockito.any());
    }

    @Test
    public void validateNetworkStats() {

        Mockito.when(getAutoScaleMetricsCommandMock.isForVpc()).thenReturn(false);
        PowerMockito.when(libvirtComputingResourceMock.getNetworkStats(Mockito.any(), Mockito.any())).thenReturn(networkStats);
        PowerMockito.when(libvirtComputingResourceMock.getNetworkLbStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(lbStats);

        Answer answer = libvirtGetAutoScaleMetricsCommandWrapperSpy.execute(getAutoScaleMetricsCommandMock, libvirtComputingResourceMock);
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(values.size(), 3);
        for (AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.LbAverageConnections)) {
                assertEquals(value.getValue(), Double.valueOf(lbStats[0]));
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NetworkTransmit)) {
                assertEquals(value.getValue(), Double.valueOf(networkStats[0]));
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScaleCounter.NetworkReceive)) {
                assertEquals(value.getValue(), Double.valueOf(networkStats[1]));
            }
        }

        Mockito.verify(libvirtComputingResourceMock, Mockito.never()).getVPCNetworkStats(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
