//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PreMigrationCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtPreMigrationCommandWrapperTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Mock
    private PreMigrationCommand preMigrationCommand;

    @Mock
    private VirtualMachineTO virtualMachineTO;

    @Mock
    private Connect connect;

    @Mock
    private Domain domain;

    @Mock
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;

    private LibvirtPreMigrationCommandWrapper wrapper;

    private static final String VM_NAME = "test-vm";

    @Before
    public void setUp() {
        wrapper = new LibvirtPreMigrationCommandWrapper();
        when(preMigrationCommand.getVmName()).thenReturn(VM_NAME);
        when(preMigrationCommand.getVirtualMachine()).thenReturn(virtualMachineTO);
        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
    }

    @Test
    public void testExecute_NoClvmVolumes_Success() throws LibvirtException {
        List<DiskDef> disks = createNonClvmDisks();
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

        Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertEquals("Source host prepared for migration", answer.getDetails());
        verify(domain, times(1)).free();
    }

    @Test
    public void testExecute_ClvmVolumes_ConvertedToSharedMode() throws LibvirtException {
        List<DiskDef> disks = createClvmDisks();
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

        try (MockedStatic<LibvirtComputingResource> mockedStatic = Mockito.mockStatic(LibvirtComputingResource.class)) {
            mockedStatic.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.SHARED)
            )).then(invocation -> null);

            Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedStatic.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.SHARED)
            ), times(1));
            verify(domain, times(1)).free();
        }
    }

    @Test
    public void testExecute_ClvmNgVolumes_ConvertedToSharedMode() throws LibvirtException {
        List<DiskDef> disks = createClvmNgDisks();
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

        try (MockedStatic<LibvirtComputingResource> mockedStatic = Mockito.mockStatic(LibvirtComputingResource.class)) {
            mockedStatic.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedStatic.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.SHARED)
            ), times(1));
        }
    }

    @Test
    public void testExecute_MixedVolumes_OnlyClvmConverted() throws LibvirtException {
        List<DiskDef> disks = createMixedDisks();
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

        try (MockedStatic<LibvirtComputingResource> mockedStatic = Mockito.mockStatic(LibvirtComputingResource.class)) {
            mockedStatic.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedStatic.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.SHARED)
            ), times(1));
        }
    }

    @Test
    public void testExecute_LibvirtException_ReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = Mockito.mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("Connection failed");
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenThrow(libvirtException);

        Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Failed to prepare source host"));
        Assert.assertTrue(answer.getDetails().contains("Connection failed"));
    }

    @Test
    public void testExecute_DomainFreeFails_StillReturnsSuccess() throws LibvirtException {
        List<DiskDef> disks = createNonClvmDisks();
        LibvirtException libvirtException = Mockito.mock(LibvirtException.class);
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);
        Mockito.doThrow(libvirtException).when(domain).free();

        Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testExecute_NullVmName_ReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = Mockito.mock(LibvirtException.class);
        when(preMigrationCommand.getVmName()).thenReturn(null);
        when(libvirtUtilitiesHelper.getConnectionByVmName(null)).thenThrow(libvirtException);

        Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void testExecute_EmptyDiskList_Success() throws LibvirtException {
        List<DiskDef> disks = new ArrayList<>();
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(connect);
        when(connect.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

        Answer answer = wrapper.execute(preMigrationCommand, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        verify(domain, times(1)).free();
    }

    private List<DiskDef> createNonClvmDisks() {
        List<DiskDef> disks = new ArrayList<>();

        DiskDef disk1 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk1.getDiskPath()).thenReturn("/mnt/nfs/volume1.qcow2");
        disks.add(disk1);

        DiskDef disk2 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk2.getDiskPath()).thenReturn("/mnt/nfs/volume2.qcow2");
        disks.add(disk2);

        return disks;
    }

    private List<DiskDef> createClvmDisks() {
        List<DiskDef> disks = new ArrayList<>();

        DiskDef disk1 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk1.getDiskPath()).thenReturn("/dev/clvm-vg/volume1");
        disks.add(disk1);

        DiskDef disk2 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk2.getDiskPath()).thenReturn("/dev/clvm-vg/volume2");
        disks.add(disk2);

        return disks;
    }

    private List<DiskDef> createClvmNgDisks() {
        List<DiskDef> disks = new ArrayList<>();

        DiskDef disk1 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk1.getDiskPath()).thenReturn("/dev/clvmng-vg/volume1");
        disks.add(disk1);

        DiskDef disk2 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(disk2.getDiskPath()).thenReturn("/dev/clvmng-vg/volume2");
        disks.add(disk2);

        return disks;
    }

    private List<DiskDef> createMixedDisks() {
        List<DiskDef> disks = new ArrayList<>();

        DiskDef clvmDisk = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(clvmDisk.getDiskPath()).thenReturn("/dev/clvm-vg/volume1");
        disks.add(clvmDisk);

        DiskDef nfsDisk = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(nfsDisk.getDiskPath()).thenReturn("/mnt/nfs/volume2.qcow2");
        disks.add(nfsDisk);

        DiskDef clvmNgDisk = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(clvmNgDisk.getDiskPath()).thenReturn("/dev/clvmng-vg/volume3");
        disks.add(clvmNgDisk);

        return disks;
    }
}
