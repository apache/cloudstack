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

import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.db.Filter;

/**
 * 
 * SnapshotManager contains all of the code to work with volume snapshots.
 * 
 */
public interface SnapshotManager {

    public static final int HOURLYMAX = 8;
    public static final int DAILYMAX = 8;
    public static final int WEEKLYMAX = 8;
    public static final int MONTHLYMAX = 12;
    public static final int DELTAMAX = 16;

    /**
     * After successfully creating a snapshot of a volume, copy the snapshot to the secondary storage for 1) reliability 2) So
     * that storage space on Primary is conserved.
     * 
     * @param snapshot
     *            Info about the created snapshot on primary storage.
     * @param startEventId
     *            event id of the scheduled event for this snapshot
     * @return True if the snapshot was successfully backed up.
     */
    public boolean backupSnapshotToSecondaryStorage(SnapshotVO snapshot);

    /**
     * Once a snapshot has completed, 1) If success, update the database entries 2) If success and there are excess snapshots
     * for any of the policies given, delete the oldest one. 3) Schedule the next recurring snapshot.
     * 
     * @param volumeId
     *            The volume for which the snapshot is being taken
     * @param snapshotId
     *            The snapshot which has just completed
     * @param policyIds
     *            The list of policyIds to which this snapshot belongs to
     * @param backedUp
     *            If true, the snapshot has been successfully created.
     */
    void postCreateSnapshot(Long volumeId, Long snapshotId, Long policyId, boolean backedUp);

    /**
     * Destroys the specified snapshot from secondary storage
     */
    boolean destroySnapshot(long userId, long snapshotId, long policyId);

    /**
     * Deletes snapshot scheduling policy. Delete will fail if this policy is assigned to one or more volumes
     */
    boolean deletePolicy(long userId, Long policyId);

    /**
     * Lists all snapshots for the volume which are created using schedule of the specified policy
     */
    /*
     * List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter);
     */
    /**
     * List all policies which are assigned to the specified volume
     */
    List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId);

    /**
     * List all policies to which a specified snapshot belongs. For ex: A snapshot may belong to a hourly snapshot and a daily
     * snapshot run at the same time
     */
    /*
     * List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId);
     */
    /**
     * List all snapshots for a specified volume irrespective of the policy which created the snapshot
     */
    List<SnapshotVO> listSnapsforVolume(long volumeId);

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

    void validateSnapshot(Long userId, SnapshotVO snapshot);

    SnapshotPolicyVO getPolicyForVolume(long volumeId);

    boolean destroySnapshotBackUp(long snapshotId);

    /**
     * Create a snapshot of a volume
     * 
     * @param cmd
     *            the API command wrapping the parameters for creating the snapshot (mainly volumeId)
     * @return the Snapshot that was created
     */
    SnapshotVO createSnapshotOnPrimary(VolumeVO volume, Long polocyId, Long snapshotId) throws ResourceAllocationException;

    List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId);

    List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter);

    void downloadSnapshotsFromSwift(SnapshotVO ss);

    HostVO getSecondaryStorageHost(SnapshotVO snapshot);

    String getSecondaryStorageURL(SnapshotVO snapshot);

    void deleteSnapshotsForVolume (String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId );

    void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId);

    SwiftTO getSwiftTO(Long id);
}
