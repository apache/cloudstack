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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="snapshot_policy")
public class SnapshotPolicyVO {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;
    
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
    
    public SnapshotPolicyVO() { }

    public SnapshotPolicyVO(long volumeId, String schedule, String timezone, short interval, int maxSnaps) {
    	this.volumeId = volumeId;
        this.schedule = schedule;
        this.timezone = timezone;
        this.interval = interval;
        this.maxSnaps = maxSnaps;
        this.active = true;
    }

    public Long getId() {
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
}
