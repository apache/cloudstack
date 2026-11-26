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
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.network.Networks;
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
    public void testExecute_VlanNic_AppliesManualVlanTrunkMembership() throws LibvirtException, com.cloud.exception.InternalErrorException {
        NicTO vlanNic = new NicTO();
        vlanNic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        vlanNic.setMac("02:00:00:00:00:01");
        when(virtualMachineTO.getNics()).thenReturn(new NicTO[]{vlanNic});

        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        when(libvirtComputingResource.getInterface(connect, VM_NAME, vlanNic.getMac())).thenReturn(liveInterface);
        when(libvirtComputingResource.getVifDriver(vlanNic.getType(), vlanNic.getName())).thenReturn(vifDriver);

        List<DiskDef> disks = createNonClvmDisks();
        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            Mockito.verify(vifDriver).ensureVlanTrunkMembership(liveInterface, vlanNic);
        }
    }

    private NicTO vlanNicWithMac(String mac, boolean enabled) {
        NicTO vlanNic = new NicTO();
        vlanNic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        vlanNic.setMac(mac);
        vlanNic.setEnabled(enabled);
        return vlanNic;
    }

    private Answer executeWithVlanNic(NicTO vlanNic, LibvirtVMDef.InterfaceDef liveInterface, VifDriver vifDriver, Domain domain) throws LibvirtException {
        when(virtualMachineTO.getNics()).thenReturn(new NicTO[]{vlanNic});
        when(libvirtComputingResource.getInterface(connect, VM_NAME, vlanNic.getMac())).thenReturn(liveInterface);
        when(libvirtComputingResource.getVifDriver(vlanNic.getType(), vlanNic.getName())).thenReturn(vifDriver);
        Mockito.lenient().when(libvirtComputingResource.getDomain(connect, VM_NAME)).thenReturn(domain);
        List<DiskDef> disks = createNonClvmDisks();
        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);
            return wrapper.execute(postMigrationCommand, libvirtComputingResource);
        }
    }

    @Test
    public void testExecute_VlanMembershipFails_SetsNicLinkDownAndFails() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:01", true);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        Domain domain = Mockito.mock(Domain.class);
        Mockito.doThrow(new com.cloud.exception.InternalErrorException("bridge vlan failed")).when(vifDriver).ensureVlanTrunkMembership(liveInterface, vlanNic);

        Answer answer = executeWithVlanNic(vlanNic, liveInterface, vifDriver, domain);

        Assert.assertFalse(answer.getResult());
        Mockito.verify(liveInterface).setLinkStateUp(false);
        Mockito.verify(domain).updateDeviceFlags(any(), eq(Domain.DeviceModifyFlags.LIVE));
    }

    @Test
    public void testExecute_VlanMembershipSucceedsAfterFailure_RestoresNicLink() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:02", true);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        Domain domain = Mockito.mock(Domain.class);
        Mockito.doThrow(new com.cloud.exception.InternalErrorException("bridge vlan failed")).doNothing().when(vifDriver).ensureVlanTrunkMembership(liveInterface, vlanNic);

        Assert.assertFalse(executeWithVlanNic(vlanNic, liveInterface, vifDriver, domain).getResult());
        Assert.assertTrue(executeWithVlanNic(vlanNic, liveInterface, vifDriver, domain).getResult());

        Mockito.verify(liveInterface).setLinkStateUp(false);
        Mockito.verify(liveInterface).setLinkStateUp(true);
    }

    @Test
    public void testExecute_FirstVlanNicFails_StillProcessesAndCutsOffTheOtherNics() throws Exception {
        NicTO failingNic = vlanNicWithMac("02:00:00:00:0f:05", true);
        NicTO secondFailingNic = vlanNicWithMac("02:00:00:00:0f:06", true);
        NicTO healthyNic = vlanNicWithMac("02:00:00:00:0f:07", true);
        LibvirtVMDef.InterfaceDef failingLive = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        LibvirtVMDef.InterfaceDef secondFailingLive = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        LibvirtVMDef.InterfaceDef healthyLive = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        Domain domain = Mockito.mock(Domain.class);
        Mockito.when(healthyLive.isLinkStateUp()).thenReturn(true);
        Mockito.doThrow(new com.cloud.exception.InternalErrorException("first failed")).when(vifDriver).ensureVlanTrunkMembership(failingLive, failingNic);
        Mockito.doThrow(new com.cloud.exception.InternalErrorException("second failed")).when(vifDriver).ensureVlanTrunkMembership(secondFailingLive, secondFailingNic);
        when(virtualMachineTO.getNics()).thenReturn(new NicTO[]{failingNic, healthyNic, secondFailingNic});
        when(libvirtComputingResource.getInterface(connect, VM_NAME, failingNic.getMac())).thenReturn(failingLive);
        when(libvirtComputingResource.getInterface(connect, VM_NAME, healthyNic.getMac())).thenReturn(healthyLive);
        when(libvirtComputingResource.getInterface(connect, VM_NAME, secondFailingNic.getMac())).thenReturn(secondFailingLive);
        when(libvirtComputingResource.getVifDriver(any(), any())).thenReturn(vifDriver);
        when(libvirtComputingResource.getDomain(connect, VM_NAME)).thenReturn(domain);
        List<DiskDef> disks = createNonClvmDisks();
        Answer answer;
        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);
            answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);
        }

        Assert.assertFalse(answer.getResult());
        Mockito.verify(vifDriver).ensureVlanTrunkMembership(healthyLive, healthyNic);
        Mockito.verify(failingLive).setLinkStateUp(false);
        Mockito.verify(secondFailingLive).setLinkStateUp(false);
        Mockito.verify(healthyLive, Mockito.never()).setLinkStateUp(Mockito.anyBoolean());
    }

    @Test
    public void testExecute_VlanMembershipSucceeds_DoesNotTouchNicLink() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:03", true);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        Domain domain = Mockito.mock(Domain.class);
        Mockito.when(liveInterface.isLinkStateUp()).thenReturn(true);

        Assert.assertTrue(executeWithVlanNic(vlanNic, liveInterface, Mockito.mock(VifDriver.class), domain).getResult());

        Mockito.verify(liveInterface, Mockito.never()).setLinkStateUp(Mockito.anyBoolean());
    }

    @Test
    public void testExecute_VlanMembershipSucceedsAndLinkWasCutOffByAnEarlierHost_RestoresNicLink() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:08", true);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        Mockito.when(liveInterface.isLinkStateUp()).thenReturn(false);

        Assert.assertTrue(executeWithVlanNic(vlanNic, liveInterface, Mockito.mock(VifDriver.class), Mockito.mock(Domain.class)).getResult());

        Mockito.verify(liveInterface).setLinkStateUp(true);
    }

    @Test
    public void testExecute_VlanMembershipSucceedsOnDisabledNicWithLinkDown_LeavesLinkAlone() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:09", false);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        Mockito.lenient().when(liveInterface.isLinkStateUp()).thenReturn(false);

        Assert.assertTrue(executeWithVlanNic(vlanNic, liveInterface, Mockito.mock(VifDriver.class), Mockito.mock(Domain.class)).getResult());

        Mockito.verify(liveInterface, Mockito.never()).setLinkStateUp(Mockito.anyBoolean());
    }

    @Test
    public void testExecute_VlanMembershipFailsOnDisabledNic_LeavesLinkAlone() throws Exception {
        NicTO vlanNic = vlanNicWithMac("02:00:00:00:0f:04", false);
        LibvirtVMDef.InterfaceDef liveInterface = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        Mockito.doThrow(new com.cloud.exception.InternalErrorException("bridge vlan failed")).when(vifDriver).ensureVlanTrunkMembership(liveInterface, vlanNic);

        Assert.assertFalse(executeWithVlanNic(vlanNic, liveInterface, vifDriver, Mockito.mock(Domain.class)).getResult());

        Mockito.verify(liveInterface, Mockito.never()).setLinkStateUp(Mockito.anyBoolean());
    }

    @Test
    public void testExecute_NonVlanNic_SkipsManualVlanTrunkMembership() throws LibvirtException {
        NicTO publicNic = new NicTO();
        publicNic.setBroadcastType(Networks.BroadcastDomainType.Native);
        publicNic.setMac("02:00:00:00:00:02");
        when(virtualMachineTO.getNics()).thenReturn(new NicTO[]{publicNic});

        List<DiskDef> disks = createNonClvmDisks();
        try (MockedStatic<LibvirtConnection> mockedConnection = Mockito.mockStatic(LibvirtConnection.class)) {
            mockedConnection.when(() -> LibvirtConnection.getConnectionByVmName(VM_NAME)).thenReturn(connect);
            when(libvirtComputingResource.getDisks(connect, VM_NAME)).thenReturn(disks);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            Mockito.verify(libvirtComputingResource, Mockito.never()).getInterface(any(), any(), any());
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
                    eq(virtualMachineTO),
                    eq(LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE)
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
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
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
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
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
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
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
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
                    any()
            )).then(invocation -> null);

            Answer answer = wrapper.execute(postMigrationCommand, libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            mockedResource.verify(() -> LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                    eq(disks),
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
