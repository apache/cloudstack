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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.SetupCommand;
import com.cloud.host.Host;
import com.cloud.host.HostEnvironment;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.utils.ExecutionResult;
import com.cloud.vm.VMInstanceVO;
import com.xensource.xenapi.Connection;

@RunWith(PowerMockRunner.class)
public class XenServer56WrapperTest {

    @Mock
    private XenServer56Resource xenServer56Resource;

    @Test
    public void testCheckOnHostCommand() {
        final com.cloud.host.Host host = Mockito.mock(com.cloud.host.Host.class);
        final CheckOnHostCommand onHostCommand = new CheckOnHostCommand(host);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(onHostCommand, xenServer56Resource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testFenceCommand() {
        final VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        final Host host = Mockito.mock(Host.class);

        final Connection conn = Mockito.mock(Connection.class);

        final FenceCommand fenceCommand = new FenceCommand(vm, host);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer56Resource.getConnection()).thenReturn(conn);

        final Answer answer = wrapper.execute(fenceCommand, xenServer56Resource);

        verify(xenServer56Resource, times(1)).getConnection();
        verify(xenServer56Resource, times(1)).checkHeartbeat(fenceCommand.getHostGuid());

        assertFalse(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandSuccess() {
        final Connection conn = Mockito.mock(Connection.class);

        final NetworkUsageCommand networkCommand = new NetworkUsageCommand("192.168.10.10", "domRName", false, "192.168.10.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer56Resource.getConnection()).thenReturn(conn);
        when(xenServer56Resource.getNetworkStats(conn, networkCommand.getPrivateIP())).thenReturn(new long[]{1, 1});

        final Answer answer = wrapper.execute(networkCommand, xenServer56Resource);

        verify(xenServer56Resource, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandFailure() {
        final Connection conn = Mockito.mock(Connection.class);

        final NetworkUsageCommand networkCommand = new NetworkUsageCommand("192.168.10.10", "domRName", false, "192.168.10.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer56Resource.getConnection()).thenReturn(conn);
        when(xenServer56Resource.getNetworkStats(conn, networkCommand.getPrivateIP())).thenReturn(new long[0]);

        final Answer answer = wrapper.execute(networkCommand, xenServer56Resource);

        verify(xenServer56Resource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandCreateVpc() {
        final ExecutionResult executionResult = Mockito.mock(ExecutionResult.class);

        final NetworkUsageCommand networkCommand = new NetworkUsageCommand("192.168.10.10", "domRName", true, "192.168.10.1", "10.1.1.1/24");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final String args = " -l 192.168.10.1 -c -v 10.1.1.1/24";
        when(xenServer56Resource.executeInVR(networkCommand.getPrivateIP(), "vpc_netusage.sh", args)).thenReturn(executionResult);
        when(executionResult.isSuccess()).thenReturn(true);

        final Answer answer = wrapper.execute(networkCommand, xenServer56Resource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testNetworkUsageCommandCreateVpcFailure() {
        final ExecutionResult executionResult = Mockito.mock(ExecutionResult.class);

        final NetworkUsageCommand networkCommand = new NetworkUsageCommand("192.168.10.10", "domRName", true, "192.168.10.1", "10.1.1.1/24");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final String args = " -l 192.168.10.1 -c -v 10.1.1.1/24";
        when(xenServer56Resource.executeInVR(networkCommand.getPrivateIP(), "vpc_netusage.sh", args)).thenReturn(executionResult);
        when(executionResult.isSuccess()).thenReturn(false);

        final Answer answer = wrapper.execute(networkCommand, xenServer56Resource);

        assertFalse(answer.getResult());
    }

    @Test
    public void testSetupCommand() {
        final XsHost xsHost = Mockito.mock(XsHost.class);
        final HostEnvironment env = Mockito.mock(HostEnvironment.class);

        final SetupCommand setupCommand = new SetupCommand(env);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer56Resource.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(setupCommand, xenServer56Resource);
        verify(xenServer56Resource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }
}