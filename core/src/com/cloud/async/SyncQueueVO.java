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

package com.cloud.async;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="sync_queue")
public class SyncQueueVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="sync_objtype")
	private String syncObjType;
    
    @Column(name="sync_objid")
	private Long syncObjId;
    
    @Column(name="queue_proc_number")
    private Long lastProcessNumber;
    
    @Column(name="queue_proc_time")
    @Temporal(TemporalType.TIMESTAMP)
	private Date lastProcessTime;
    
    @Column(name="queue_proc_msid")
	private Long lastProcessMsid;
    
    @Column(name="created")
    @Temporal(TemporalType.TIMESTAMP)
	private Date created;
    
    @Column(name="last_updated")
    @Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdated;
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSyncObjType() {
		return syncObjType;
	}

	public void setSyncObjType(String syncObjType) {
		this.syncObjType = syncObjType;
	}

	public Long getSyncObjId() {
		return syncObjId;
	}

	public void setSyncObjId(Long syncObjId) {
		this.syncObjId = syncObjId;
	}
	
	public Long getLastProcessNumber() {
		return lastProcessNumber;
	}
	
	public void setLastProcessNumber(Long number) {
		lastProcessNumber = number;
	}

	public Date getLastProcessTime() {
		return lastProcessTime;
	}

	public void setLastProcessTime(Date lastProcessTime) {
		this.lastProcessTime = lastProcessTime;
	}

	public Long getLastProcessMsid() {
		return lastProcessMsid;
	}

	public void setLastProcessMsid(Long lastProcessMsid) {
		this.lastProcessMsid = lastProcessMsid;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("SyncQueueVO {id:").append(getId());
		sb.append(", syncObjType: ").append(getSyncObjType());
		sb.append(", syncObjId: ").append(getSyncObjId());
		sb.append(", lastProcessMsid: ").append(getLastProcessMsid());
		sb.append(", lastProcessNumber: ").append(getLastProcessNumber());
		sb.append(", lastProcessTime: ").append(getLastProcessTime());
		sb.append(", lastUpdated: ").append(getLastUpdated());
		sb.append(", created: ").append(getCreated());
		sb.append("}");
		return sb.toString();
	}
}
