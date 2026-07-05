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
package org.apache.cloudstack.vm;

import java.util.Collections;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.storage.Storage;
import com.cloud.vm.VmwareCbtMigrationDiskVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.VmwareCbtMigrationDiskDao;

public class VmwareCbtMigrationImportTest {

    @Test
    public void testConvertedInstanceUsesVirtioDiskControllerForImport() {
        VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = new VmwareCbtMigrationVO(1L, 2L, 3L, 4L,
                "display-name", "vcenter", "datacenter", "source-host", "source-cluster", "source-vm");

        VmwareCbtMigrationDiskVO disk = new VmwareCbtMigrationDiskVO(migration.getId(), "619-2000", 2000,
                "[datastore] source-vm/ROOT.vmdk", "datastore", 64424509440L);
        disk.setTargetPath(String.format("/mnt/pool/cloudstack-cbt/%s/virt-v2v-output-1/%s-sda",
                migration.getUuid(), migration.getUuid()));
        disk.setTargetFormat("qcow2");

        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        Mockito.when(diskDao.listByMigrationId(migration.getId())).thenReturn(Collections.singletonList(disk));
        ReflectionTestUtils.setField(manager, "vmwareCbtMigrationDiskDao", diskDao);
        ReflectionTestUtils.setField(manager, "primaryDataStoreDao", Mockito.mock(PrimaryDataStoreDao.class));

        UnmanagedInstanceTO convertedInstance = ReflectionTestUtils.invokeMethod(manager,
                "createConvertedInstanceForImport", migration);

        Assert.assertNotNull(convertedInstance);
        Assert.assertEquals("virtio", convertedInstance.getDisks().get(0).getController());
    }

    @Test
    public void testConvertedInstanceUsesRbdImageNameAsFileBaseName() {
        VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = new VmwareCbtMigrationVO(1L, 2L, 3L, 4L,
                "display-name", "vcenter", "datacenter", "source-host", "source-cluster", "source-vm");

        VmwareCbtMigrationDiskVO disk = new VmwareCbtMigrationDiskVO(migration.getId(), "619-2000", 2000,
                "[datastore] source-vm/ROOT.vmdk", "datastore", 64424509440L);
        disk.setTargetPath(String.format("cloudstack-cbt-%s-619-2000-ROOT-545", migration.getUuid()));
        disk.setTargetFormat("raw");

        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        Mockito.when(diskDao.listByMigrationId(migration.getId())).thenReturn(Collections.singletonList(disk));
        PrimaryDataStoreDao primaryDataStoreDao = Mockito.mock(PrimaryDataStoreDao.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(primaryDataStoreDao.findById(migration.getStoragePoolId())).thenReturn(storagePool);
        Mockito.when(storagePool.getUuid()).thenReturn("rbd-pool-uuid");
        Mockito.when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        ReflectionTestUtils.setField(manager, "vmwareCbtMigrationDiskDao", diskDao);
        ReflectionTestUtils.setField(manager, "primaryDataStoreDao", primaryDataStoreDao);

        UnmanagedInstanceTO convertedInstance = ReflectionTestUtils.invokeMethod(manager,
                "createConvertedInstanceForImport", migration);

        Assert.assertNotNull(convertedInstance);
        Assert.assertEquals(disk.getTargetPath(), convertedInstance.getDisks().get(0).getFileBaseName());
        Assert.assertEquals("RBD", convertedInstance.getDisks().get(0).getDatastoreType());
        Assert.assertEquals("rbd-pool-uuid", convertedInstance.getDisks().get(0).getDatastoreName());
    }
}
