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

import java.util.Date;
import com.cloud.storage.Volume.VolumeType;

import com.cloud.serializer.Param;

public class CreateSnapshotResultObject {
	
	@Param(name="id")
	private long id;
	
	@Param(name="account")
	private String accountName;
	
	@Param(name="volumeid")
	private long volumeId;

	@Param(name="domainid")
	private long domainId;
	
	@Param(name="domainname")
	private String domainName;
	
	@Param(name="created")
	private Date created;
	
	@Param(name="name")
	private String name;
	
	@Param(name="path")
	private String path;

	@Param(name="snapshottype")
	private String snapshotType;
	
	@Param(name="volumename")
	private String volumeName;
	
	@Param(name="volumetype")
	private VolumeType volumeType;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getAccountName() {
		return accountName;
	}
	
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	
	public long getVolumeId() {
		return volumeId;
	}
	
	public void setVolumeId(long volumeId) {
		this.volumeId = volumeId;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}
	
	public long getDomainId() {
		return domainId;
	}
	
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	
	public String getDomainName() {
		return domainName;
	}
	
	public void setSnapshotType(String snapshotType){
		this.snapshotType = snapshotType;
	}
	
	public String getSnapshotType(){
		return this.snapshotType;
	}
	
	public String getVolumeName() {
		return volumeName;
	}

	public void setVolumeName(String volumeName) {
		this.volumeName = volumeName;
	}

	public VolumeType getVolumeType() {
		return volumeType;
	}

	public void setVolumeType(VolumeType volumeType) {
		this.volumeType = volumeType;
	}
}
