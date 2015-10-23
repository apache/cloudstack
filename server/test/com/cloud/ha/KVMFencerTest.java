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
package com.cloud.ha;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class KVMFencerTest {

    @Mock
    HostDao hostDao;
    @Mock
    AgentManager agentManager;
    @Mock
    AlertManager alertMgr;
    @Mock
    ResourceManager resourceManager;

    KVMFencer fencer;

    @Before
    public void setup() {
        fencer = new KVMFencer();
        fencer._agentMgr = agentManager;
        fencer._alertMgr = alertMgr;
        fencer._hostDao = hostDao;
        fencer._resourceMgr = resourceManager;
    }

    @Test
    public void testWithSingleHost() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getId()).thenReturn(1l);
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithSingleHostDown() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getStatus()).thenReturn(Status.Down);
        Mockito.when(host.getId()).thenReturn(1l);
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithHosts() throws AgentUnavailableException, OperationTimedoutException {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(Status.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        FenceAnswer answer = new FenceAnswer(null, true, "ok");
        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenReturn(answer);

        Assert.assertTrue(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithFailingFence() throws AgentUnavailableException, OperationTimedoutException {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(Status.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenThrow(new AgentUnavailableException(2l));

        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithTimeoutingFence() throws AgentUnavailableException, OperationTimedoutException {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(Status.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenThrow(new OperationTimedoutException(null, 2l, 0l, 0, false));

        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithSingleNotKVM() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.Any);
        Mockito.when(host.getStatus()).thenReturn(Status.Down);
        Mockito.when(host.getId()).thenReturn(1l);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertNull(fencer.fenceOff(virtualMachine, host));
    }

}
