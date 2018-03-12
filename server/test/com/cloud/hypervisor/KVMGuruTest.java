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
package com.cloud.hypervisor;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KVMGuruTest {

    @Mock
    HostDao hostDao;

    @Spy
    @InjectMocks
    private KVMGuru guru = new KVMGuru();

    @Mock
    VirtualMachineTO vmTO;
    @Mock
    VirtualMachineProfile vmProfile;
    @Mock
    VirtualMachine vm;
    @Mock
    HostVO host;

    private static final long hostId = 1l;

    @Before
    public void setup() {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(true);
        Mockito.when(vmProfile.getVirtualMachine()).thenReturn(vm);
        Mockito.when(vm.getHostId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(host.getCpus()).thenReturn(3);
        Mockito.when(host.getSpeed()).thenReturn(1995l);
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(500);
    }

    @Test
    public void testSetVmQuotaPercentage() {
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSetVmQuotaPercentageNullHost() {
        Mockito.when(hostDao.findById(hostId)).thenReturn(null);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
    }

    @Test
    public void testSetVmQuotaPercentageZeroDivision() {
        Mockito.when(host.getSpeed()).thenReturn(0l);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageNotCPULimit() {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(false);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmProfile, Mockito.never()).getVirtualMachine();
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageOverProvision() {
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(3000);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(1d);
    }
}