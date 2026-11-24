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
package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.TakeBackupCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockInfo;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtTakeBackupCommandWrapperTest {

    private LibvirtTakeBackupCommandWrapper wrapper;
    private LibvirtComputingResource libvirtComputingResource;
    private TakeBackupCommand command;

    @Before
    public void setUp() {
        wrapper = new LibvirtTakeBackupCommandWrapper();
        libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
        when(libvirtComputingResource.getNasBackupPath()).thenReturn("nasbackup.sh");
        command = new TakeBackupCommand("i-2-3-VM", "i-2-3-VM/2026.10.04.00.00.00");
        command.setBackupRepoType("nfs");
        command.setBackupRepoAddress("10.0.0.1:/backup");
        command.setWait(60);
    }

    @SuppressWarnings("unchecked")
    private List<String> runAndCaptureArgv() {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, "1024"));

            BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);
            Assert.assertTrue(answer.getResult());

            ArgumentCaptor<List<String[]>> captor = ArgumentCaptor.forClass(List.class);
            scriptMock.verify(() -> Script.executePipedCommands(captor.capture(), anyLong()));
            return Arrays.asList(captor.getValue().get(0));
        }
    }

    @Test
    public void executePassesQuiesceTimeoutWhenQuiescing() {
        command.setQuiesce(true);
        command.setQuiesceTimeout(30);

        List<String> argv = runAndCaptureArgv();

        int idx = argv.indexOf("--quiesce-timeout");
        Assert.assertTrue(idx > 0);
        Assert.assertEquals("30", argv.get(idx + 1));
        Assert.assertEquals("true", argv.get(argv.indexOf("-q") + 1));
    }

    @Test
    public void executeOmitsQuiesceTimeoutWithoutQuiesce() {
        command.setQuiesce(false);
        command.setQuiesceTimeout(30);

        Assert.assertFalse(runAndCaptureArgv().contains("--quiesce-timeout"));
    }

    @Test
    public void executeOmitsQuiesceTimeoutWhenNotPositive() {
        command.setQuiesce(true);
        command.setQuiesceTimeout(0);

        Assert.assertFalse(runAndCaptureArgv().contains("--quiesce-timeout"));
    }

    private void runFailing(Pair<Integer, String> result, boolean expectJobAbort) {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong())).thenReturn(result);
            scriptMock.when(() -> Script.getExecutableAbsolutePath("virsh")).thenReturn("/usr/bin/virsh");

            BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);
            Assert.assertFalse(answer.getResult());

            if (expectJobAbort) {
                scriptMock.verify(() -> Script.executeCommandForExitValue("/usr/bin/virsh", "-c", "qemu:///system",
                        "domjobabort", "i-2-3-VM"));
            } else {
                scriptMock.verify(() -> Script.executeCommandForExitValue(anyString(), anyString(), anyString(),
                        anyString(), anyString()), never());
            }
        }
    }

    private static final String LINSTOR_DEVICE = "/dev/drbd/by-res/cs-vol-1/0";

    /** A stopped VM with one volume on a LINSTOR pool; its device exists here as given. */
    private KVMStoragePoolManager mockStoppedVmOnLinstor(boolean deviceExists) {
        PrimaryDataStoreTO pool = Mockito.mock(PrimaryDataStoreTO.class);
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.lenient().when(pool.getUuid()).thenReturn("pool-uuid");
        command.setVolumePools(Collections.singletonList(pool));
        command.setVolumePaths(Collections.singletonList(LINSTOR_DEVICE));
        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        wrapper = Mockito.spy(wrapper);
        Mockito.doReturn(deviceExists).when(wrapper).deviceExists(LINSTOR_DEVICE);
        return storagePoolMgr;
    }

    @Test
    public void executeConnectsLinstorVolumeMissingOnThisHost() {
        KVMStoragePoolManager storagePoolMgr = mockStoppedVmOnLinstor(false);

        List<String> argv = runAndCaptureArgv();

        Assert.assertEquals(LINSTOR_DEVICE, argv.get(argv.indexOf("-d") + 1));
        InOrder inOrder = Mockito.inOrder(storagePoolMgr);
        inOrder.verify(storagePoolMgr).connectPhysicalDisk(Storage.StoragePoolType.Linstor, "pool-uuid", "vol-1", null);
        inOrder.verify(storagePoolMgr).disconnectPhysicalDisk(Storage.StoragePoolType.Linstor, "pool-uuid", "vol-1");
    }

    @Test
    public void executeLeavesLinstorVolumeWithDeviceOnThisHostAlone() {
        KVMStoragePoolManager storagePoolMgr = mockStoppedVmOnLinstor(true);

        runAndCaptureArgv();

        Mockito.verify(storagePoolMgr, never()).connectPhysicalDisk(Mockito.any(Storage.StoragePoolType.class),
                anyString(), anyString(), Mockito.any());
        Mockito.verify(storagePoolMgr, never()).disconnectPhysicalDisk(Mockito.any(Storage.StoragePoolType.class),
                anyString(), anyString());
    }

    @Test
    public void executeDisconnectsConnectedVolumeWhenBackupFails() {
        KVMStoragePoolManager storagePoolMgr = mockStoppedVmOnLinstor(false);

        runFailing(new Pair<>(1, "qemu-img convert failed"), false);

        Mockito.verify(storagePoolMgr).disconnectPhysicalDisk(Storage.StoragePoolType.Linstor, "pool-uuid", "vol-1");
    }

    @Test
    public void executeFailsWhenLinstorVolumeCannotBeConnected() {
        KVMStoragePoolManager storagePoolMgr = mockStoppedVmOnLinstor(false);
        when(storagePoolMgr.connectPhysicalDisk(Storage.StoragePoolType.Linstor, "pool-uuid", "vol-1", null))
                .thenThrow(new CloudRuntimeException("resource not found"));

        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("resource not found"));
            scriptMock.verify(() -> Script.executePipedCommands(anyList(), anyLong()), never());
        }
        Mockito.verify(storagePoolMgr, never()).disconnectPhysicalDisk(Mockito.any(Storage.StoragePoolType.class),
                anyString(), anyString());
    }

    /** The VM runs here with disks of the given sizes on a pool that hands out scratch devices. */
    private KVMStoragePool mockRunningVmOnScratchPool(long... diskSizes) throws LibvirtException {
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = Mockito.mock(LibvirtUtilitiesHelper.class);
        Connect conn = Mockito.mock(Connect.class);
        Domain dm = Mockito.mock(Domain.class);
        DomainInfo info = new DomainInfo();
        info.state = DomainInfo.DomainState.VIR_DOMAIN_RUNNING;
        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnectionByVmName("i-2-3-VM")).thenReturn(conn);
        when(libvirtComputingResource.getDomain(conn, "i-2-3-VM")).thenReturn(dm);
        when(dm.getInfo()).thenReturn(info);

        List<DiskDef> disks = new ArrayList<>();
        List<PrimaryDataStoreTO> pools = new ArrayList<>();
        List<String> volumePaths = new ArrayList<>();
        List<String> scratchDevices = new ArrayList<>();
        for (int i = 0; i < diskSizes.length; i++) {
            String label = "vd" + (char) ('a' + i);
            DiskDef disk = Mockito.mock(DiskDef.class);
            when(disk.getDeviceType()).thenReturn(DiskDef.DeviceType.DISK);
            when(disk.getDiskLabel()).thenReturn(label);
            DomainBlockInfo blockInfo = Mockito.mock(DomainBlockInfo.class);
            when(blockInfo.getCapacity()).thenReturn(diskSizes[i]);
            when(dm.blockInfo(label)).thenReturn(blockInfo);
            disks.add(disk);

            PrimaryDataStoreTO pool = Mockito.mock(PrimaryDataStoreTO.class);
            when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.Linstor);
            pools.add(pool);
            volumePaths.add("/dev/drbd/by-res/cs-vol-" + i + "/0");
            scratchDevices.add("/dev/scratch/" + label);
        }
        DiskDef cdrom = Mockito.mock(DiskDef.class);
        when(cdrom.getDeviceType()).thenReturn(DiskDef.DeviceType.CDROM);
        disks.add(cdrom);
        when(libvirtComputingResource.getDisks(conn, "i-2-3-VM")).thenReturn(disks);
        when(pools.get(0).getUuid()).thenReturn("pool-uuid");
        command.setVolumePools(pools);
        command.setVolumePaths(volumePaths);
        wrapper = Mockito.spy(wrapper);
        Mockito.doReturn(true).when(wrapper).deviceExists(anyString());

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool scratchPool = Mockito.mock(KVMStoragePool.class);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(Storage.StoragePoolType.Linstor, "pool-uuid")).thenReturn(scratchPool);
        when(scratchPool.createBackupScratch(Mockito.eq("i-2-3-VM"), anyList()))
                .thenReturn(new Pair<>("cs-bkscratch-1", scratchDevices));
        return scratchPool;
    }

    @Test
    public void executeAbortsPullBackupJobOnTimeout() throws LibvirtException {
        command.setMode("full");
        command.setMethod("content");
        KVMStoragePool scratchPool = mockRunningVmOnScratchPool(1L << 30);

        runFailing(new Pair<>(-1, Script.ERR_TIMEOUT), true);

        Mockito.verify(scratchPool).deleteBackupScratch("cs-bkscratch-1");
    }

    @Test
    public void executeLeavesPullBackupJobToScriptOnFailure() throws LibvirtException {
        command.setMode("full");
        command.setMethod("content");
        KVMStoragePool scratchPool = mockRunningVmOnScratchPool(1L << 30);

        runFailing(new Pair<>(1, "qemu-img convert failed"), false);

        Mockito.verify(scratchPool).deleteBackupScratch("cs-bkscratch-1");
    }

    @Test
    public void executeFailsWhenScratchCannotBeCreated() throws LibvirtException {
        command.setMode("full");
        command.setMethod("content");
        KVMStoragePool scratchPool = mockRunningVmOnScratchPool(1L << 30);
        when(scratchPool.createBackupScratch(Mockito.eq("i-2-3-VM"), anyList()))
                .thenThrow(new CloudRuntimeException("no storage pool"));

        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("no storage pool"));
            scriptMock.verify(() -> Script.executePipedCommands(anyList(), anyLong()), never());
        }
        Mockito.verify(scratchPool, never()).deleteBackupScratch(anyString());
    }

    @Test
    public void executeKeepsBackupWhenScratchCannotBeDeleted() throws LibvirtException {
        command.setMode("full");
        command.setMethod("content");
        KVMStoragePool scratchPool = mockRunningVmOnScratchPool(1L << 30);
        Mockito.doThrow(new CloudRuntimeException("in use")).when(scratchPool).deleteBackupScratch("cs-bkscratch-1");

        Assert.assertTrue(runAndCaptureArgv().contains("--scratch-devs"));
    }

    @Test
    public void executeTakesStoppedVmBackupWithoutScratch() throws LibvirtException {
        command.setMode("full");
        command.setMethod("content");
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = Mockito.mock(LibvirtUtilitiesHelper.class);
        Connect conn = Mockito.mock(Connect.class);
        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnectionByVmName("i-2-3-VM")).thenReturn(conn);
        when(libvirtComputingResource.getDomain(conn, "i-2-3-VM")).thenThrow(Mockito.mock(LibvirtException.class));

        Assert.assertFalse(runAndCaptureArgv().contains("--scratch-devs"));
    }

    @Test
    public void scratchSizeHoldsAFullyAllocatedQcow2() {
        // qemu-img measure -O qcow2 --size <n>, "fully allocated size"
        Assert.assertTrue(LibvirtTakeBackupCommandWrapper.scratchSize(540909568L) >= 541327360L);
        Assert.assertTrue(LibvirtTakeBackupCommandWrapper.scratchSize(5371715584L) >= 5372837888L);
        Assert.assertTrue(LibvirtTakeBackupCommandWrapper.scratchSize(1099511627776L) >= 1099679662080L);
    }

    @Test
    public void executeDoesNotAbortPushBackupJobOnTimeout() {
        command.setMode("legacy-full");

        runFailing(new Pair<>(-1, Script.ERR_TIMEOUT), false);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executePassesContentMethodToScript() throws LibvirtException {
        command.setMode("incremental");
        command.setMethod("content");
        command.setParentPaths(Arrays.asList("i-2-3-VM/2026.10.03.00.00.00/root.vol-0.qcow2",
                "i-2-3-VM/2026.10.03.00.00.00/datadisk.vol-1.qcow2"));
        KVMStoragePool scratchPool = mockRunningVmOnScratchPool(1L << 30, 5L << 30);

        List<String> argv = runAndCaptureArgv();

        Assert.assertEquals("incremental", argv.get(argv.indexOf("-M") + 1));
        Assert.assertEquals("content", argv.get(argv.indexOf("--method") + 1));
        Assert.assertFalse(argv.contains("--bitmap-new"));
        // One scratch device per disk, in disk order, each large enough for the whole disk.
        Assert.assertEquals("/dev/scratch/vda,/dev/scratch/vdb", argv.get(argv.indexOf("--scratch-devs") + 1));
        ArgumentCaptor<List<Long>> sizes = ArgumentCaptor.forClass(List.class);
        Mockito.verify(scratchPool).createBackupScratch(Mockito.eq("i-2-3-VM"), sizes.capture());
        Assert.assertEquals(Arrays.asList(LibvirtTakeBackupCommandWrapper.scratchSize(1L << 30),
                LibvirtTakeBackupCommandWrapper.scratchSize(5L << 30)), sizes.getValue());
        Mockito.verify(scratchPool).deleteBackupScratch("cs-bkscratch-1");
    }

    @Test
    public void executeOmitsScratchDevicesForPushMode() {
        command.setMode("legacy-full");

        Assert.assertFalse(runAndCaptureArgv().contains("--scratch-devs"));
    }

    @Test
    public void executeRejectsContentIncrementalWithoutParentPaths() {
        command.setMode("incremental");
        command.setMethod("content");

        BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("content incremental mode requires parentPaths", answer.getDetails());
    }

    @Test
    public void executeRejectsLegacyFullWithContentMethod() {
        command.setMode("legacy-full");
        command.setMethod("content");

        BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
    }
}
