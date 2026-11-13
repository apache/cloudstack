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
package com.cloud.vm;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckCpuCompatibilityCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerImplCpuCompatTest {

    @Mock
    private HostDao _hostDao;

    @Mock
    private AgentManager _agentMgr;

    @InjectMocks
    private VirtualMachineManagerImpl vmm = new VirtualMachineManagerImpl();

    private HostVO kvmHostUp(long id, String name) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(id);
        Mockito.when(host.getName()).thenReturn(name);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return host;
    }

    @Test
    public void blankModelReturnsEmptyWithoutQueryingHosts() {
        List<String> result = vmm.findHostsIncompatibleWithCpuModel(1L, "  ");
        Assert.assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(_hostDao);
    }

    @Test
    public void reportsHostsThatFailTheCompatibilityCheck() throws Exception {
        HostVO good = kvmHostUp(10L, "host-good");
        HostVO bad = kvmHostUp(11L, "host-bad");
        Mockito.when(_hostDao.findByClusterId(2L, com.cloud.host.Host.Type.Routing)).thenReturn(Arrays.asList(good, bad));
        Mockito.when(_agentMgr.send(Mockito.eq(10L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new Answer(null, true, "compatible"));
        Mockito.when(_agentMgr.send(Mockito.eq(11L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new Answer(null, false, "incompatible"));

        List<String> result = vmm.findHostsIncompatibleWithCpuModel(2L, "Haswell-noTSX");

        Assert.assertEquals(Arrays.asList("host-bad"), result);
    }

    @Test
    public void allCompatibleReturnsEmpty() throws Exception {
        HostVO good = kvmHostUp(10L, "host-good");
        Mockito.when(_hostDao.findByClusterId(2L, com.cloud.host.Host.Type.Routing)).thenReturn(Arrays.asList(good));
        Mockito.when(_agentMgr.send(Mockito.eq(10L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new Answer(null, true, "compatible"));

        Assert.assertTrue(vmm.findHostsIncompatibleWithCpuModel(2L, "Haswell-noTSX").isEmpty());
    }

    @Test
    public void unknownModelFailsClosed() throws Exception {
        // virsh reports an unknown model as an error with result=true (fail-open); setting a baseline must still
        // reject it, so a non-existent model name is never accepted as a cluster baseline.
        HostVO host = kvmHostUp(10L, "host-a");
        Mockito.when(_hostDao.findByClusterId(2L, com.cloud.host.Host.Type.Routing)).thenReturn(Arrays.asList(host));
        Mockito.when(_agentMgr.send(Mockito.eq(10L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new Answer(null, true, "error: internal error: Unknown CPU model NotARealModel"));

        Assert.assertEquals(Arrays.asList("host-a"), vmm.findHostsIncompatibleWithCpuModel(2L, "NotARealModel"));
    }

    @Test
    public void preFeatureAgentUnsupportedAnswerIsSkipped() throws Exception {
        // an old agent that cannot handle the command returns an UnsupportedAnswer; that is "cannot verify",
        // not "incompatible", so the host is skipped rather than blocking the baseline during a rolling upgrade.
        HostVO host = kvmHostUp(10L, "host-a");
        Mockito.when(_hostDao.findByClusterId(2L, com.cloud.host.Host.Type.Routing)).thenReturn(Arrays.asList(host));
        Mockito.when(_agentMgr.send(Mockito.eq(10L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new com.cloud.agent.api.UnsupportedAnswer(null, "unsupported command"));

        Assert.assertTrue(vmm.findHostsIncompatibleWithCpuModel(2L, "Haswell-noTSX").isEmpty());
    }

    @Test
    public void reachableHostWithConnectionErrorIsSkipped() throws Exception {
        // a reachable host whose libvirtd was momentarily down (virsh "failed to connect") cannot be verified
        // and must be skipped, not reported incompatible, so one transient failure does not block the baseline.
        HostVO host = kvmHostUp(10L, "host-a");
        Mockito.when(_hostDao.findByClusterId(2L, com.cloud.host.Host.Type.Routing)).thenReturn(Arrays.asList(host));
        Mockito.when(_agentMgr.send(Mockito.eq(10L), Mockito.any(CheckCpuCompatibilityCommand.class)))
                .thenReturn(new Answer(null, true, "error: failed to connect to the hypervisor"));

        Assert.assertTrue(vmm.findHostsIncompatibleWithCpuModel(2L, "Haswell-noTSX").isEmpty());
    }
}
