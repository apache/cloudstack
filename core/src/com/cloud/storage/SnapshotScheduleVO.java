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

package com.cloud.storage;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.api.Identity;
import com.cloud.storage.snapshot.SnapshotSchedule;

@Entity
@Table(name="snapshot_schedule")
public class SnapshotScheduleVO implements SnapshotSchedule, Identity {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	long id;
	
    // DB constraint: For a given volume and policyId, there will only be one entry in this table.
    @Column(name="volume_id")
    long volumeId;

    @Column(name="policy_id")
    long policyId;

    @Column(name="scheduled_timestamp")
    @Temporal(value=TemporalType.TIMESTAMP)
    Date scheduledTimestamp;
    
    @Column(name="async_job_id")
    Long asyncJobId;
    
    @Column(name="snapshot_id")
    Long snapshotId;

    @Column(name="uuid")
    String uuid = UUID.randomUUID().toString();
    
    public SnapshotScheduleVO() { }

    public SnapshotScheduleVO(long volumeId, long policyId, Date scheduledTimestamp) {
        this.volumeId = volumeId;
        this.policyId = policyId;
        this.scheduledTimestamp = scheduledTimestamp;
        this.snapshotId = null;
        this.asyncJobId = null;
    }
    
    public long getId() {
        return id;
    }
    
    public Long getVolumeId() {
        return volumeId;
    }
    
    public Long getPolicyId() {
        return policyId;
    }

	public void setPolicyId(long policyId) {
        this.policyId = policyId;
    }

    /**
	 * @return the scheduledTimestamp
	 */
	public Date getScheduledTimestamp() {
		return scheduledTimestamp;
	}
	
	public void setScheduledTimestamp(Date scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    public Long getAsyncJobId() {
	    return asyncJobId;
	}
	
	public void setAsyncJobId(Long asyncJobId) {
	    this.asyncJobId = asyncJobId;
	}
	
	public Long getSnapshotId() {
	    return snapshotId;
	}
	
	public void setSnapshotId(Long snapshotId) {
	    this.snapshotId = snapshotId;
	}
	
	@Override
	public String getUuid() {
		return this.uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
