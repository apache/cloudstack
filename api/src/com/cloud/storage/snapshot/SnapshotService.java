/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.snapshot;

import java.util.List;

import com.cloud.api.commands.CreateSnapshotPolicyCmd;
import com.cloud.api.commands.DeleteSnapshotPoliciesCmd;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.commands.ListSnapshotPoliciesCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;

public interface SnapshotService {

    /**
     * List all snapshots of a disk volume. Optionally lists snapshots created by specified interval
     * 
     * @param cmd
     *            the command containing the search criteria (order by, limit, etc.)
     * @return list of snapshots
     * @throws PermissionDeniedException
     */
    List<? extends Snapshot> listSnapshots(ListSnapshotsCmd cmd);

    /**
     * Delete specified snapshot from the specified. If no other policies are assigned it calls destroy snapshot. This
     * will be
     * used for manual snapshots too.
     * 
     * @param snapshotId
     *            TODO
     */
    boolean deleteSnapshot(long snapshotId);

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
    List<? extends SnapshotPolicy> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd);

    boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd);

    Snapshot allocSnapshot(Long volumeId, Long policyId) throws ResourceAllocationException;

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
}
