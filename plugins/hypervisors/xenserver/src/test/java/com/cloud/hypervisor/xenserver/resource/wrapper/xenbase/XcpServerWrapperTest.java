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
package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.network.router.VirtualRouterAutoScale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.hypervisor.xenserver.resource.XcpServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
public class XcpServerWrapperTest {

    @Mock
    protected XcpServerResource XcpServerResource;


    final static long[] vpcStats = { 1000L, 2000L };
    final static long[] networkStats = { 3000L, 4000L };
    final static long[] lbStats = { 5L };

    @Test
    public void testNetworkUsageCommandCreate() {
        final Connection conn = Mockito.mock(Connection.class);

        final String privateIP = "192.168.0.10";
        final String domRName = "dom";
        final String option = "create";
        final boolean forVpc = true;

        final NetworkUsageCommand usageCommand = new NetworkUsageCommand(privateIP, domRName, option, forVpc);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(XcpServerResource.getConnection()).thenReturn(conn);
        when(XcpServerResource.networkUsage(conn, usageCommand.getPrivateIP(), "create", null)).thenReturn("success");

        final Answer answer = wrapper.execute(usageCommand, XcpServerResource);

        verify(XcpServerResource, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandGet() {
        final Connection conn = Mockito.mock(Connection.class);

        final String privateIP = "192.168.0.10";
        final String domRName = "dom";
        final boolean forVpc = true;
        final String gatewayIp = "172.16.0.10";

        final NetworkUsageCommand usageCommand = new NetworkUsageCommand(privateIP, domRName, forVpc, gatewayIp);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(XcpServerResource.getConnection()).thenReturn(conn);
        when(XcpServerResource.getNetworkStats(conn, usageCommand.getPrivateIP(), null)).thenReturn(new long[]{1l, 1l});

        final Answer answer = wrapper.execute(usageCommand, XcpServerResource);

        verify(XcpServerResource, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandExceptiopn() {
        final Connection conn = Mockito.mock(Connection.class);

        final String privateIP = "192.168.0.10";
        final String domRName = "dom";
        final String option = null;
        final boolean forVpc = true;

        final NetworkUsageCommand usageCommand = new NetworkUsageCommand(privateIP, domRName, option, forVpc);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(XcpServerResource.getConnection()).thenReturn(conn);
        when(XcpServerResource.networkUsage(conn, usageCommand.getPrivateIP(), "create", null)).thenThrow(new CloudRuntimeException("FAILED"));

        final Answer answer = wrapper.execute(usageCommand, XcpServerResource);

        verify(XcpServerResource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }


    @Test
    public void testGetAutoScaleMetricsCommandForVpc() {
        List<VirtualRouterAutoScale.AutoScaleMetrics> metrics = new ArrayList<>();
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4));
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS, 1L, 2L, 3L, 4));
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS, 1L, 2L, 3L, 4));

        final GetAutoScaleMetricsCommand getAutoScaleMetricsCommand = new GetAutoScaleMetricsCommand("192.168.10.10", true, "10.10.10.10", 8080, metrics);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(XcpServerResource.getVPCNetworkStats(getAutoScaleMetricsCommand.getPrivateIP(), getAutoScaleMetricsCommand.getPublicIP())).thenReturn(vpcStats);
        when(XcpServerResource.getNetworkLbStats(getAutoScaleMetricsCommand.getPrivateIP(),  getAutoScaleMetricsCommand.getPublicIP(), getAutoScaleMetricsCommand.getPort())).thenReturn(lbStats);

        final Answer answer = wrapper.execute(getAutoScaleMetricsCommand, XcpServerResource);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<VirtualRouterAutoScale.AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(3, values.size());
        for (VirtualRouterAutoScale.AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS)) {
                assertEquals(Double.valueOf(lbStats[0]), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(vpcStats[0]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(vpcStats[1]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            }
        }
    }

    @Test
    public void testGetAutoScaleMetricsCommandForNetwork() {
        List<VirtualRouterAutoScale.AutoScaleMetrics> metrics = new ArrayList<>();
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS, 1L, 2L, 3L, 4));
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS, 1L, 2L, 3L, 4));
        metrics.add(new VirtualRouterAutoScale.AutoScaleMetrics(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS, 1L, 2L, 3L, 4));

        final GetAutoScaleMetricsCommand getAutoScaleMetricsCommand = new GetAutoScaleMetricsCommand("192.168.10.10", false, "10.10.10.10", 8080, metrics);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Connection conn = Mockito.mock(Connection.class);
        when(XcpServerResource.getConnection()).thenReturn(conn);
        when(XcpServerResource.getNetworkStats(conn, getAutoScaleMetricsCommand.getPrivateIP(), getAutoScaleMetricsCommand.getPublicIP())).thenReturn(networkStats);
        when(XcpServerResource.getNetworkLbStats(getAutoScaleMetricsCommand.getPrivateIP(),  getAutoScaleMetricsCommand.getPublicIP(), getAutoScaleMetricsCommand.getPort())).thenReturn(lbStats);

        final Answer answer = wrapper.execute(getAutoScaleMetricsCommand, XcpServerResource);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);

        GetAutoScaleMetricsAnswer getAutoScaleMetricsAnswer = (GetAutoScaleMetricsAnswer) answer;
        List<VirtualRouterAutoScale.AutoScaleMetricsValue> values = getAutoScaleMetricsAnswer.getValues();

        assertEquals(3, values.size());
        for (VirtualRouterAutoScale.AutoScaleMetricsValue value : values) {
            if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.LB_AVERAGE_CONNECTIONS)) {
                assertEquals(Double.valueOf(lbStats[0]), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_TRANSMIT_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(networkStats[0]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            } else if (value.getMetrics().getCounter().equals(VirtualRouterAutoScale.VirtualRouterAutoScaleCounter.NETWORK_RECEIVED_AVERAGE_MBPS)) {
                assertEquals(Double.valueOf(Double.valueOf(networkStats[1]) / VirtualRouterAutoScale.MBITS_TO_BYTES), value.getValue());
            }
        }
    }

    @Test
    public void testGetAutoScaleMetricsCommandWithException() {
        List<VirtualRouterAutoScale.AutoScaleMetrics> metrics = new ArrayList<>();

        final GetAutoScaleMetricsCommand getAutoScaleMetricsCommand = new GetAutoScaleMetricsCommand("192.168.10.10", false, "10.10.10.10", 8080, metrics);
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Connection conn = Mockito.mock(Connection.class);
        when(XcpServerResource.getConnection()).thenReturn(conn);
        when(XcpServerResource.getNetworkStats(conn, getAutoScaleMetricsCommand.getPrivateIP(), getAutoScaleMetricsCommand.getPublicIP())).thenThrow(NumberFormatException.class);

        final Answer answer = wrapper.execute(getAutoScaleMetricsCommand, XcpServerResource);
        assertFalse(answer.getResult());
        assertTrue(answer instanceof GetAutoScaleMetricsAnswer);
    }
}
