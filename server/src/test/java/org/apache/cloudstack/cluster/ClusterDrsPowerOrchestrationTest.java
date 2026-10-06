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
package org.apache.cloudstack.cluster;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.HostVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsPowerOrchestrationTest {

    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private OutOfBandManagementService outOfBandManagementService;

    @InjectMocks
    private ClusterDrsServiceImpl drs = new ClusterDrsServiceImpl();

    private VMInstanceVO vm(long id, VirtualMachine.Type type, VirtualMachine.State state) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getType()).thenReturn(type);
        Mockito.lenient().when(vm.getState()).thenReturn(state);
        return vm;
    }

    private HostVO host(long id) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        return host;
    }

    @Test
    public void hostEvacuatableTrueForRunningUserVmsOnly() {
        VMInstanceVO a = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO b = vm(2L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(10L)).thenReturn(Arrays.asList(a, b));
        Assert.assertTrue(drs.hostEvacuatable(host(10L)));
    }

    @Test
    public void hostEvacuatableFalseWhenASystemVmIsPresent() {
        VMInstanceVO user = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO router = vm(3L, VirtualMachine.Type.DomainRouter, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(11L)).thenReturn(Arrays.asList(user, router));
        Assert.assertFalse(drs.hostEvacuatable(host(11L)));
    }

    @Test
    public void hostEvacuatableFalseWhenAVmIsNotRunning() {
        VMInstanceVO stopping = vm(4L, VirtualMachine.Type.User, VirtualMachine.State.Stopping);
        Mockito.when(vmInstanceDao.listByHostId(12L)).thenReturn(Collections.singletonList(stopping));
        Assert.assertFalse(drs.hostEvacuatable(host(12L)));
    }

    @Test
    public void hostEvacuatableFalseWhenEmpty() {
        Mockito.when(vmInstanceDao.listByHostId(13L)).thenReturn(Collections.emptyList());
        Assert.assertFalse(drs.hostEvacuatable(host(13L)));
    }

    @Test
    public void vmResourceNeedZeroWhenOfferingMissing() {
        VMInstanceVO vm = vm(5L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.eq(5L), Mockito.anyLong())).thenReturn(null);
        Assert.assertEquals(0d, drs.vmResourceNeed(vm, true), 0.0001d);
    }

    @Test
    public void vmResourceNeedComputesCpuAndMemory() {
        VMInstanceVO vm = vm(6L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(99L);
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getCpu()).thenReturn(4);
        Mockito.when(offering.getSpeed()).thenReturn(2000);
        Mockito.when(offering.getRamSize()).thenReturn(2048);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(6L, 99L)).thenReturn(offering);

        Assert.assertEquals(8000d, drs.vmResourceNeed(vm, true), 0.0001d);
        Assert.assertEquals(2048d * 1024L * 1024L, drs.vmResourceNeed(vm, false), 0.0001d);
    }

    @Test
    public void isPowerManageableReflectsOobmEnabled() {
        HostVO h = host(20L);
        Mockito.when(outOfBandManagementService.isOutOfBandManagementEnabled(h)).thenReturn(true);
        Assert.assertTrue(drs.isPowerManageable(h));
    }

    @Test
    public void isPowerManageableFalseWhenOobmLookupThrows() {
        HostVO h = host(21L);
        Mockito.when(outOfBandManagementService.isOutOfBandManagementEnabled(h)).thenThrow(new RuntimeException("boom"));
        Assert.assertFalse(drs.isPowerManageable(h));
    }
}
