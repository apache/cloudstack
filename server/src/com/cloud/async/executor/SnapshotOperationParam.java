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

package com.cloud.async.executor;

import java.util.List;

public class SnapshotOperationParam {
    public enum SnapshotOp {Create, Delete, CreateVolume};
    private long accountId;
    private long userId;
	private long snapshotId = 0;
	private List<Long> policyIds = null;
	private long policyId = 0;
	private long volumeId;
	private String name = null;
	private long eventId;
	
	public SnapshotOperationParam() {
	}
	
	// Used for delete snapshot
	public SnapshotOperationParam(long userId, long accountId, long volumeId, long snapshotId, long policyId) {
		setUserId(userId);
		setAccountId(accountId);
		setVolumeId(volumeId);
		this.snapshotId = snapshotId;
		this.policyId = policyId;
	}
	
	// Used to create a snapshot
    public SnapshotOperationParam(long userId, long accountId, long volumeId, List<Long> policyIds) {
        setUserId(userId);
        setAccountId(accountId);
        setVolumeId(volumeId);
        this.policyIds = policyIds;
    }
    
    // Used for CreateVolumeFromSnapshot
	public SnapshotOperationParam(long userId, long accountId, long volumeId, long snapshotId, String volumeName) {
	    setUserId(userId);
	    setAccountId(accountId);
        setVolumeId(volumeId);
        this.snapshotId = snapshotId;
	    setName(volumeName);
	}

	public long getUserId() {
	    return userId;
	}
	
	public long getAccountId() {
	    return accountId;
	}
	
	public long getVolumeId() {
	    return volumeId;
	}
	
	public String getName() {
	    return name;
	}
	
    public long getSnapshotId() {
		return snapshotId;
	}
	
	public void setSnapshotId(long snapshotId) {
		this.snapshotId = snapshotId;
	}
	
	public List<Long> getPolicyIds() {
	    return policyIds;
	}
	
	public long getPolicyId() {
	    return policyId;
	}
	
	private void setUserId(long userId) {
	    this.userId = userId;
	}
	
	private void setAccountId(long accountId) {
	    this.accountId = accountId;
	}
	
	private void setVolumeId(long volumeId) {
	    this.volumeId = volumeId;
	}
	
	private void setName(String name) {
	    this.name = name;
	}
	
    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
}
