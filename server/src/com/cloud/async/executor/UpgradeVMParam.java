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

public class UpgradeVMParam {
	private long userId;
	private long vmId;
	private long serviceOfferingId;
	private long eventId;
	
	public UpgradeVMParam() {
	}
	
	public UpgradeVMParam(long userId, long vmId, long serviceOfferingId) {
		this.userId = userId;
		this.vmId = vmId;
		this.serviceOfferingId = serviceOfferingId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public long getVmId() {
		return vmId;
	}
	
	public void setVmId(long vmId) {
		this.vmId = vmId;
	}
	
	public long getServiceOfferingId() {
		return serviceOfferingId;
	}
	
	public void setServiceOfferingId(long serviceOfferingId) {
		this.serviceOfferingId = serviceOfferingId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
}
