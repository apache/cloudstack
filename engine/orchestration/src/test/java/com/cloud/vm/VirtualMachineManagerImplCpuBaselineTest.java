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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerImplCpuBaselineTest {

    private final VirtualMachineManagerImpl vmm = Mockito.spy(new VirtualMachineManagerImpl());

    private DeployDestination destInCluster(long clusterId) {
        Host host = Mockito.mock(Host.class);
        Mockito.when(host.getClusterId()).thenReturn(clusterId);
        DeployDestination dest = Mockito.mock(DeployDestination.class);
        Mockito.when(dest.getHost()).thenReturn(host);
        return dest;
    }

    private VMInstanceVO kvmVm() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return vm;
    }

    @Test
    public void pinsBaselineModelWhenClusterHasOne() {
        Mockito.doReturn("Haswell-noTSX").when(vmm).getClusterCpuBaselineModel(5L);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        Mockito.when(vmTO.getDetails()).thenReturn(null);

        vmm.applyClusterCpuBaseline(vmTO, kvmVm(), destInCluster(5L));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(vmTO).setDetails(captor.capture());
        Assert.assertEquals("custom", captor.getValue().get(VmDetailConstants.GUEST_CPU_MODE));
        Assert.assertEquals("Haswell-noTSX", captor.getValue().get(VmDetailConstants.GUEST_CPU_MODEL));
        Assert.assertEquals("forbid", captor.getValue().get(VmDetailConstants.GUEST_CPU_MODEL_FALLBACK));
    }

    @Test
    public void doesNothingWhenClusterBaselineIsBlank() {
        Mockito.doReturn("").when(vmm).getClusterCpuBaselineModel(5L);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);

        vmm.applyClusterCpuBaseline(vmTO, kvmVm(), destInCluster(5L));

        Mockito.verify(vmTO, Mockito.never()).setDetails(Mockito.anyMap());
    }

    @Test
    public void doesNotOverrideAnExplicitVmCpuModel() {
        Mockito.doReturn("Haswell-noTSX").when(vmm).getClusterCpuBaselineModel(5L);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        Map<String, String> explicit = new HashMap<>();
        explicit.put(VmDetailConstants.GUEST_CPU_MODE, "host-passthrough");
        explicit.put(VmDetailConstants.GUEST_CPU_MODEL, "EPYC");
        Mockito.when(vmTO.getDetails()).thenReturn(explicit);

        vmm.applyClusterCpuBaseline(vmTO, kvmVm(), destInCluster(5L));

        Mockito.verify(vmTO, Mockito.never()).setDetails(Mockito.anyMap());
    }

    @Test
    public void ignoresNonKvmWithoutReadingClusterBaseline() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);

        vmm.applyClusterCpuBaseline(vmTO, vm, Mockito.mock(DeployDestination.class));

        Mockito.verify(vmm, Mockito.never()).getClusterCpuBaselineModel(Mockito.anyLong());
        Mockito.verify(vmTO, Mockito.never()).setDetails(Mockito.anyMap());
    }

    @Test
    public void ignoresSystemVmWithoutReadingClusterBaseline() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);

        vmm.applyClusterCpuBaseline(vmTO, vm, Mockito.mock(DeployDestination.class));

        Mockito.verify(vmm, Mockito.never()).getClusterCpuBaselineModel(Mockito.anyLong());
        Mockito.verify(vmTO, Mockito.never()).setDetails(Mockito.anyMap());
    }
}
