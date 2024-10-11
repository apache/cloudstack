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

import java.util.List;

import org.apache.cloudstack.api.command.user.snapshot.CopySnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotPolicyCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ExtractSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.snapshot.UpdateSnapshotPolicyCmd;

import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public interface SnapshotApiService {

    /**
     * List all snapshots of a disk volume. Optionally lists snapshots created by specified interval
     *
     * @param cmd
     *            the command containing the search criteria (order by, limit, etc.)
     * @return list of snapshots
     */
    Pair<List<? extends Snapshot>, Integer> listSnapshots(ListSnapshotsCmd cmd);

    /**
     * Delete specified snapshot from the specified. If no other policies are assigned it calls destroy snapshot. This
     * will be
     * used for manual snapshots too.
     *
     * @param snapshotId
     *            TODO
     */
    boolean deleteSnapshot(long snapshotId, Long zoneId);

    /**
     * Creates a policy with specified schedule. maxSnaps specifies the number of most recent snapshots that are to be
     * retained.
     * If the number of snapshots go beyond maxSnaps the oldest snapshot is deleted
     *
     * @param cmd
     *            the command that
     * @param policyOwner
     *            TODO
     * @return the newly created snapshot policy if success, null otherwise
     */
    SnapshotPolicy createPolicy(CreateSnapshotPolicyCmd cmd, Account policyOwner);

    /**
     * Get the recurring snapshots scheduled for this volume currently along with the time at which they are scheduled
     *
     * @param cmd
     *            the command wrapping the volumeId (volume for which the snapshots are required) and policyId (to show
     *            snapshots for only this policy).
     * @return The list of snapshot schedules.
     */
    public List<? extends SnapshotSchedule> findRecurringSnapshotSchedule(ListRecurringSnapshotScheduleCmd cmd);

    /**
     * list all snapshot policies assigned to the specified volume
     *
     * @param cmd
     *            the command that specifies the volume criteria
     * @return list of snapshot policies
     */
    Pair<List<? extends SnapshotPolicy>, Integer> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd);

    boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd);

    Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType) throws ResourceAllocationException;

    Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType, Boolean isFromVmSnapshot, List<Long> zoneIds)
            throws ResourceAllocationException;


    /**
     * Create a snapshot of a volume
     *
     * @param snapshotOwner
     *            TODO
     * @param cmd
     *            the API command wrapping the parameters for creating the snapshot (mainly volumeId)
     *
     * @return the Snapshot that was created
     */
    Snapshot createSnapshot(Long volumeId, Long policyId, Long snapshotId, Account snapshotOwner);

    /**
     * Extracts the snapshot to a particular location.
     *
     * @param cmd
     *            the command specifying url (where the snapshot needs to be extracted to), zoneId (zone where the snapshot exists) and
     *            id (the id of the snapshot)
     *
     */
    String extractSnapshot(ExtractSnapshotCmd cmd);

    /**
     * Archives a snapshot from primary storage to secondary storage.
     * @param id Snapshot ID
     * @return Archived Snapshot object
     */
    Snapshot archiveSnapshot(Long id);

    /**
     * @param vol
     * @return
     */
    Long getHostIdForSnapshotOperation(Volume vol);

    Snapshot revertSnapshot(Long snapshotId);

    Snapshot backupSnapshotFromVmSnapshot(Long snapshotId, Long vmId, Long volumeId, Long vmSnapshotId);

    SnapshotPolicy updateSnapshotPolicy(UpdateSnapshotPolicyCmd updateSnapshotPolicyCmd);

    void markVolumeSnapshotsAsDestroyed(Volume volume);

    Snapshot copySnapshot(CopySnapshotCmd cmd) throws StorageUnavailableException, ResourceAllocationException;
}
