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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Test;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;

public class LibvirtDiskOnlyVMSnapshotCommandWrapperTest {

    @Test
    public void testBackupNvramIfNeededCopiesActiveNvram() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO vmSnapshotTO = mock(VMSnapshotTO.class);
        PrimaryDataStoreTO dataStoreTO = mock(PrimaryDataStoreTO.class);
        VolumeObjectTO rootVolume = mock(VolumeObjectTO.class);

        Path activeNvram = Files.createTempFile("active-", ".fd");
        Files.writeString(activeNvram, "snapshot-nvram");
        Path poolDirectory = Files.createTempDirectory("pool-");

        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.getTarget()).thenReturn(vmSnapshotTO);
        when(vmSnapshotTO.getId()).thenReturn(42L);
        when(command.getVolumeTOs()).thenReturn(List.of(rootVolume));
        when(rootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(rootVolume.getDataStore()).thenReturn(dataStoreTO);
        when(dataStoreTO.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(dataStoreTO.getUuid()).thenReturn("pool-uuid");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(activeNvram.toString());
        when(resource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        when(storagePoolManager.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, "pool-uuid")).thenReturn(storagePool);
        when(storagePool.getLocalPathFor(anyString())).thenAnswer(invocation -> poolDirectory.resolve(invocation.getArgument(0, String.class)).toString());

        String nvramSnapshotPath = wrapper.backupNvramIfNeeded(command, resource);

        assertEquals(".cloudstack-vm-snapshot-nvram/42.fd", nvramSnapshotPath);
        assertEquals("snapshot-nvram", Files.readString(poolDirectory.resolve(nvramSnapshotPath)));
    }

    @Test(expected = IOException.class)
    public void testBackupNvramIfNeededFailsWhenUefiNvramIsMissing() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);

        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn("/tmp/" + UUID.randomUUID() + ".fd");

        wrapper.backupNvramIfNeeded(command, resource);
    }

    @Test(expected = IOException.class)
    public void testValidateNvramRevertStateFailsForLegacySnapshotsOnUefiVms() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);

        Path activeNvram = Files.createTempFile("active-", ".fd");
        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getNvramSnapshotPath()).thenReturn(null);
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(activeNvram.toString());

        wrapper.validateNvramRevertState(command, resource, null, mock(KVMStoragePoolManager.class));
    }

    @Test(expected = IOException.class)
    public void testValidateNvramRevertStateFailsForLegacySnapshotsOnFallbackHostsForUefiVms() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);

        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getNvramSnapshotPath()).thenReturn(null);
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(Path.of("/tmp", UUID.randomUUID() + ".fd").toString());

        wrapper.validateNvramRevertState(command, resource, null, mock(KVMStoragePoolManager.class));
    }

    @Test
    public void testValidateNvramRevertStateAllowsFallbackHostsWithoutLocalNvram() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);
        SnapshotObjectTO rootSnapshot = mock(SnapshotObjectTO.class);

        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(poolDirectory.resolve("missing").resolve("vm-uuid.fd").toString());
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(rootSnapshot, storagePoolManager)).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        wrapper.validateNvramRevertState(command, resource, rootSnapshot, storagePoolManager);
    }

    @Test
    public void testRestoreNvramIfNeededRestoresSnapshotBytes() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);
        SnapshotObjectTO rootSnapshot = mock(SnapshotObjectTO.class);

        Path activeNvram = Files.createTempFile("active-", ".fd");
        Files.writeString(activeNvram, "current");
        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(activeNvram.toString());
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(rootSnapshot, storagePoolManager)).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        wrapper.restoreNvramIfNeeded(command, resource, rootSnapshot, storagePoolManager);

        assertEquals("snapshot", Files.readString(activeNvram));
    }

    @Test
    public void testRestoreNvramIfNeededCreatesMissingActiveNvramFile() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);
        SnapshotObjectTO rootSnapshot = mock(SnapshotObjectTO.class);

        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Path activeNvram = poolDirectory.resolve("target").resolve("vm-uuid.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(activeNvram.toString());
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(rootSnapshot, storagePoolManager)).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        wrapper.restoreNvramIfNeeded(command, resource, rootSnapshot, storagePoolManager);

        assertEquals("snapshot", Files.readString(activeNvram));
    }

    @Test
    public void testRestoreNvramIfNeededPreservesActiveNvramWhenCopyFails() throws Exception {
        LibvirtRevertDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtRevertDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected void copyNvramSnapshotToTemporaryPath(Path snapshotNvramPath, Path temporaryNvramPath) throws IOException {
                Files.writeString(temporaryNvramPath, "partial");
                throw new IOException("copy failed");
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        RevertDiskOnlyVmSnapshotCommand command = mock(RevertDiskOnlyVmSnapshotCommand.class);
        SnapshotObjectTO rootSnapshot = mock(SnapshotObjectTO.class);

        Path activeNvram = Files.createTempFile("active-", ".fd");
        Files.writeString(activeNvram, "current");
        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getVmUuid()).thenReturn("vm-uuid");
        when(command.getVmName()).thenReturn("vm-name");
        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(resource.getUefiNvramPath("vm-uuid")).thenReturn(activeNvram.toString());
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(rootSnapshot, storagePoolManager)).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        try {
            wrapper.restoreNvramIfNeeded(command, resource, rootSnapshot, storagePoolManager);
            fail("Expected restore to fail when the snapshot copy fails.");
        } catch (IOException expected) {
            assertEquals("current", Files.readString(activeNvram));
        }
    }

    @Test
    public void testDeleteNvramSnapshotIfNeededDeletesSidecar() throws Exception {
        LibvirtDeleteDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtDeleteDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        DeleteDiskOnlyVmSnapshotCommand command = mock(DeleteDiskOnlyVmSnapshotCommand.class);
        SnapshotObjectTO rootSnapshot = mock(SnapshotObjectTO.class);
        VolumeObjectTO rootVolume = mock(VolumeObjectTO.class);

        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(rootSnapshot.getVolume()).thenReturn(rootVolume);
        when(rootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(rootSnapshot, storagePoolManager)).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        wrapper.deleteNvramSnapshotIfNeeded(command, resource, storagePoolManager, List.<DataTO>of(rootSnapshot));

        assertFalse(Files.exists(snapshotNvram));
        assertTrue(Files.exists(poolDirectory));
    }

    @Test
    public void testDeleteNvramSnapshotIfNeededDeletesSidecarUsingPrimaryDataStore() throws Exception {
        LibvirtDeleteDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtDeleteDiskOnlyVMSnapshotCommandWrapper();
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        KVMStoragePool storagePool = mock(KVMStoragePool.class);
        DeleteDiskOnlyVmSnapshotCommand command = mock(DeleteDiskOnlyVmSnapshotCommand.class);
        PrimaryDataStoreTO primaryDataStoreTO = mock(PrimaryDataStoreTO.class);

        Path poolDirectory = Files.createTempDirectory("pool-");
        Path snapshotNvram = poolDirectory.resolve("nvram/42.fd");
        Files.createDirectories(snapshotNvram.getParent());
        Files.writeString(snapshotNvram, "snapshot");

        when(command.getNvramSnapshotPath()).thenReturn("nvram/42.fd");
        when(command.getPrimaryDataStore()).thenReturn(primaryDataStoreTO);
        when(primaryDataStoreTO.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(primaryDataStoreTO.getUuid()).thenReturn("pool-uuid");
        when(storagePoolManager.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, "pool-uuid")).thenReturn(storagePool);
        when(storagePool.getLocalPathFor("nvram/42.fd")).thenReturn(snapshotNvram.toString());

        wrapper.deleteNvramSnapshotIfNeeded(command, resource, storagePoolManager, List.of());

        assertFalse(Files.exists(snapshotNvram));
    }

    @Test
    public void testResumeVmIfNeededOnlyResumesWhenWrapperSuspendedVm() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        Domain domain = mock(Domain.class);

        wrapper.resumeVmIfNeeded(domain, "vm-name", false);

        verify(domain, never()).resume();
        verify(domain, never()).getInfo();
    }

    @Test
    public void testSuspendVmIfNeededSkipsAlreadyPausedVm() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        Domain domain = mock(Domain.class);
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainInfo.DomainState.VIR_DOMAIN_PAUSED;
        when(domain.getInfo()).thenReturn(domainInfo);

        assertFalse(wrapper.suspendVmIfNeeded(domain));
        verify(domain, never()).suspend();
    }

    @Test
    public void testShouldSuspendVmForSnapshotWhenUefiAndNotQuiesced() {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getTarget()).thenReturn(target);
        when(target.getQuiescevm()).thenReturn(false);

        assertTrue(wrapper.shouldSuspendVmForSnapshot(command));
    }

    @Test
    public void testShouldSuspendVmForSnapshotWhenQuiesceIsRequested() {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getTarget()).thenReturn(target);
        when(target.getQuiescevm()).thenReturn(true);

        assertTrue(wrapper.shouldSuspendVmForSnapshot(command));
    }

    @Test
    public void testShouldFreezeVmFilesystemsForSnapshotWhenQuiesceIsRequested() {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.isUefiEnabled()).thenReturn(true);
        when(command.getTarget()).thenReturn(target);
        when(target.getQuiescevm()).thenReturn(true);

        assertTrue(wrapper.shouldFreezeVmFilesystemsForSnapshot(command));
    }

    @Test
    public void testGetFlagsToUseForRunningVmSnapshotCreationOmitsLibvirtQuiesceWhenAlreadyFrozen() {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper();
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(target.getQuiescevm()).thenReturn(true);

        int flags = wrapper.getFlagsToUseForRunningVmSnapshotCreation(target, true);

        assertEquals(0, flags & Domain.SnapshotCreateFlags.QUIESCE);
        assertTrue((flags & Domain.SnapshotCreateFlags.DISK_ONLY) != 0);
        assertTrue((flags & Domain.SnapshotCreateFlags.ATOMIC) != 0);
        assertTrue((flags & Domain.SnapshotCreateFlags.NO_METADATA) != 0);
    }

    @Test
    public void testFreezeAndVerifyVmFilesystemsSucceedsWhenGuestAgentReportsFrozen() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected String getResultOfQemuCommand(String cmd, Domain domain) {
                if ("status".equals(cmd)) {
                    return "{\"return\":\"frozen\"}";
                }
                return "{\"return\":0}";
            }
        };

        Domain domain = mock(Domain.class);
        wrapper.freezeVmFilesystems(domain, "vm-name");
        wrapper.verifyVmFilesystemsFrozen(domain, "vm-name");
    }

    @Test
    public void testFreezeVmFilesystemsFailsForQemuErrorResponse() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected String getResultOfQemuCommand(String cmd, Domain domain) {
                return "{\"error\":{\"class\":\"GenericError\",\"desc\":\"guest agent failure\"}}";
            }
        };

        try {
            wrapper.freezeVmFilesystems(mock(Domain.class), "vm-name");
            fail("QEMU guest agent error responses must be treated as freeze failures.");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Failed to freeze VM [vm-name] filesystems"));
        }
    }

    @Test
    public void testThawVmFilesystemsFailsForQemuErrorResponse() {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected String getResultOfQemuCommand(String cmd, Domain domain) {
                return "{\"error\":{\"class\":\"GenericError\",\"desc\":\"guest agent failure\"}}";
            }
        };

        assertFalse(wrapper.thawVmFilesystemsIfNeeded(mock(Domain.class), "vm-name"));
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmThawsWhenFreezeVerificationFails() throws Exception {
        final boolean[] thawCalled = {false};
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return true;
            }

            @Override
            protected void freezeVmFilesystems(Domain domain, String vmName) {
            }

            @Override
            protected void verifyVmFilesystemsFrozen(Domain domain, String vmName) throws IOException {
                throw new IOException("status verification failed");
            }

            @Override
            protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName) {
                thawCalled[0] = true;
                return true;
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertFalse(answer.getResult());
        assertTrue("Thaw must be attempted after a successful freeze followed by verification failure", thawCalled[0]);
    }

    @Test
    public void testVerifyVmFilesystemsFrozenFailsForQemuErrorResponse() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected String getResultOfQemuCommand(String cmd, Domain domain) {
                return "{\"error\":{\"class\":\"GenericError\",\"desc\":\"guest agent failure\"}}";
            }
        };

        try {
            wrapper.verifyVmFilesystemsFrozen(mock(Domain.class), "vm-name");
            fail("QEMU guest agent error responses must be treated as IO failures.");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Failed to verify VM [vm-name] filesystem freeze state"));
        }
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmReturnsFailureAnswerWhenFreezeStatusJsonIsMalformed() throws Exception {
        final boolean[] thawCalled = {false};
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return true;
            }

            @Override
            protected void freezeVmFilesystems(Domain domain, String vmName) {
            }

            @Override
            protected String getResultOfQemuCommand(String cmd, Domain domain) {
                return "not-json";
            }

            @Override
            protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName) {
                thawCalled[0] = true;
                return true;
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("Failed to verify VM [vm-name] filesystem freeze state"));
        assertTrue("Thaw must be attempted when freeze verification fails on malformed JSON", thawCalled[0]);
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmSuspendsBeforeNvramCopyForQuiescedUefiSnapshots() throws Exception {
        List<String> operations = new ArrayList<>();
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected void freezeVmFilesystems(Domain domain, String vmName) {
                operations.add("freeze");
            }

            @Override
            protected void verifyVmFilesystemsFrozen(Domain domain, String vmName) {
                operations.add("verify");
            }

            @Override
            protected boolean suspendVmIfNeeded(Domain domain) {
                operations.add("suspend");
                return true;
            }

            @Override
            protected String backupNvramIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
                operations.add("backup");
                return "nvram/42.fd";
            }

            @Override
            protected void cleanupNvramSnapshotIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource, String nvramSnapshotPath) {
            }

            @Override
            protected boolean resumeVmIfNeeded(Domain domain, String vmName) {
                operations.add("resume");
                return true;
            }

            @Override
            protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName) {
                operations.add("thaw");
                return true;
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(command.isUefiEnabled()).thenReturn(true);
        when(target.getQuiescevm()).thenReturn(true);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);
        doThrow(mock(org.libvirt.LibvirtException.class)).when(domain).snapshotCreateXML(anyString(), anyInt());

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertFalse(answer.getResult());
        assertEquals(List.of("freeze", "verify", "suspend", "backup", "resume", "thaw"), operations);
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmReturnsSuccessWithWarningWhenThawFails() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return true;
            }

            @Override
            protected void freezeVmFilesystems(Domain domain, String vmName) {
            }

            @Override
            protected void verifyVmFilesystemsFrozen(Domain domain, String vmName) {
            }

            @Override
            protected String backupNvramIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
                return null;
            }

            @Override
            protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName) {
                return false;
            }

            @Override
            protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName, boolean frozen) {
                return false;
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertTrue("Snapshot metadata must still be returned when thaw fails after snapshot creation", answer.getResult());
        assertTrue(answer.getDetails().contains("could not be thawed"));
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmReturnsSuccessWithWarningWhenResumeFails() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return true;
            }

            @Override
            protected boolean suspendVmIfNeeded(Domain domain) {
                return true;
            }

            @Override
            protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected String backupNvramIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
                return null;
            }

            @Override
            protected boolean resumeVmIfNeeded(Domain domain, String vmName) {
                return false;
            }

            @Override
            protected boolean resumeVmIfNeeded(Domain domain, String vmName, boolean suspended) {
                return false;
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertTrue("Snapshot metadata must still be returned when resume fails after snapshot creation", answer.getResult());
        assertTrue(answer.getDetails().contains("could not be resumed"));
    }

    @Test
    public void testTakeDiskOnlyVmSnapshotOfRunningVmHandlesNullErrorMessage() throws Exception {
        LibvirtCreateDiskOnlyVMSnapshotCommandWrapper wrapper = new LibvirtCreateDiskOnlyVMSnapshotCommandWrapper() {
            @Override
            protected Pair<String, java.util.Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS,
                    List<com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
                return new Pair<>("<domainsnapshot/>", Collections.emptyMap());
            }

            @Override
            protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
                return false;
            }

            @Override
            protected String backupNvramIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) throws IOException {
                throw new IOException();
            }
        };
        LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        org.libvirt.Connect connect = mock(org.libvirt.Connect.class);
        Domain domain = mock(Domain.class);
        CreateDiskOnlyVmSnapshotCommand command = mock(CreateDiskOnlyVmSnapshotCommand.class);
        VMSnapshotTO target = mock(VMSnapshotTO.class);

        when(command.getVmName()).thenReturn("vm-name");
        when(command.getVolumeTOs()).thenReturn(List.of());
        when(command.getTarget()).thenReturn(target);
        when(command.isUefiEnabled()).thenReturn(true);
        when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(resource.getDisks(connect, "vm-name")).thenReturn(List.of());
        when(resource.getDomain(connect, "vm-name")).thenReturn(domain);

        CreateDiskOnlyVmSnapshotAnswer answer = (CreateDiskOnlyVmSnapshotAnswer) wrapper.takeDiskOnlyVmSnapshotOfRunningVm(command, resource);

        assertFalse(answer.getResult());
    }
}
