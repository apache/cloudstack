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
import com.cloud.agent.api.PostMigrationAnswer;
import com.cloud.agent.api.PostMigrationCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
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
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class LibvirtPostMigrationCommandWrapperTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Mock
    private PostMigrationCommand postMigrationCommand;

    @Mock
    private VirtualMachineTO virtualMachineTO;

    @Mock
    private Connect connect;

    private LibvirtPostMigrationCommandWrapper wrapper;

    private static final String VM_NAME = "test-vm";

    @Before
    public void setUp() {
        wrapper = new LibvirtPostMigrationCommandWrapper();
        when(postMigrationCommand.getVmName()).thenReturn(VM_NAME);
        when(postMigrationCommand.getVirtualMachine()).thenReturn(virtualMachineTO);
    }

    @Test
    public void testExecute_NoClvmVolumes_Success() throws LibvirtException {
        List<DiskDef> disks = createNonClvmDisks();

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            Assert.assertTrue(answer instanceof PostMigrationAnswer);
        }
    }

    @Test
    public void testExecute_ClvmVolumes_ConvertedToExclusiveMode() throws LibvirtException {
        List<DiskDef> disks = createClvmDisks();

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class);
             MockedStatic<LibvirtComputingResource> mockedResource = Mockito.mockStatic(LibvirtComputingResource.class)) {

            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            mockedResource.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            ), times(1));
        }
    }

    @Test
    public void testExecute_ClvmNgVolumes_ConvertedToExclusiveMode() throws LibvirtException {
        List<DiskDef> disks = createClvmNgDisks();

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class);
             MockedStatic<LibvirtComputingResource> mockedResource = Mockito.mockStatic(LibvirtComputingResource.class)) {

            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            mockedResource.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            ), times(1));
        }
    }

    @Test
    public void testExecute_MixedVolumes_OnlyClvmConverted() throws LibvirtException {
        List<DiskDef> disks = createMixedDisks();

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class);
             MockedStatic<LibvirtComputingResource> mockedResource = Mockito.mockStatic(LibvirtComputingResource.class)) {

            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            mockedResource.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            ), times(1));
        }
    }

    @Test
    public void testExecute_LibvirtException_ReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = Mockito.mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("Connection failed");

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME))
                    .thenThrow(libvirtException);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Connection failed"));
        }
    }

    @Test
    public void testExecute_RuntimeException_ReturnsFailure() throws LibvirtException {
        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME))
                    .thenThrow(new RuntimeException("Unexpected error"));

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Unexpected error"));
        }
    }

    @Test
    public void testExecute_NullVmName_ReturnsFailure() {
        when(postMigrationCommand.getVmName()).thenReturn(null);

        Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("VM or VM name is null"));
    }

    @Test
    public void testExecute_NullVirtualMachine_ReturnsFailure() {
        when(postMigrationCommand.getVirtualMachine()).thenReturn(null);

        Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("VM or VM name is null"));
    }

    @Test
    public void testExecute_EmptyDiskList_Success() throws LibvirtException {
        List<DiskDef> disks = new ArrayList<>();

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
        }
    }

    @Test
    public void testExecute_MultipleClvmVolumes_AllConverted() throws LibvirtException {
        List<DiskDef> disks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            DiskDef disk = Mockito.mock(DiskDef.class);
            Mockito.lenient().when(disk.getDiskPath()).thenReturn("/dev/clvm-vg/volume" + i);
            disks.add(disk);
        }

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class);
             MockedStatic<LibvirtComputingResource> mockedResource = Mockito.mockStatic(LibvirtComputingResource.class)) {

            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            mockedResource.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            ), times(1));
        }
    }

    @Test
    public void testExecute_ClvmAndClvmNgMixed_BothConverted() throws LibvirtException {
        List<DiskDef> disks = new ArrayList<>();

        DiskDef clvmDisk1 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(clvmDisk1.getDiskPath()).thenReturn("/dev/clvm-vg/volume1");
        disks.add(clvmDisk1);

        DiskDef clvmNgDisk = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(clvmNgDisk.getDiskPath()).thenReturn("/dev/clvmng-vg/volume2");
        disks.add(clvmNgDisk);

        DiskDef clvmDisk2 = Mockito.mock(DiskDef.class);
        Mockito.lenient().when(clvmDisk2.getDiskPath()).thenReturn("/dev/clvm-vg/volume3");
        disks.add(clvmDisk2);

        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class);
             MockedStatic<LibvirtComputingResource> mockedResource = Mockito.mockStatic(LibvirtComputingResource.class)) {

            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            mockedResource.when(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    any(),
                    any(),
                    any(),
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
                    eq(libvirtComputingResource),
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            ), times(1));
        }
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
