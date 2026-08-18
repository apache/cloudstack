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

import org.junit.Assert;
import org.junit.Test;

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
    public void testCurrentStepDurationFormattingMatchesImportTaskStyle() {
        Assert.assertEquals("0 secs", manager.formatDuration(0));
        Assert.assertEquals("1 sec", manager.formatDuration(1));
        Assert.assertEquals("59 secs", manager.formatDuration(59));
        Assert.assertEquals("2 min 9 secs", manager.formatDuration(129));
        Assert.assertEquals("1 hr 1 min 1 sec", manager.formatDuration(3661));
    }
}
