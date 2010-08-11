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

public class VolumeOperationParam {
	
	public enum VolumeOp { Create, Attach, Detach;}
	
	// Used for all VolumeOps
	private VolumeOp op;
	private long accountId;
	private long userId;
	
	// Used for  Create
	private long zoneId;
	private String name;
	private long diskOfferingId;
	
	// Used for Attach and Detach
	private long vmId;
	
	
	// Used for Attach, Detach, and Delete
	private long volumeId;
	private long eventId;
	private Long deviceId;

	public VolumeOperationParam() {
	}
	
	public VolumeOp getOp() {
		return op;
	}
	
	public void setOp(VolumeOp op) {
		this.op = op;
	}
	
	public long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public long getZoneId() {
		return zoneId;
	}
	
	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public long getDiskOfferingId() {
		return diskOfferingId;
	}
	
	public void setDiskOfferingId(long diskOfferingId) {
		this.diskOfferingId = diskOfferingId;
	}
	
	public long getVolumeId() {
		return volumeId;
	}
	
	public void setVolumeId(long volumeId) {
		this.volumeId = volumeId;
	}
	
	public long getVmId() {
		return vmId;
	}
	
	public void setVmId(long vmId) {
		this.vmId = vmId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

}
