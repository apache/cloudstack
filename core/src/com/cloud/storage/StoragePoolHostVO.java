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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for storage pools and hosts
 * @author chiradeep
 *
 */
@Entity
@Table(name="storage_pool_host_ref")
public class StoragePoolHostVO implements StoragePoolHostAssoc {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Long id;
	
	@Column(name="pool_id")
	private long poolId;
	
	@Column(name="host_id")
	private long hostId;
	
	@Column(name="local_path")
	private String localPath;
	
	@Column(name=GenericDaoBase.CREATED_COLUMN)
	private Date created = null;
	
	@Column(name="last_updated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date lastUpdated = null;
	
	
	public StoragePoolHostVO() {
		super();
	}


	public StoragePoolHostVO(long poolId, long hostId, String localPath) {
		this.poolId = poolId;
		this.hostId = hostId;
		this.localPath = localPath;
	}


	@Override
	public long getHostId() {
		return hostId;
	}


	@Override
	public String getLocalPath() {
		return localPath;
	}

	@Override
	public long getPoolId() {
		return poolId;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public Date getLastUpdated() {
		return lastUpdated;
	}


	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

}
