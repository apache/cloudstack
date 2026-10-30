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

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerImplMigrationIpTest {

    private static final long SRC = 7L;
    private static final long DST = 5L;

    @Mock
    private HostDetailsDao hostDetailsDao;

    @InjectMocks
    private VirtualMachineManagerImpl virtualMachineManagerImpl = new VirtualMachineManagerImpl();

    private VMInstanceVO kvmVm() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(vm.getHostId()).thenReturn(SRC);
        return vm;
    }

    private Host hostWithId(long id) {
        Host host = Mockito.mock(Host.class);
        when(host.getId()).thenReturn(id);
        return host;
    }

    private void stubMigrationIp(long hostId, String value) {
        DetailVO detail = Mockito.mock(DetailVO.class);
        when(detail.getValue()).thenReturn(value);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_MIGRATION_IP)).thenReturn(detail);
    }

    @Test
    public void usesDedicatedIpWhenBothHostsHaveMigrationNetwork() {
        stubMigrationIp(SRC, "10.0.7.7");
        stubMigrationIp(DST, "192.0.2.17");
        Assert.assertEquals("192.0.2.17", virtualMachineManagerImpl.resolveMigrationIp(kvmVm(), hostWithId(DST)));
    }

    @Test
    public void returnsNullWhenSourceHasNoMigrationNetwork() {
        when(hostDetailsDao.findDetail(SRC, Host.HOST_MIGRATION_IP)).thenReturn(null);
        Assert.assertNull(virtualMachineManagerImpl.resolveMigrationIp(kvmVm(), hostWithId(DST)));
    }

    @Test
    public void returnsNullWhenDestinationHasNoMigrationNetwork() {
        stubMigrationIp(SRC, "10.0.7.7");
        when(hostDetailsDao.findDetail(DST, Host.HOST_MIGRATION_IP)).thenReturn(null);
        Assert.assertNull(virtualMachineManagerImpl.resolveMigrationIp(kvmVm(), hostWithId(DST)));
    }

    @Test
    public void returnsNullWhenDestinationMigrationIpBlank() {
        stubMigrationIp(SRC, "10.0.7.7");
        stubMigrationIp(DST, "   ");
        Assert.assertNull(virtualMachineManagerImpl.resolveMigrationIp(kvmVm(), hostWithId(DST)));
    }

    @Test
    public void returnsNullForNonKvmWithoutQueryingHostDetails() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);

        Assert.assertNull(virtualMachineManagerImpl.resolveMigrationIp(vm, Mockito.mock(Host.class)));
        Mockito.verify(hostDetailsDao, Mockito.never()).findDetail(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void preflightChecksSourceWhenResolvedIpBlankAndSourceUsesMigrationNetwork() {
        stubMigrationIp(SRC, "10.0.7.7");

        // resolved IP is blank (destination lacks a migration network): preflight checks only the source,
        // reusing the caller's resolved IP rather than re-resolving (no destination lookup).
        virtualMachineManagerImpl.preflightMigrationNetwork(kvmVm(), hostWithId(DST), "");

        Mockito.verify(hostDetailsDao, Mockito.times(1)).findDetail(SRC, Host.HOST_MIGRATION_IP);
        Mockito.verify(hostDetailsDao, Mockito.never()).findDetail(DST, Host.HOST_MIGRATION_IP);
    }

    @Test
    public void preflightSkipsWhenResolvedIpPresent() {
        virtualMachineManagerImpl.preflightMigrationNetwork(kvmVm(), hostWithId(DST), "192.0.2.17");
        Mockito.verify(hostDetailsDao, Mockito.never()).findDetail(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void preflightSkipsNonKvmWithoutQueryingHostDetails() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);

        virtualMachineManagerImpl.preflightMigrationNetwork(vm, Mockito.mock(Host.class), "");

        Mockito.verify(hostDetailsDao, Mockito.never()).findDetail(Mockito.anyLong(), Mockito.anyString());
    }
}
