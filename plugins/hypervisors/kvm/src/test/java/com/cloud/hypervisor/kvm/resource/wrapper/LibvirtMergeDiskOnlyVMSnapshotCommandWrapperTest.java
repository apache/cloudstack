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
package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.junit.Before;
import org.junit.Test;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockJobInfo;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.SnapshotMergeTreeTO;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.vm.VirtualMachine;

public class LibvirtMergeDiskOnlyVMSnapshotCommandWrapperTest {

    private static final String VM_NAME = "i-2-10-VM";

    private QemuImg qemuImg;
    private LibvirtMergeDiskOnlyVMSnapshotCommandWrapper wrapper;
    private LibvirtComputingResource resource;
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    private KVMStoragePoolManager storagePoolManager;
    private KVMStoragePool storagePool;
    private Domain domain;
    private FakeBlockJobs fakeBlockJobs;
    private Path poolDirectory;

    @Before
    public void setup() throws Exception {
        qemuImg = mock(QemuImg.class);
        wrapper = new LibvirtMergeDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected QemuImg getQemuImg(long timeoutInMillis) {
                return qemuImg;
            }

            @Override
            protected long getPollIntervalInMillis() {
                return 10;
            }
        };

        resource = mock(LibvirtComputingResource.class);
        libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        storagePoolManager = mock(KVMStoragePoolManager.class);
        storagePool = mock(KVMStoragePool.class);
        domain = mock(Domain.class);
        Connect connect = mock(Connect.class);
        poolDirectory = Files.createTempDirectory("merge-pool-");
        fakeBlockJobs = new FakeBlockJobs(poolDirectory);

        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDomain(connect, VM_NAME)).thenReturn(domain);
        when(resource.getDisks(connect, VM_NAME)).thenReturn(new ArrayList<>());
        when(resource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        when(resource.getQcow2DeltaMergeTimeout()).thenReturn(259200);
        when(resource.getCmdsTimeout()).thenReturn(7200000);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(any(), any())).thenReturn(storagePool);
        when(storagePoolManager.getStoragePool(any(), anyString())).thenReturn(storagePool);
        when(storagePool.getLocalPathFor(anyString())).thenAnswer(invocation -> poolDirectory.resolve(invocation.getArgument(0, String.class)).toString());
        when(domain.getName()).thenReturn(VM_NAME);
        fakeBlockJobs.attachTo(domain);
    }

    @Test
    public void testGetMergeTimeoutInSecondsLeavesAMarginBeforeTheCommandWait() {
        assertEquals(259200, wrapper.getMergeTimeoutInSeconds(commandWithWait(0), 259200));
        assertEquals(15, wrapper.getMergeTimeoutInSeconds(commandWithWait(20), 259200));
        assertEquals(270, wrapper.getMergeTimeoutInSeconds(commandWithWait(300), 259200));
        assertEquals(1740, wrapper.getMergeTimeoutInSeconds(commandWithWait(1800), 259200));
        assertEquals(1, wrapper.getMergeTimeoutInSeconds(commandWithWait(3), 259200));
        assertEquals(100, wrapper.getMergeTimeoutInSeconds(commandWithWait(1800), 100));
    }

    @Test
    public void testGetDiskChainsReadsActiveImageBackingChainAndMirror() throws Exception {
        fakeBlockJobs.addDisk("vda", "top-a", "snap-a", "base-a");
        fakeBlockJobs.disk("vda").job = "active-commit";
        fakeBlockJobs.disk("vda").ready = true;

        LibvirtMergeDiskOnlyVMSnapshotCommandWrapper.DiskChain diskChain = wrapper.getDiskChains(domain).get("vda");

        assertNotNull(diskChain);
        assertEquals(fakeBlockJobs.path("top-a"), diskChain.activeSource);
        assertEquals(List.of(fakeBlockJobs.path("snap-a"), fakeBlockJobs.path("base-a")), diskChain.backingFiles);
        assertTrue(diskChain.isActiveCommitReady());
        assertTrue(diskChain.contains(fakeBlockJobs.path("snap-a")));
        assertFalse(diskChain.contains(fakeBlockJobs.path("other")));
    }

    @Test
    public void testRunningVmMergePivotsDisksOnlyOnceEveryDiskIsReady() throws Exception {
        fakeBlockJobs.addDisk("vda", "top-a", "snap-a", "base-a");
        fakeBlockJobs.addDisk("vdb", "top-b", "snap-b", "base-b");
        fakeBlockJobs.disk("vdb").pollsUntilFinished = 3;
        MergeDiskOnlyVmSnapshotCommand command = runningCommand(1800, activeTree("vol-a", "vda", "top-a", "snap-a"), activeTree("vol-b", "vdb", "top-b", "snap-b"));

        Answer answer = wrapper.execute(command, resource);

        assertTrue(answer.getDetails(), answer.getResult());
        assertEquals(List.of("vol-a", "vol-b"), ((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids());
        assertFalse("A disk was pivoted while another one was not ready yet.", fakeBlockJobs.pivotedBeforeEveryDiskWasReady);
        assertEquals(fakeBlockJobs.path("snap-a"), fakeBlockJobs.disk("vda").chain.get(0));
        assertEquals(fakeBlockJobs.path("snap-b"), fakeBlockJobs.disk("vdb").chain.get(0));
        assertFalse(Files.exists(Path.of(fakeBlockJobs.path("top-a"))));
        assertFalse(Files.exists(Path.of(fakeBlockJobs.path("top-b"))));
        verify(domain, never()).blockJobAbort(anyString(), eq(0));
    }

    @Test
    public void testRunningVmMergeCancelsEveryDiskWhenOneIsNotReadyInTime() throws Exception {
        fakeBlockJobs.addDisk("vda", "top-a", "snap-a", "base-a");
        fakeBlockJobs.addDisk("vdb", "top-b", "snap-b", "base-b");
        fakeBlockJobs.disk("vdb").pollsUntilFinished = Integer.MAX_VALUE;
        MergeDiskOnlyVmSnapshotCommand command = runningCommand(6, activeTree("vol-a", "vda", "top-a", "snap-a"), activeTree("vol-b", "vdb", "top-b", "snap-b"));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertTrue(((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids().isEmpty());
        verify(domain, never()).blockJobAbort(anyString(), eq(Domain.BlockJobAbortFlags.PIVOT));
        verify(domain).blockJobAbort("vda", 0);
        verify(domain).blockJobAbort("vdb", 0);
        assertEquals(List.of(fakeBlockJobs.path("top-a"), fakeBlockJobs.path("snap-a"), fakeBlockJobs.path("base-a")), fakeBlockJobs.disk("vda").chain);
        assertEquals(List.of(fakeBlockJobs.path("top-b"), fakeBlockJobs.path("snap-b"), fakeBlockJobs.path("base-b")), fakeBlockJobs.disk("vdb").chain);
        assertTrue(Files.exists(Path.of(fakeBlockJobs.path("top-a"))));
        assertTrue(Files.exists(Path.of(fakeBlockJobs.path("top-b"))));
    }

    @Test
    public void testRunningVmMergeReportsPivotedDisksWhenALaterPivotFails() throws Exception {
        fakeBlockJobs.addDisk("vda", "top-a", "snap-a", "base-a");
        fakeBlockJobs.addDisk("vdb", "top-b", "snap-b", "base-b");
        fakeBlockJobs.disk("vdb").pivotFails = true;
        MergeDiskOnlyVmSnapshotCommand command = runningCommand(1800, activeTree("vol-a", "vda", "top-a", "snap-a"), activeTree("vol-b", "vdb", "top-b", "snap-b"));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertEquals(List.of("vol-a"), ((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids());
        verify(domain).blockJobAbort("vdb", 0);
        assertFalse(Files.exists(Path.of(fakeBlockJobs.path("top-a"))));
        assertTrue(Files.exists(Path.of(fakeBlockJobs.path("top-b"))));
        assertEquals(fakeBlockJobs.path("top-b"), fakeBlockJobs.disk("vdb").chain.get(0));
    }

    @Test
    public void testRunningVmMergeReportsOnlyTheNonActiveCommitsThatCompleted() throws Exception {
        fakeBlockJobs.addDisk("vda", "top-a", "child-a", "parent-a", "base-a");
        fakeBlockJobs.addDisk("vdb", "top-b", "child-b", "parent-b", "base-b");
        fakeBlockJobs.disk("vdb").commitFails = true;
        Files.createFile(Path.of(fakeBlockJobs.path("grandchild-a")));
        MergeDiskOnlyVmSnapshotCommand command = runningCommand(1800, nonActiveTree("vol-a", "vda", "child-a", "parent-a", "grandchild-a"),
                nonActiveTree("vol-b", "vdb", "child-b", "parent-b"));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertEquals(List.of("vol-a"), ((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids());
        verify(domain, never()).blockJobAbort(anyString(), eq(Domain.BlockJobAbortFlags.PIVOT));
        verify(qemuImg).rebase(argThat(file -> file.getFileName().equals(fakeBlockJobs.path("grandchild-a"))),
                argThat(file -> file.getFileName().equals(fakeBlockJobs.path("parent-a"))), anyString(), eq(false));
        assertFalse(Files.exists(Path.of(fakeBlockJobs.path("child-a"))));
        assertTrue(Files.exists(Path.of(fakeBlockJobs.path("child-b"))));
        assertEquals(List.of(fakeBlockJobs.path("top-b"), fakeBlockJobs.path("child-b"), fakeBlockJobs.path("parent-b"), fakeBlockJobs.path("base-b")),
                fakeBlockJobs.disk("vdb").chain);
    }

    @Test
    public void testStoppedVmMergeDoesNotChangeAnyChainWhenACommitFails() throws Exception {
        Files.createFile(poolDirectory.resolve("child-a"));
        Files.createFile(poolDirectory.resolve("child-b"));
        doThrow(new QemuImgException("commit failed")).when(qemuImg).commit(argThat(file -> file.getFileName().endsWith("child-b")), any(), eq(true));
        MergeDiskOnlyVmSnapshotCommand command = stoppedCommand(stoppedTree("vol-a", "child-a", "parent-a", "grandchild-a"),
                stoppedTree("vol-b", "child-b", "parent-b"));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertTrue(((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids().isEmpty());
        verify(qemuImg, never()).rebase(any(QemuImgFile.class), any(QemuImgFile.class), anyString(), eq(false));
        assertTrue(Files.exists(poolDirectory.resolve("child-a")));
        assertTrue(Files.exists(poolDirectory.resolve("child-b")));
    }

    @Test
    public void testStoppedVmMergeChangesTheChainsOnlyAfterEveryCommitSucceeded() throws Exception {
        Files.createFile(poolDirectory.resolve("child-a"));
        Files.createFile(poolDirectory.resolve("child-b"));
        MergeDiskOnlyVmSnapshotCommand command = stoppedCommand(stoppedTree("vol-a", "child-a", "parent-a", "grandchild-a"),
                stoppedTree("vol-b", "child-b", "parent-b"));

        Answer answer = wrapper.execute(command, resource);

        assertTrue(answer.getDetails(), answer.getResult());
        assertEquals(List.of("vol-a", "vol-b"), ((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids());
        verify(qemuImg).rebase(argThat(file -> file.getFileName().endsWith("grandchild-a")), argThat(file -> file.getFileName().endsWith("parent-a")),
                anyString(), eq(false));
        assertFalse(Files.exists(poolDirectory.resolve("child-a")));
        assertFalse(Files.exists(poolDirectory.resolve("child-b")));
    }

    @Test
    public void testStoppedVmMergeAbortsWithoutCommittingOnceTheDeadlineIsReached() throws Exception {
        MergeDiskOnlyVmSnapshotCommand command = stoppedCommand(stoppedTree("vol-a", "child-a", "parent-a"), stoppedTree("vol-b", "child-b", "parent-b"));
        command.setWait(6);
        doAnswer(invocation -> {
            Thread.sleep(1100);
            return null;
        }).when(qemuImg).commit(argThat(file -> file.getFileName().endsWith("child-a")), any(), eq(true));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertTrue(((MergeDiskOnlyVmSnapshotAnswer) answer).getMergedVolumeUuids().isEmpty());
        verify(qemuImg, never()).commit(argThat(file -> file.getFileName().endsWith("child-b")), any(), eq(true));
    }

    private MergeDiskOnlyVmSnapshotCommand commandWithWait(int wait) {
        MergeDiskOnlyVmSnapshotCommand command = new MergeDiskOnlyVmSnapshotCommand(new ArrayList<>(), VirtualMachine.State.Running, VM_NAME);
        command.setWait(wait);
        return command;
    }

    private MergeDiskOnlyVmSnapshotCommand runningCommand(int wait, SnapshotMergeTreeTO... trees) {
        MergeDiskOnlyVmSnapshotCommand command = new MergeDiskOnlyVmSnapshotCommand(List.of(trees), VirtualMachine.State.Running, VM_NAME);
        command.setWait(wait);
        return command;
    }

    private MergeDiskOnlyVmSnapshotCommand stoppedCommand(SnapshotMergeTreeTO... trees) {
        MergeDiskOnlyVmSnapshotCommand command = new MergeDiskOnlyVmSnapshotCommand(List.of(trees), VirtualMachine.State.Stopped, VM_NAME);
        command.setWait(1800);
        return command;
    }

    private SnapshotMergeTreeTO activeTree(String volumeUuid, String label, String topPath, String basePath) {
        VolumeObjectTO volume = volume(volumeUuid, label, topPath);
        when(volume.getObjectType()).thenReturn(DataObjectType.VOLUME);
        return new SnapshotMergeTreeTO(snapshot(basePath, volume), volume, new ArrayList<>());
    }

    private SnapshotMergeTreeTO nonActiveTree(String volumeUuid, String label, String topPath, String basePath, String... grandChildren) {
        VolumeObjectTO volume = volume(volumeUuid, label, "top-" + label);
        SnapshotObjectTO child = snapshot(topPath, volume);
        when(child.getObjectType()).thenReturn(DataObjectType.SNAPSHOT);
        return new SnapshotMergeTreeTO(snapshot(basePath, volume), child, dataTos(volume, grandChildren));
    }

    private SnapshotMergeTreeTO stoppedTree(String volumeUuid, String childPath, String parentPath, String... grandChildren) {
        VolumeObjectTO volume = volume(volumeUuid, null, childPath);
        SnapshotObjectTO parent = snapshot(parentPath, volume);
        PrimaryDataStoreTO dataStore = mock(PrimaryDataStoreTO.class);
        when(dataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(dataStore.getUuid()).thenReturn("pool");
        when(parent.getDataStore()).thenReturn(dataStore);
        return new SnapshotMergeTreeTO(parent, snapshot(childPath, volume), dataTos(volume, grandChildren));
    }

    private List<DataTO> dataTos(VolumeObjectTO volume, String... paths) {
        List<DataTO> dataTos = new ArrayList<>();
        for (String path : paths) {
            dataTos.add(snapshot(path, volume));
        }
        return dataTos;
    }

    private VolumeObjectTO volume(String uuid, String label, String path) {
        VolumeObjectTO volume = mock(VolumeObjectTO.class);
        when(volume.getUuid()).thenReturn(uuid);
        when(volume.getPath()).thenReturn(path);
        if (label != null) {
            LibvirtVMDef.DiskDef diskDef = mock(LibvirtVMDef.DiskDef.class);
            when(diskDef.getDiskLabel()).thenReturn(label);
            when(resource.getDiskWithPathOfVolumeObjectTO(any(), eq(volume))).thenReturn(diskDef);
        }
        return volume;
    }

    private SnapshotObjectTO snapshot(String path, VolumeObjectTO volume) {
        SnapshotObjectTO snapshot = mock(SnapshotObjectTO.class);
        when(snapshot.getPath()).thenReturn(path);
        when(snapshot.getVolume()).thenReturn(volume);
        return snapshot;
    }

    /**
     * Imitates how Libvirt handles block commits: active commits stay ready until pivoted or cancelled, non-active commits finish on their own, and
     * cancelling any of them leaves the backing chain unchanged.
     */
    private static class FakeBlockJobs {
        private final Path poolDirectory;
        private final Map<String, FakeDisk> disks = new LinkedHashMap<>();
        private boolean pivotedBeforeEveryDiskWasReady;

        private FakeBlockJobs(Path poolDirectory) {
            this.poolDirectory = poolDirectory;
        }

        private String path(String name) {
            return poolDirectory.resolve(name).toString();
        }

        private void addDisk(String label, String... chain) throws Exception {
            FakeDisk disk = new FakeDisk();
            for (String name : chain) {
                disk.chain.add(path(name));
                Files.createFile(Path.of(path(name)));
            }
            disks.put(label, disk);
        }

        private FakeDisk disk(String label) {
            return disks.get(label);
        }

        private void attachTo(Domain domain) throws LibvirtException {
            when(domain.getXMLDesc(0)).thenAnswer(invocation -> {
                disks.values().forEach(FakeDisk::advance);
                return xml();
            });
            when(domain.getBlockJobInfo(anyString(), eq(0))).thenAnswer(invocation -> {
                DomainBlockJobInfo blockJobInfo = new DomainBlockJobInfo();
                FakeDisk disk = disks.get(invocation.getArgument(0, String.class));
                if (disk != null && disk.job != null) {
                    blockJobInfo.type = 3;
                    blockJobInfo.end = 100;
                    blockJobInfo.cur = disk.ready ? 100 : 50;
                }
                return blockJobInfo;
            });
            doAnswer(invocation -> {
                FakeDisk disk = disks.get(invocation.getArgument(0, String.class));
                disk.base = invocation.getArgument(1, String.class);
                disk.top = invocation.getArgument(2, String.class);
                disk.job = (invocation.getArgument(4, Integer.class) & Domain.BlockCommitFlags.ACTIVE) != 0 ? "active-commit" : "commit";
                return null;
            }).when(domain).blockCommit(anyString(), anyString(), anyString(), anyLong(), anyInt());
            doAnswer(invocation -> {
                FakeDisk disk = disks.get(invocation.getArgument(0, String.class));
                int flags = invocation.getArgument(1, Integer.class);
                if (flags == Domain.BlockJobAbortFlags.PIVOT) {
                    if (disk.pivotFails || !disk.ready) {
                        throw mock(LibvirtException.class);
                    }
                    pivotedBeforeEveryDiskWasReady |= disks.values().stream().anyMatch(other -> other.job != null && !other.ready);
                    disk.chain.subList(0, disk.chain.indexOf(disk.base)).clear();
                }
                disk.job = null;
                disk.ready = false;
                return null;
            }).when(domain).blockJobAbort(anyString(), anyInt());
        }

        private String xml() {
            StringBuilder xml = new StringBuilder("<domain><name>" + VM_NAME + "</name><devices>");
            for (Map.Entry<String, FakeDisk> entry : disks.entrySet()) {
                FakeDisk disk = entry.getValue();
                xml.append("<disk type='file' device='disk'><source file='").append(disk.chain.get(0)).append("'/>");
                for (int i = 1; i < disk.chain.size(); i++) {
                    xml.append("<backingStore type='file'><source file='").append(disk.chain.get(i)).append("'/>");
                }
                xml.append("<backingStore/>");
                for (int i = 1; i < disk.chain.size(); i++) {
                    xml.append("</backingStore>");
                }
                if ("active-commit".equals(disk.job)) {
                    xml.append("<mirror type='file' job='active-commit'").append(disk.ready ? " ready='yes'" : "").append("><source file='")
                            .append(disk.base).append("'/></mirror>");
                }
                xml.append("<target dev='").append(entry.getKey()).append("' bus='virtio'/></disk>");
            }
            return xml.append("</devices></domain>").toString();
        }
    }

    private static class FakeDisk {
        private final List<String> chain = new ArrayList<>();
        private String job;
        private boolean ready;
        private String base;
        private String top;
        private int pollsUntilFinished;
        private boolean pivotFails;
        private boolean commitFails;

        private void advance() {
            if (job == null || ready) {
                return;
            }
            if (pollsUntilFinished > 0) {
                if (pollsUntilFinished != Integer.MAX_VALUE) {
                    pollsUntilFinished--;
                }
                return;
            }
            if ("active-commit".equals(job)) {
                ready = true;
                return;
            }
            if (!commitFails) {
                chain.remove(top);
            }
            job = null;
        }
    }
}
