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

package org.apache.cloudstack.backup;

import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface NativeBackupProvider extends BackupProvider {
    String VM_WORK_JOB_HANDLER = NativeBackupService.class.getSimpleName();

    ConfigKey<Integer> backupCompressionTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.compression.timeout", "28800", "Backup compression timeout (in " +
            "seconds). Will only start counting once the backup compression async job actually starts.", true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> backupCompressionMinimumFreeStorage = new ConfigKey<>("Advanced", Double.class, "backup.compression.minimum.free.storage", "1", "The minimum " +
            "amount of free storage that should be available to start the compression. This configuration uses a multiplier on the backup size, by default, it needs the same " +
            "amount of free storage as the backup uses while uncompressed.", true, ConfigKey.Scope.Zone);

    ConfigKey<Integer> backupCompressionCoroutines = new ConfigKey<>("Advanced", Integer.class, "backup.compression.coroutines", "1", "Number of parallel coroutines " +
            "for the compression process. This is translated to qemu-img '-m' parameter.", true, ConfigKey.Scope.Cluster);

    ConfigKey<Integer> backupCompressionRateLimit = new ConfigKey<>("Advanced", Integer.class, "backup.compression.rate.limit", "0", "Limit the compression rate to " +
            "this configuration's value (in MB/s). Values lower than 1 disable the limit.", true, ConfigKey.Scope.Cluster);

    /**
     * Actually execute the backup after being queued.
     * */
    default Pair<Boolean, Long> orchestrateTakeBackup(Backup backup, boolean quiesceVm, boolean isolated) {
        return null;
    }

    /**
     * Actually delete the backup after being queued.
     * */
    default Boolean orchestrateDeleteBackup(Backup backup, boolean forced) {
        return null;
    }

    /**
     * Actually restore the backup after being queued.
     * */
    default Boolean orchestrateRestoreVMFromBackup(Backup backup, VirtualMachine vm, boolean quickRestore, Long hostId, boolean sameVmAsBackup) {
        return null;
    }

    /**
     * This method should be overwritten by any backup providers that want to schedule their backup restore jobs in the same queue as the VM jobs.
     * Otherwise, just use the restoreBackedUpVolume method.
     * */
    default Pair<Boolean, String> orchestrateRestoreBackedUpVolume(Backup backup, VirtualMachine vm, Backup.VolumeInfo backupVolumeInfo, String hostIp, boolean quickRestore) {
        return null;
    }

    /**
     * This method should be overwritten by any native backup providers that want to allow backup compression through ACS.<br/>
     * The compression is done in two steps:<br/>
     * 1) Compress the backup to a different file;<br/>
     * 2) Switch the old file for the newly compressed one.<br/>
     * <br/> <br/>
     * This method is supposed to execute step 1.
     *
     * @return
     */
    default boolean startBackupCompression(long backupId, long hostId) {
        return false;
    }

    /**
     * This method should be overwritten by any native backup providers that want to allow backup compression through ACS.<br/>
     * The compression is done in two steps:<br/>
     * 1) Compress the backup to a different file;<br/>
     * 2) Switch the old file for the newly compressed one.<br/>
     * <br/> <br/>
     * This method is supposed to execute step 2.
     *
     * @return
     */
    default boolean finalizeBackupCompression(long backupId, long hostId) {
        return false;
    }

    /**
     * This method should be overwritten by any native backup providers that allow volume detach but need to prepare it beforehand.
     * */
    default void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine) {
    }

    /**
     * This method should be overwritten by any native backup providers that allow volume migration but need to prepare it beforehand.
     * */
    default void prepareVolumeForMigration(Volume volume, VirtualMachine virtualMachine) {
    }

    /**
     * This method should be overwritten by any native backup providers that must update metadata regarding a volume after certain operations (such as after a volume migration).
     * */
    default void updateVolumeId(VirtualMachine virtualMachine, long oldVolumeId, long newVolumeId) {
    }

    /**
     * This method should be overwritten by any native backup providers that are compatible with VM Snapshots but need to prepare the VM to be reverted.
     * Currently, the only strategy that calls this method is the {@code KvmFileBasedStorageVmSnapshotStrategy}.
     * */
    default void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot, VirtualMachine virtualMachine) {
    }
}
