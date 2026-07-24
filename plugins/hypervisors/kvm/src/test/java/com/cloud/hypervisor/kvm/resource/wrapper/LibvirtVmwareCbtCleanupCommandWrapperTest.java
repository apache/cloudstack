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
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtCleanupCommandWrapperTest {

    private static final String MIGRATION_UUID = "migration-uuid";

    private final LibvirtVmwareCbtCleanupCommandWrapper wrapper = new LibvirtVmwareCbtCleanupCommandWrapper();

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

    private VmwareCbtCleanupCommand createCommand(String targetPath) {
        return new VmwareCbtCleanupCommand(MIGRATION_UUID,
                List.of(new VmwareCbtDiskTO("disk-1", 2000, "[datastore] vm/disk.vmdk", "datastore",
                        targetPath, "qcow2", null, null, 8192)),
                true, true, true);
    }
}
