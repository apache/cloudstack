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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtCleanupCommand;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtCleanupCommandWrapperTest {

    private static final String MIGRATION_UUID = "migration-uuid";

    private final TestLibvirtVmwareCbtCleanupCommandWrapper wrapper = new TestLibvirtVmwareCbtCleanupCommandWrapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolManager;
    @Mock
    private KVMStoragePool rbdStoragePool;

    @Test
    public void testExecuteDeletesOnlyMigrationDirectory() throws IOException {
        Path root = temporaryFolder.newFolder("primary").toPath();
        Path migrationDirectory = root.resolve("cloudstack-cbt").resolve(MIGRATION_UUID);
        Path targetDisk = migrationDirectory.resolve("disk.qcow2");
        Path siblingDisk = root.resolve("cloudstack-cbt").resolve("other-migration").resolve("disk.qcow2");
        Files.createDirectories(targetDisk.getParent());
        Files.createDirectories(siblingDisk.getParent());
        Files.writeString(targetDisk, "partial target");
        Files.writeString(siblingDisk, "do not delete");

        Answer answer = wrapper.execute(createCommand(targetDisk.toString()), libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertFalse(Files.exists(migrationDirectory));
        Assert.assertTrue(Files.exists(siblingDisk));
        Assert.assertEquals(List.of("/cloudstack-cbt/migration-uuid/"), wrapper.killedProcessMarkers);
    }

    @Test
    public void testCleanupDoesNotWaitBehindInFlightCopyCommand() throws IOException {
        Path targetDisk = temporaryFolder.newFile("sequential-copy.qcow2").toPath();

        Assert.assertFalse(createCommand(targetDisk.toString()).executeInSequence());
    }

    @Test
    public void testExecuteSkipsPathsOutsideMigrationDirectory() throws IOException {
        Path targetDisk = temporaryFolder.newFile("disk.qcow2").toPath();

        Answer answer = wrapper.execute(createCommand(targetDisk.toString()), libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(Files.exists(targetDisk));
    }

    @Test
    public void testExecuteDeletesOnlyMarkedRbdTargetImages() {
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.deletePhysicalDisk(Mockito.eq("cloudstack-cbt-migration-uuid-disk-1"), Mockito.eq(Storage.ImageFormat.RAW))).thenReturn(true);
        VmwareCbtCleanupCommand command = new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                                "cloudstack-cbt-migration-uuid-disk-1", "raw", null, null, 8192),
                        new VmwareCbtDiskTO("disk-2", 2001, "[datastore] vm/disk2.vmdk", "datastore",
                                "live-volume-that-must-not-be-removed", "raw", null, null, 8192)),
                true, true, true);
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Mockito.verify(rbdStoragePool).deletePhysicalDisk("cloudstack-cbt-migration-uuid-disk-1", Storage.ImageFormat.RAW);
        Mockito.verify(rbdStoragePool, Mockito.never()).deletePhysicalDisk("live-volume-that-must-not-be-removed", Storage.ImageFormat.RAW);
    }

    @Test
    public void testExecuteDeletesOnlyMarkedBlockDeviceTargetVolumes() {
        KVMStoragePool linstorStoragePool = Mockito.mock(KVMStoragePool.class);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.Linstor, "linstor-pool-uuid")).thenReturn(linstorStoragePool);
        Mockito.when(linstorStoragePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(linstorStoragePool.deletePhysicalDisk(Mockito.eq("cbt-migratio-2000"), Mockito.eq(Storage.ImageFormat.RAW))).thenReturn(true);
        VmwareCbtCleanupCommand command = new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                                "cbt-migratio-2000", "raw", null, null, 8192),
                        new VmwareCbtDiskTO("disk-2", 2001, "[datastore] vm/disk2.vmdk", "datastore",
                                "live-volume-that-must-not-be-removed", "raw", null, null, 8192)),
                true, true, true);
        command.setTargetStorageType(VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.Linstor);
        command.setDestinationStoragePoolUuid("linstor-pool-uuid");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Mockito.verify(linstorStoragePool).deletePhysicalDisk("cbt-migratio-2000", Storage.ImageFormat.RAW);
        Mockito.verify(linstorStoragePool, Mockito.never()).deletePhysicalDisk("live-volume-that-must-not-be-removed", Storage.ImageFormat.RAW);
    }

    @Test
    public void testExecuteFailsWhenMarkedRbdTargetImageCannotBeDeleted() {
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getUuid()).thenReturn("rbd-pool-uuid");
        Mockito.when(rbdStoragePool.deletePhysicalDisk(Mockito.eq("cloudstack-cbt-migration-uuid-disk-1"), Mockito.eq(Storage.ImageFormat.RAW))).thenReturn(false);
        // The image is still listed on the pool, so the deletion genuinely failed and cleanup must say so
        // rather than treating the target as already removed.
        KVMPhysicalDisk stillPresent = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(stillPresent.getName()).thenReturn("cloudstack-cbt-migration-uuid-disk-1");
        Mockito.when(rbdStoragePool.listPhysicalDisks()).thenReturn(List.of(stillPresent));
        VmwareCbtCleanupCommand command = new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                        "cloudstack-cbt-migration-uuid-disk-1", "raw", null, null, 8192)),
                true, true, true);
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Unable to clean up VMware CBT migration"));
    }

    /**
     * A cancelled migration has already had its targets released, so the delete that follows (whose
     * cleanup parameter defaults to true) runs cleanup a second time. Cleanup has to be idempotent: a
     * target the pool no longer lists is the intended end state, not a failure.
     */
    @Test
    public void testExecuteSucceedsWhenRbdTargetImageIsAlreadyGone() {
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getUuid()).thenReturn("rbd-pool-uuid");
        Mockito.when(rbdStoragePool.deletePhysicalDisk(Mockito.eq("cloudstack-cbt-migration-uuid-disk-1"), Mockito.eq(Storage.ImageFormat.RAW))).thenReturn(false);
        // The pool is reachable and simply no longer holds the image.
        KVMPhysicalDisk unrelated = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(unrelated.getName()).thenReturn("some-other-image");
        Mockito.when(rbdStoragePool.listPhysicalDisks()).thenReturn(List.of(unrelated));
        VmwareCbtCleanupCommand command = new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                        "cloudstack-cbt-migration-uuid-disk-1", "raw", null, null, 8192)),
                true, true, true);
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
    }

    /**
     * If the pool cannot be inspected, absence cannot be proven, so a failed deletion must still be
     * reported as a failure rather than masked.
     */
    @Test
    public void testExecuteFailsWhenPoolCannotBeInspected() {
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getUuid()).thenReturn("rbd-pool-uuid");
        Mockito.when(rbdStoragePool.deletePhysicalDisk(Mockito.eq("cloudstack-cbt-migration-uuid-disk-1"), Mockito.eq(Storage.ImageFormat.RAW))).thenReturn(false);
        Mockito.when(rbdStoragePool.listPhysicalDisks()).thenThrow(new RuntimeException("pool unreachable"));
        VmwareCbtCleanupCommand command = new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                        "cloudstack-cbt-migration-uuid-disk-1", "raw", null, null, 8192)),
                true, true, true);
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
    }

    private VmwareCbtCleanupCommand createCommand(String targetPath) {
        return new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                        targetPath, "qcow2", null, null, 8192)),
                true, true, true);
    }

    private static class TestLibvirtVmwareCbtCleanupCommandWrapper extends LibvirtVmwareCbtCleanupCommandWrapper {
        private final List<String> killedProcessMarkers = new ArrayList<>();

        @Override
        protected void killInFlightCopyProcesses(String marker, String migrationUuid) {
            killedProcessMarkers.add(marker);
        }
    }
}
