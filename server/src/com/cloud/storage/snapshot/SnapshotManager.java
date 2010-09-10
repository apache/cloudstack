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

import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.Filter;

/**
*
* SnapshotManager contains all of the code to work with volume snapshots.
* 
*/
public interface SnapshotManager extends Manager {
 
	public static final int HOURLYMAX = 8;
	public static final int DAILYMAX = 8;
	public static final int WEEKLYMAX = 8;
	public static final int MONTHLYMAX = 8;
	
	/**
     * This is the synchronous version of the below command. 
     * @throws ResourceAllocationException 
     * @throws InvalidParameterValueException 
     */
    SnapshotVO createSnapshot(long userId, long volumeId, List<Long> policies) throws InvalidParameterValueException, ResourceAllocationException;

    /**
     * Creates a snapshot for the given volume
     */
    long createSnapshotAsync(long userId, long volumeId, List<Long> policies);

    /**
     * After successfully creating a snapshot of a volume, copy the snapshot to the secondary storage for 
     * 1) reliability
     * 2) So that storage space on Primary is conserved. 
     * @param userId The user who invoked this command.
     * @param snapshot Info about the created snapshot on primary storage.
     * @return True if the snapshot was successfully backed up. 
     */
    public boolean backupSnapshotToSecondaryStorage(long userId, SnapshotVO snapshot);
    
    /**
     * Once a snapshot has completed, 
     * 1) If success, update the database entries 
     * 2) If success and there are excess snapshots for any of the policies given, delete the oldest one.
     * 3) Schedule the next recurring snapshot.
     * @param userId     The user who executed this command
     * @param volumeId   The volume for which the snapshot is being taken
     * @param snapshotId The snapshot which has just completed
     * @param policyIds  The list of policyIds to which this snapshot belongs to
     * @param backedUp   If true, the snapshot has been successfully created.
     */
    void postCreateSnapshot(long userId, long volumeId, long snapshotId, List<Long> policyIds, boolean backedUp);
    
    /**
     * Creates a volume from the specified snapshot. A new volume is returned which is not attached to any VM Instance
     */
    //VolumeVO createVolumeFromSnapshot(long userId, long accountId, long snapshotId, String volumeName);
    long createVolumeFromSnapshotAsync(long userId, long accountId, long snapshotId, String volumeName) throws InternalErrorException;
    
    /**
     * Destroys the specified snapshot from secondary storage
     */
    long destroySnapshotAsync(long userId, long volumeId, long snapshotId, long policyId);
    boolean destroySnapshot(long userId, long snapshotId, long policyId);

    /**
     * Delete specified snapshot from the specified.
     * If no other policies are assigned it calls destroy snapshot.
     * This will be used for manual snapshots too.
     */
    boolean deleteSnapshot(long userId, long snapshotId, long policyId);    
    

    /**
     * Creates a policy with specified schedule. maxSnaps specifies the number of most recent snapshots that are to be retained.
     * If the number of snapshots go beyond maxSnaps the oldest snapshot is deleted 
     * @param volumeId
     * @param userId 
     * @param timeZone The timezone in which the 'schedule' string is specified
     */
    SnapshotPolicyVO createPolicy(long userId, long accountId, long volumeId, String schedule, short interval, int maxSnaps, String timezone);
    
    /**
     * Deletes snapshot scheduling policy. Delete will fail if this policy is assigned to one or more volumes
     */
    boolean deletePolicy(long userId, long policyId);
    
    /**
     * Lists all snapshots for the volume which are created using schedule of the specified policy
     */
    List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter);
      
    /**
     * List all policies which are assigned to the specified volume
     */
    List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId);
    
    /**
     * List all policies to which a specified snapshot belongs. For ex: A snapshot 
     * may belong to a hourly snapshot and a daily snapshot run at the same time
     */
    List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId);
    
    /**
     * List all snapshots for a specified volume irrespective of the policy which
     * created the snapshot
     */
    List<SnapshotVO> listSnapsforVolume(long volumeId);

    /**
     * Get the recurring snapshots scheduled for this volume currently along with the time at which they are scheduled 
     * @param volumeId The volume for which the snapshots are required.
     * @param policyId Show snapshots for only this policy.
     * @return The list of snapshot schedules.
     */
    public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(Long volumeId, Long policyId);

	SnapshotPolicyVO getPolicyForVolumeByInterval(long volumeId, short interval);

	void deletePoliciesForVolume(Long volumeId);

    /**
     * For each of the volumes in the account, 
     * (which can span across multiple zones and multiple secondary storages), 
     * delete the dir on the secondary storage which contains the backed up snapshots for that volume.
     * This is called during deleteAccount.
     * @param accountId The account which is to be deleted.
     */
	boolean deleteSnapshotDirsForAccount(long accountId);

    void validateSnapshot(Long userId, SnapshotVO snapshot);

	ImageFormat getImageFormat(Long volumeId);

}
