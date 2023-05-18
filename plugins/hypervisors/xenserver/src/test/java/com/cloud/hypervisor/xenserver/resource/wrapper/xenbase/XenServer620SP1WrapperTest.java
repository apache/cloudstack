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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetGPUStatsCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.hypervisor.xenserver.resource.XenServer620SP1Resource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;

@RunWith(PowerMockRunner.class)
public class XenServer620SP1WrapperTest {

    @Mock
    private XenServer620SP1Resource xenServer620SP1Resource;

    @Test
    public void testGetGPUStatsCommand() {
        final String guuid = "246a5b75-05ed-4bbc-a171-2d1fe94a1b0e";

        final Connection conn = Mockito.mock(Connection.class);

        final GetGPUStatsCommand gpuStats = new GetGPUStatsCommand(guuid, "xen");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer620SP1Resource.getConnection()).thenReturn(conn);
        try {
            when(xenServer620SP1Resource.getGPUGroupDetails(conn)).thenReturn(new HashMap<String, HashMap<String, VgpuTypesInfo>>());
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(gpuStats, xenServer620SP1Resource);
        verify(xenServer620SP1Resource, times(1)).getConnection();
        try {
            verify(xenServer620SP1Resource, times(1)).getGPUGroupDetails(conn);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetGPUStatsCommandFailure() {
        final String guuid = "246a5b75-05ed-4bbc-a171-2d1fe94a1b0e";

        final Connection conn = Mockito.mock(Connection.class);

        final GetGPUStatsCommand gpuStats = new GetGPUStatsCommand(guuid, "xen");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer620SP1Resource.getConnection()).thenReturn(conn);
        try {
            when(xenServer620SP1Resource.getGPUGroupDetails(conn)).thenThrow(new CloudRuntimeException("Failed!"));
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(gpuStats, xenServer620SP1Resource);
        verify(xenServer620SP1Resource, times(1)).getConnection();
        try {
            verify(xenServer620SP1Resource, times(1)).getGPUGroupDetails(conn);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertFalse(answer.getResult());
    }
}
