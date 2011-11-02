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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.utils.DateUtil.IntervalType;

@Entity
@Table(name="snapshot_policy")
public class SnapshotPolicyVO implements SnapshotPolicy, Identity {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="volume_id")
    long volumeId;

    @Column(name="schedule")
    String schedule;

    @Column(name="timezone")
    String timezone;
    
    @Column(name="interval")
    private short interval;
    
    @Column(name="max_snaps")
    private int maxSnaps;
    
    @Column(name="active")
    boolean active = false;

    @Column(name="uuid")
    String uuid;
    
    public SnapshotPolicyVO() { 
    	this.uuid = UUID.randomUUID().toString();
    }

    public SnapshotPolicyVO(long volumeId, String schedule, String timezone, IntervalType intvType, int maxSnaps) {
    	this.volumeId = volumeId;
        this.schedule = schedule;
        this.timezone = timezone;
        this.interval = (short)intvType.ordinal();
        this.maxSnaps = maxSnaps;
        this.active = true;
    	this.uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }
    
    public long getVolumeId() {
		return volumeId;
	}

	public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getSchedule() {
        return schedule;
    }
    
    public void setInterval(short interval) {
        this.interval = interval;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public short getInterval() {
        return interval;
    }

    public void setMaxSnaps(int maxSnaps) {
        this.maxSnaps = maxSnaps;
    }

    public int getMaxSnaps() {
        return maxSnaps;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
