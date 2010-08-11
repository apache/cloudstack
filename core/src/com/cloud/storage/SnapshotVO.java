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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;

@Entity
@Table(name="snapshots")
public class SnapshotVO implements Snapshot {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;

    @Column(name="account_id")
    long accountId;

    @Column(name="volume_id")
    long volumeId;

    @Expose
    @Column(name="path")
    String path;

    @Expose
    @Column(name="name")
    String name;

    @Expose
    @Column(name="status", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private Status status;

    @Column(name="snapshot_type")
    short snapshotType;

    @Column(name="type_description")
    String typeDescription;

    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name="backup_snap_id")
    String backupSnapshotId;
    
    @Column(name="prev_snap_id")
    long prevSnapshotId;
    
    public SnapshotVO() { }

    public SnapshotVO(long accountId, long volumeId, String path, String name, short snapshotType, String typeDescription) {
        this.accountId = accountId;
        this.volumeId = volumeId;
        this.path = path;
        this.name = name;
        this.snapshotType = snapshotType;
        this.typeDescription = typeDescription;
        this.status = Status.Creating;
        this.prevSnapshotId = 0;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getVolumeId() {
        return volumeId;
    }

    @Override
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
    	this.path = path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public short getSnapshotType() {
        return snapshotType;
    }
    public void setSnapshotType(short snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getTypeDescription() {
        return typeDescription;
    }
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
    
	@Override
    public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public String getBackupSnapshotId(){
		return backupSnapshotId;
	}
	
	public long getPrevSnapshotId(){
		return prevSnapshotId;
	}
	
	public void setBackupSnapshotId(String backUpSnapshotId){
		this.backupSnapshotId = backUpSnapshotId;
	}
	
	public void setPrevSnapshotId(long prevSnapshotId){
		this.prevSnapshotId = prevSnapshotId;
	}

    public static SnapshotType getSnapshotType(List<Long> policyIds) {
        assert policyIds != null && !policyIds.isEmpty();
        SnapshotType snapshotType = SnapshotType.RECURRING;
        if (policyIds.contains(MANUAL_POLICY_ID)) {
            snapshotType = SnapshotType.MANUAL;
        }
        return snapshotType;
    }
    
    public static SnapshotType getSnapshotType(String snapshotType) {
        if (SnapshotType.MANUAL.equals(snapshotType)) {
            return SnapshotType.MANUAL;
        }
        if (SnapshotType.RECURRING.equals(snapshotType)) {
            return SnapshotType.RECURRING;
        }
        if (SnapshotType.TEMPLATE.equals(snapshotType)) {
            return SnapshotType.TEMPLATE;
        }
        return null;
    }
}
