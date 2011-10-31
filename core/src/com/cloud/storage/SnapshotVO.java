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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;

@Entity
@Table(name="snapshots")
public class SnapshotVO implements Snapshot {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id = -1;
    
    @Column(name="data_center_id")
    long dataCenterId;

    @Column(name="account_id")
    long accountId;
    
    @Column(name="domain_id")
    long domainId;

    @Column(name="volume_id")
    Long volumeId;
    
    @Column(name="disk_offering_id")
    Long diskOfferingId;

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
    
    @Column(name="size")
    long size;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name="backup_snap_id")
    String backupSnapshotId;
    
    @Column(name="swift_id")
    Long swiftId;
    
    @Column(name="sechost_id")
    Long secHostId;

    @Column(name="prev_snap_id")
    long prevSnapshotId;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    HypervisorType  hypervisorType;
    
    @Expose
    @Column(name="version")
    String version;
    
    public SnapshotVO() { }

    public SnapshotVO(long dcId, long accountId, long domainId, Long volumeId, Long diskOfferingId, String path, String name, short snapshotType, String typeDescription, long size, HypervisorType hypervisorType ) {
        this.dataCenterId = dcId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.volumeId = volumeId;
        this.diskOfferingId = diskOfferingId;
        this.path = path;
        this.name = name;
        this.snapshotType = snapshotType;
        this.typeDescription = typeDescription;
        this.size = size;
        this.status = Status.Creating;
        this.prevSnapshotId = 0;
        this.hypervisorType = hypervisorType;
        this.version = "2.2";
    }

    
    @Override
    public Long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getVolumeId() {
        return volumeId;
    }

    public long getDiskOfferingId() {
        return diskOfferingId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
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
    public short getsnapshotType() {
        return snapshotType;
    }
    
    @Override
    public Type getType() {
        if (snapshotType < 0 || snapshotType >= Type.values().length) {
            return null;
        }
        return Type.values()[snapshotType];
    }
    
    public Long getSwiftId() {
        return swiftId;
    }

    public void setSwiftId(Long swiftId) {
        this.swiftId = swiftId;
    }

    public Long getSecHostId() {
        return secHostId;
    }

    public void setSecHostId(Long secHostId) {
        this.secHostId = secHostId;
    }

    @Override
    public HypervisorType getHypervisorType() {
    	return hypervisorType;
    }
    
    public void setSnapshotType(short snapshotType) {
        this.snapshotType = snapshotType;
    }
    
    @Override
    public boolean isRecursive(){
        if ( snapshotType >= Type.HOURLY.ordinal() && snapshotType <= Type.MONTHLY.ordinal() ) {
            return true;
        }
        return false;
    }

    public long getSize() {
        return size;
    }

    public String getTypeDescription() {
        return typeDescription;
    }
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
	   
    public static Type getSnapshotType(String snapshotType) {
        for ( Type type : Type.values()) {
            if ( type.equals(snapshotType)) {
                return type;
            }
        }
        return null;
    }

}
