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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.vm.VmwareCbtMigrationDiskVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.VmwareCbtMigrationDiskDao;

public class VmwareCbtMigrationCheckpointTest {

    @Test
    public void successfulCyclePersistsSnapshotChangeIdForNextCycle() {
        VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(migration.getId()).thenReturn(42L);
        VmwareCbtMigrationDiskVO disk = new VmwareCbtMigrationDiskVO(42L, "disk-1", 2000,
                "[datastore] vm/disk.vmdk", "datastore", 1024L);
        disk.setChangeId("baseline");
        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        Mockito.when(diskDao.listByMigrationId(42L)).thenReturn(Collections.singletonList(disk));
        ReflectionTestUtils.setField(manager, "vmwareCbtMigrationDiskDao", diskDao);

        VmwareCbtChangedDiskInfo changedDisk = new VmwareCbtChangedDiskInfo("disk-1", "cycle-1-snapshot",
                Collections.emptyList());
        ReflectionTestUtils.invokeMethod(manager, "updateDiskChangeIds", migration,
                Collections.singletonList(changedDisk));

        Assert.assertEquals("cycle-1-snapshot", disk.getChangeId());
        Mockito.verify(diskDao).update(disk.getId(), disk);
    }
}
