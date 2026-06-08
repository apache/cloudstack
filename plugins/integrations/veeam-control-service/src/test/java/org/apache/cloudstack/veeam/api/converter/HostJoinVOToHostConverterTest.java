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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.Host;
import org.junit.Test;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.host.Status;
import com.cloud.resource.ResourceState;

public class HostJoinVOToHostConverterTest {

    @Test
    public void testToHost_MapsUpHostAndCoreFields() {
        final HostJoinVO vo = mock(HostJoinVO.class);
        when(vo.getUuid()).thenReturn("host-1");
        when(vo.getName()).thenReturn("kvm-1");
        when(vo.getPrivateIpAddress()).thenReturn("10.10.10.11");
        when(vo.isInMaintenanceStates()).thenReturn(false);
        when(vo.getStatus()).thenReturn(Status.Up);
        when(vo.getResourceState()).thenReturn(ResourceState.Enabled);
        when(vo.getClusterUuid()).thenReturn("cl-1");
        when(vo.getSpeed()).thenReturn(2400L);
        when(vo.getCpuSockets()).thenReturn(2);
        when(vo.getCpus()).thenReturn(8);
        when(vo.getTotalMemory()).thenReturn(16000L);
        when(vo.getMemUsedCapacity()).thenReturn(4000L);

        final Host host = HostJoinVOToHostConverter.toHost(vo);

        assertEquals("host-1", host.getId());
        assertEquals("kvm-1", host.getName());
        assertEquals("10.10.10.11", host.getAddress());
        assertEquals("up", host.getStatus());
        assertEquals("cl-1", host.getCluster().getId());
        assertEquals("2400", host.getCpu().getSpeed());
        assertEquals("16000", host.getMemory());
        assertEquals("12000", host.getMaxSchedulingMemory());
        assertTrue(host.getHref().contains("/api/hosts/host-1"));
    }

    @Test
    public void testToHost_MapsMaintenanceAndFallbackName() {
        final HostJoinVO vo = mock(HostJoinVO.class);
        when(vo.getUuid()).thenReturn("host-2");
        when(vo.getName()).thenReturn(null);
        when(vo.getPrivateIpAddress()).thenReturn("10.10.10.12");
        when(vo.isInMaintenanceStates()).thenReturn(true);
        when(vo.getStatus()).thenReturn(Status.Down);
        when(vo.getResourceState()).thenReturn(ResourceState.Disabled);
        when(vo.getClusterUuid()).thenReturn("cl-2");
        when(vo.getSpeed()).thenReturn(2200L);
        when(vo.getCpuSockets()).thenReturn(1);
        when(vo.getCpus()).thenReturn(4);
        when(vo.getTotalMemory()).thenReturn(8000L);
        when(vo.getMemUsedCapacity()).thenReturn(2000L);

        final Host host = HostJoinVOToHostConverter.toHost(vo);

        assertEquals("host-host-2", host.getName());
        assertEquals("maintenance", host.getStatus());
    }

    @Test
    public void testToHostList_ConvertsAllEntries() {
        final HostJoinVO first = mock(HostJoinVO.class);
        when(first.getUuid()).thenReturn("h1");
        when(first.getName()).thenReturn("h1");
        when(first.getPrivateIpAddress()).thenReturn("1.1.1.1");
        when(first.isInMaintenanceStates()).thenReturn(false);
        when(first.getStatus()).thenReturn(Status.Up);
        when(first.getResourceState()).thenReturn(ResourceState.Enabled);
        when(first.getClusterUuid()).thenReturn("c1");
        when(first.getSpeed()).thenReturn(1000L);
        when(first.getCpuSockets()).thenReturn(1);
        when(first.getCpus()).thenReturn(1);
        when(first.getTotalMemory()).thenReturn(1024L);
        when(first.getMemUsedCapacity()).thenReturn(24L);

        final HostJoinVO second = mock(HostJoinVO.class);
        when(second.getUuid()).thenReturn("h2");
        when(second.getName()).thenReturn("h2");
        when(second.getPrivateIpAddress()).thenReturn("2.2.2.2");
        when(second.isInMaintenanceStates()).thenReturn(false);
        when(second.getStatus()).thenReturn(Status.Down);
        when(second.getResourceState()).thenReturn(ResourceState.Disabled);
        when(second.getClusterUuid()).thenReturn("c2");
        when(second.getSpeed()).thenReturn(2000L);
        when(second.getCpuSockets()).thenReturn(1);
        when(second.getCpus()).thenReturn(2);
        when(second.getTotalMemory()).thenReturn(2048L);
        when(second.getMemUsedCapacity()).thenReturn(48L);

        final List<Host> hosts = HostJoinVOToHostConverter.toHostList(List.of(first, second));

        assertEquals(2, hosts.size());
        assertEquals("h1", hosts.get(0).getId());
        assertEquals("h2", hosts.get(1).getId());
    }
}
