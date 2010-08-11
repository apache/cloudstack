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

import java.util.Date;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.Scheduler;

/**
 * This interface defines the methods to schedule and execute recurring snapshots for a volume
 */
public interface SnapshotScheduler extends Manager, Scheduler {

	/**
	 * Schedule the next snapshot job for this policy instance.
	 * Sets the time at which it is going to be scheduled
	 * @param policyInstance Contains the volume and the policy for which the snapshot has to be scheduled.
	 * 
	 * @return The timestamp at which the next snapshot is scheduled.
	 */
	public Date scheduleNextSnapshotJob(SnapshotPolicyVO policyInstance);

	/**
     * Remove schedule for volumeId, policyId combination
     * @param volumeId
     * @param policyId
     * @return
     */
    boolean removeSchedule(Long volumeId, Long policyId);

    Long scheduleManualSnapshot(Long userId, Long volumeId);
    
}
