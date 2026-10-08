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

import org.apache.cloudstack.api.command.admin.vm.DeleteVmwareCbtMigrationCmd;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.vm.VmwareCbtMigrationDiskVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.VmwareCbtMigrationCycleDao;
import com.cloud.vm.dao.VmwareCbtMigrationDao;
import com.cloud.vm.dao.VmwareCbtMigrationDiskDao;

public class VmwareCbtMigrationDeletePolicyTest {

    private final VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();

    @Test
    public void testCompletedMigrationDeleteIsAllowedButNeverCleansTargetDisks() {
        Assert.assertTrue(manager.canDeleteMigrationState(VmwareCbtMigration.State.Completed));
        Assert.assertFalse(manager.shouldCleanupDeletedMigration(VmwareCbtMigration.State.Completed, true));
    }

    @Test
    public void testFailedAndCancelledMigrationsCanCleanTargetDisksWhenRequested() {
        Assert.assertTrue(manager.canDeleteMigrationState(VmwareCbtMigration.State.Failed));
        Assert.assertTrue(manager.canDeleteMigrationState(VmwareCbtMigration.State.Cancelled));
        Assert.assertTrue(manager.shouldCleanupDeletedMigration(VmwareCbtMigration.State.Failed, true));
        Assert.assertTrue(manager.shouldCleanupDeletedMigration(VmwareCbtMigration.State.Cancelled, true));
        Assert.assertFalse(manager.shouldCleanupDeletedMigration(VmwareCbtMigration.State.Failed, false));
        Assert.assertFalse(manager.shouldCleanupDeletedMigration(VmwareCbtMigration.State.Cancelled, false));
    }

    @Test
    public void testActiveMigrationsCannotBeDeleted() {
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.Created));
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.InitialSync));
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.Replicating));
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.ReadyForCutover));
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.CuttingOver));
        Assert.assertFalse(manager.canDeleteMigrationState(VmwareCbtMigration.State.ReadyForImport));
    }

    @Test
    public void testCancelAndDeleteCleanupProtectRecordedImportedVmInEveryState() {
        VmwareCbtMigrationManagerImpl cleanupManager = new VmwareCbtMigrationManagerImpl();
        AgentManager agentManager = Mockito.mock(AgentManager.class);
        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        PrimaryDataStoreDao poolDao = Mockito.mock(PrimaryDataStoreDao.class);
        ReflectionTestUtils.setField(cleanupManager, "agentManager", agentManager);
        ReflectionTestUtils.setField(cleanupManager, "vmwareCbtMigrationDiskDao", diskDao);
        ReflectionTestUtils.setField(cleanupManager, "primaryDataStoreDao", poolDao);

        for (VmwareCbtMigration.State state : VmwareCbtMigration.State.values()) {
            VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
            Mockito.when(migration.getState()).thenReturn(state);
            Mockito.when(migration.getVmId()).thenReturn(73L);
            Mockito.when(migration.getConvertHostId()).thenReturn(null);

            ReflectionTestUtils.invokeMethod(cleanupManager, "sendCleanupCommandIfPossible", migration);
            ReflectionTestUtils.invokeMethod(cleanupManager, "sendCleanupCommandForDelete", migration);
        }

        Mockito.verifyNoInteractions(agentManager, diskDao, poolDao);
    }

    @Test
    public void testRecordedImportedVmDoesNotPreventMigrationRecordDeletionOrSourceCleanup() {
        for (VmwareCbtMigration.State state : new VmwareCbtMigration.State[] {
                VmwareCbtMigration.State.Cancelled, VmwareCbtMigration.State.Failed, VmwareCbtMigration.State.Completed }) {
            VmwareCbtMigrationManagerImpl deleteManager = Mockito.spy(new VmwareCbtMigrationManagerImpl());
            VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
            Mockito.when(migration.getId()).thenReturn(41L);
            Mockito.when(migration.getState()).thenReturn(state);
            Mockito.when(migration.getVmId()).thenReturn(73L);
            Mockito.when(migration.getConvertHostId()).thenReturn(null);
            VmwareCbtMigrationDao migrationDao = Mockito.mock(VmwareCbtMigrationDao.class);
            Mockito.when(migrationDao.findById(41L)).thenReturn(migration);
            Mockito.when(migrationDao.remove(41L)).thenReturn(true);
            VmwareCbtMigrationCycleDao cycleDao = Mockito.mock(VmwareCbtMigrationCycleDao.class);
            Mockito.when(cycleDao.listByMigrationId(41L)).thenReturn(Collections.emptyList());
            VmwareCbtMigrationDiskVO disk = Mockito.mock(VmwareCbtMigrationDiskVO.class);
            Mockito.when(disk.getId()).thenReturn(17L);
            VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
            Mockito.when(diskDao.listByMigrationId(41L)).thenReturn(Collections.singletonList(disk));
            AgentManager agentManager = Mockito.mock(AgentManager.class);
            ReflectionTestUtils.setField(deleteManager, "vmwareCbtMigrationDao", migrationDao);
            ReflectionTestUtils.setField(deleteManager, "vmwareCbtMigrationCycleDao", cycleDao);
            ReflectionTestUtils.setField(deleteManager, "vmwareCbtMigrationDiskDao", diskDao);
            ReflectionTestUtils.setField(deleteManager, "agentManager", agentManager);
            Mockito.doNothing().when(deleteManager).removeLingeringSourceSnapshots(migration);
            DeleteVmwareCbtMigrationCmd cmd = Mockito.mock(DeleteVmwareCbtMigrationCmd.class);
            Mockito.when(cmd.getId()).thenReturn(41L);
            Mockito.when(cmd.getCleanup()).thenReturn(true);

            Assert.assertTrue(deleteManager.deleteVmwareCbtMigration(cmd));

            Mockito.verifyNoInteractions(agentManager);
            Mockito.verify(diskDao, Mockito.times(1)).listByMigrationId(41L);
            Mockito.verify(diskDao).remove(17L);
            Mockito.verify(migrationDao).remove(41L);
            Mockito.verify(deleteManager, Mockito.times(state == VmwareCbtMigration.State.Completed ? 0 : 1))
                    .removeLingeringSourceSnapshots(migration);
        }
    }

    @Test
    public void testMigrationWithoutImportedVmStillExaminesCleanupTargets() {
        VmwareCbtMigrationManagerImpl cleanupManager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(migration.getId()).thenReturn(41L);
        Mockito.when(migration.getVmId()).thenReturn(null);
        VmwareCbtMigrationDao migrationDao = Mockito.mock(VmwareCbtMigrationDao.class);
        Mockito.when(migrationDao.findById(41L)).thenReturn(migration);
        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        Mockito.when(diskDao.listByMigrationId(41L)).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(cleanupManager, "vmwareCbtMigrationDiskDao", diskDao);
        ReflectionTestUtils.setField(cleanupManager, "vmwareCbtMigrationDao", migrationDao);
        ReflectionTestUtils.setField(cleanupManager, "primaryDataStoreDao", Mockito.mock(PrimaryDataStoreDao.class));

        ReflectionTestUtils.invokeMethod(cleanupManager, "sendCleanupCommandIfPossible", migration);

        Mockito.verify(diskDao).listByMigrationId(41L);
    }

    @Test
    public void testCleanupProtectsImportedVmRecordedAfterCallerLoadedMigration() {
        VmwareCbtMigrationManagerImpl cleanupManager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO staleMigration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(staleMigration.getId()).thenReturn(41L);
        Mockito.when(staleMigration.getVmId()).thenReturn(null);
        Mockito.when(staleMigration.getConvertHostId()).thenReturn(null);
        VmwareCbtMigrationVO storedMigration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(storedMigration.getVmId()).thenReturn(73L);
        VmwareCbtMigrationDao migrationDao = Mockito.mock(VmwareCbtMigrationDao.class);
        Mockito.when(migrationDao.findById(41L)).thenReturn(storedMigration);
        VmwareCbtMigrationDiskDao diskDao = Mockito.mock(VmwareCbtMigrationDiskDao.class);
        AgentManager agentManager = Mockito.mock(AgentManager.class);
        ReflectionTestUtils.setField(cleanupManager, "vmwareCbtMigrationDao", migrationDao);
        ReflectionTestUtils.setField(cleanupManager, "vmwareCbtMigrationDiskDao", diskDao);
        ReflectionTestUtils.setField(cleanupManager, "agentManager", agentManager);

        ReflectionTestUtils.invokeMethod(cleanupManager, "sendCleanupCommandIfPossible", staleMigration);
        ReflectionTestUtils.invokeMethod(cleanupManager, "sendCleanupCommandForDelete", staleMigration);

        Mockito.verify(migrationDao, Mockito.times(2)).findById(41L);
        Mockito.verifyNoInteractions(diskDao, agentManager);
    }

    @Test
    public void testCurrentStepDurationFormattingMatchesImportTaskStyle() {
        Assert.assertEquals("0 secs", manager.formatDuration(0));
        Assert.assertEquals("1 sec", manager.formatDuration(1));
        Assert.assertEquals("59 secs", manager.formatDuration(59));
        Assert.assertEquals("2 min 9 secs", manager.formatDuration(129));
        Assert.assertEquals("1 hr 1 min 1 sec", manager.formatDuration(3661));
    }
}
