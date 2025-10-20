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

import com.cloud.user.Account;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;

/**
 *
 *
 */
public interface SnapshotManager extends Configurable {

    static final ConfigKey<Integer> SnapshotHourlyMax = new ConfigKey<Integer>(Integer.class, "snapshot.max.hourly", "Snapshots", "8",
            "Maximum recurring hourly snapshots to be retained for a volume. If the limit is reached, early snapshots from the start of the hour are deleted so that newer ones can be saved. This limit does not apply to manual snapshots. If set to 0, recurring hourly snapshots can not be scheduled.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Integer> SnapshotDailyMax = new ConfigKey<Integer>(Integer.class, "snapshot.max.daily", "Snapshots", "8",
            "Maximum recurring daily snapshots to be retained for a volume. If the limit is reached, snapshots from the start of the day are deleted so that newer ones can be saved. This limit does not apply to manual snapshots. If set to 0, recurring daily snapshots can not be scheduled.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Integer> SnapshotWeeklyMax = new ConfigKey<Integer>(Integer.class, "snapshot.max.weekly", "Snapshots", "8",
            "Maximum recurring weekly snapshots to be retained for a volume. If the limit is reached, snapshots from the beginning of the week are deleted so that newer ones can be saved. This limit does not apply to manual snapshots. If set to 0, recurring weekly snapshots can not be scheduled.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Integer> SnapshotMonthlyMax = new ConfigKey<Integer>(Integer.class, "snapshot.max.monthly", "Snapshots", "8",
            "Maximum recurring monthly snapshots to be retained for a volume. If the limit is reached, snapshots from the beginning of the month are deleted so that newer ones can be saved. This limit does not apply to manual snapshots. If set to 0, recurring monthly snapshots can not be scheduled.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Boolean> usageSnapshotSelection = new ConfigKey<Boolean>("Usage", Boolean.class, "usage.snapshot.virtualsize.select", "false",
            "Set the value to true if snapshot usage need to consider virtual size, else physical size is considered ", false);
    public static final ConfigKey<Integer> BackupRetryAttempts = new ConfigKey<Integer>(Integer.class, "backup.retry", "Advanced", "3",
            "Number of times to retry in creating backup of snapshot on secondary", false, ConfigKey.Scope.Global, null);

    public static final ConfigKey<Integer> BackupRetryInterval = new ConfigKey<Integer>(Integer.class, "backup.retry.interval", "Advanced", "300",
            "Time in seconds between retries in backing up snapshot to secondary", false, ConfigKey.Scope.Global, null);

    public static final ConfigKey<Boolean> VmStorageSnapshotKvm = new ConfigKey<>(Boolean.class, "kvm.vmstoragesnapshot.enabled", "Snapshots", "true", "For live snapshot of virtual machine instance on KVM hypervisor without memory. Requires qemu version 1.6+ (on NFS or Local file system) and qemu-guest-agent installed on guest VM", true, ConfigKey.Scope.Global, null);

    ConfigKey<Boolean> KVMSnapshotEnabled = new ConfigKey<>(Boolean.class, "kvm.snapshot.enabled", "Snapshots", "true", "Whether volume snapshot is enabled on running instances " +
            "on a KVM hosts", false, ConfigKey.Scope.Global, null);

    ConfigKey<Boolean> kvmIncrementalSnapshot = new ConfigKey<>(Boolean.class, "kvm.incremental.snapshot", "Snapshots", "false", "Whether differential snapshots are enabled for" +
            " KVM or not. When this is enabled, all KVM snapshots will be incremental. Bear in mind that it will generate a new full snapshot when the snapshot chain reaches the limit defined in snapshot.delta.max.", true, ConfigKey.Scope.Cluster, null);

    ConfigKey<Integer> snapshotDeltaMax = new ConfigKey<>(Integer.class, "snapshot.delta.max", "Snapshots", "16", "Max delta snapshots between two full snapshots. " +
            "Only valid for KVM and XenServer.", true, ConfigKey.Scope.Global, null);

    ConfigKey<Boolean> snapshotShowChainSize = new ConfigKey<>(Boolean.class, "snapshot.show.chain.size", "Snapshots", "false",
            "Whether to show chain size (sum of physical size of snapshot and all its parents) for incremental snapshots in the snapshot response",
            true, ConfigKey.Scope.Global, null);

    public static final ConfigKey<Boolean> UseStorageReplication = new ConfigKey<Boolean>(Boolean.class, "use.storage.replication", "Snapshots", "false", "For snapshot copy to another primary storage in a different zone. Supports only StorPool storage for now", true, ConfigKey.Scope.StoragePool, null);

    void deletePoliciesForVolume(Long volumeId);

    /**
     * For each of the volumes in the account, (which can span across multiple zones and multiple secondary storages), delete
     * the dir on the secondary storage which contains the backed up snapshots for that volume. This is called during
     * deleteAccount.
     *
     * @param accountId
     *            The account which is to be deleted.
     */
    boolean deleteSnapshotDirsForAccount(Account account);

    boolean isHypervisorKvmAndFileBasedStorage(VolumeInfo volumeInfo, StoragePool storagePool);

    boolean canOperateOnVolume(Volume volume);

    boolean backedUpSnapshotsExistsForVolume(Volume volume);

    void cleanupSnapshotsByVolume(Long volumeId);

    Answer sendToPool(Volume vol, Command cmd);

    SnapshotVO getParentSnapshot(VolumeInfo volume);

    SnapshotInfo takeSnapshot(VolumeInfo volume) throws ResourceAllocationException;

    /**
     * Copy the snapshot policies from a volume to another.
     * @param srcVolume source volume.
     * @param destVolume destination volume.
     */
    void copySnapshotPoliciesBetweenVolumes(VolumeVO srcVolume, VolumeVO destVolume);

    void endSnapshotChainForVolume(long volumeId, Hypervisor.HypervisorType hypervisorType);
}
