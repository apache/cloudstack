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
package com.cloud.storage.snapshot;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;

/**
 *
 *
 */
public interface SnapshotManager extends Configurable {

    Integer defaultDiskSnapshotMaxHourly = 8;
    Integer defaultDiskSnapshotMaxDaily = 8;
    Integer defaultDiskSnapshotMaxWeekly = 8;
    Integer defaultDiskSnapshotMaxMonthly = 12;
    Boolean defaultDiskSnapshotUsageVirtualsizeSelect = false;
    Integer defaultBackupRetry = 3;
    Integer defaultBackupRetryInterval = 300;
    Integer snapshotDeltaMax = 16;

    ConfigKey<Integer> snapshotHourlyMax = new ConfigKey<Integer>(Integer.class,
            "snapshot.max.hourly",
            "Snapshots",
            defaultDiskSnapshotMaxHourly.toString(),
            "Maximum recurring hourly snapshots to be retained for a volume. " +
                    "If the limit is reached, early snapshots from the start of the hour are deleted so that " +
                    "newer ones can be saved. This limit does not apply to manual snapshots. " +
                    "If set to 0, recurring hourly snapshots can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> snapshotDailyMax = new ConfigKey<Integer>(Integer.class,
            "snapshot.max.daily",
            "Snapshots",
            defaultDiskSnapshotMaxDaily.toString(),
            "Maximum recurring daily snapshots to be retained for a volume. " +
                    "If the limit is reached, snapshots from the start of the day are deleted so that newer ones can be saved. " +
                    "This limit does not apply to manual snapshots. If set to 0, recurring daily snapshots can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> snapshotWeeklyMax = new ConfigKey<Integer>(Integer.class,
            "snapshot.max.weekly",
            "Snapshots",
            defaultDiskSnapshotMaxWeekly.toString(),
            "Maximum recurring weekly snapshots to be retained for a volume. " +
                    "If the limit is reached, snapshots from the beginning of the week are deleted so that newer ones can be saved. " +
                    "This limit does not apply to manual snapshots. If set to 0, recurring weekly snapshots can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> snapshotMonthlyMax = new ConfigKey<Integer>(Integer.class,
            "snapshot.max.monthly",
            "Snapshots",
            defaultDiskSnapshotMaxMonthly.toString(),
            "Maximum recurring monthly snapshots to be retained for a volume. If the limit is reached, snapshots from the " +
                    "beginning of the month are deleted so that newer ones can be saved. This limit does not apply to manual snapshots. " +
                    "If set to 0, recurring monthly snapshots can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Boolean> usageSnapshotSelection = new ConfigKey<Boolean>("Usage",
            Boolean.class,
            "usage.snapshot.virtualsize.select",
            defaultDiskSnapshotUsageVirtualsizeSelect.toString(),
            "Set the value to true if snapshot usage need to consider virtual size, else physical size is considered ",
            false);

    ConfigKey<Integer> backupRetryAttempts = new ConfigKey<Integer>(Integer.class,
            "backup.retry",
            "Advanced",
            defaultBackupRetry.toString(),
            "Number of times to retry in creating backup of snapshot on secondary storage.",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> backupRetryInterval = new ConfigKey<Integer>(Integer.class,
            "backup.retry.interval",
            "Advanced",
            defaultBackupRetryInterval.toString(),
            "Time in seconds between retries in backing up snapshot to secondary storage.",
            false,
            ConfigKey.Scope.Global,
            null);

    void deletePoliciesForVolume(Long volumeId);

    /**
     * For each of the volumes in the account, (which can span across multiple zones and multiple secondary storages), delete
     * the dir on the secondary storage which contains the backed up snapshots for that volume. This is called during
     * deleteAccount.
     *
     * @param accountId
     *            The account which is to be deleted.
     */
    boolean deleteSnapshotDirsForAccount(long accountId);

    String getSecondaryStorageURL(SnapshotVO snapshot);

    //void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId);

    boolean canOperateOnVolume(Volume volume);

    void cleanupSnapshotsByVolume(Long volumeId);

    Answer sendToPool(Volume vol, Command cmd);

    SnapshotVO getParentSnapshot(VolumeInfo volume);

    Snapshot backupSnapshot(Long snapshotId);

    SnapshotInfo takeSnapshot(VolumeInfo volume) throws ResourceAllocationException;
}
