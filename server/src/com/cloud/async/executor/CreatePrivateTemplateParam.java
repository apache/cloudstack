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

public class CreatePrivateTemplateParam {
	private long userId;
	private Long volumeId;
	private Long snapshotId;
	private long guestOsId;
	private String name;
	private String description;
	private Boolean requiresHvm;
	private Integer bits;
	private Boolean passwordEnabled;
	private Boolean isPublic;
	private Boolean isFeatured;
	
	public CreatePrivateTemplateParam() {
	}

	public CreatePrivateTemplateParam(long userId, Long volumeId, long guestOsId, String name, String description, Boolean requiresHvm, Integer bits, Boolean passwordEnabled, Boolean isPublic, Boolean featured, Long snapshotId) {
		this.userId = userId;
		this.name = name;
		this.description = description;
		this.volumeId = volumeId;
		this.guestOsId = guestOsId;
		this.requiresHvm = requiresHvm;
		this.bits = bits;
		this.passwordEnabled = passwordEnabled;
		this.isPublic = isPublic;
		this.isFeatured = featured;
		this.snapshotId = snapshotId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public long getVolumeId() {
		return volumeId;
	}
	
	public void setVmId(long volumeId) {
		this.volumeId = volumeId;
	}
	
	public Long getSnapshotId() {
		return snapshotId;
	}
	
	public void setSnapshotId(Long snapshotId) {
		this.snapshotId = snapshotId;
	}
	
	public long getGuestOsId() {
		return guestOsId;
	}
	
	public void setGuestOsId(long guestOsId) {
		this.guestOsId = guestOsId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getRequiresHvm() {
	    return requiresHvm;
	}

	public void setRequiresHvm(Boolean requiresHvm) {
	    this.requiresHvm = requiresHvm;
	}

	public Integer getBits() {
	    return bits;
	}

	public void setBits(Integer bits) {
	    this.bits = bits;
	}

	public Boolean isPasswordEnabled() {
	    return passwordEnabled;
	}

	public void setPasswordEnabled(Boolean passwordEnabled) {
	    this.passwordEnabled = passwordEnabled;
	}
	
	public Boolean isPublic() {
		return isPublic;
	}
	
	public boolean isFeatured() {
		return isFeatured;
	}
	
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
}
