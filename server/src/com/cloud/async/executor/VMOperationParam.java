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

public class VMOperationParam {
	public static enum VmOp { Noop, Start, Stop, Reboot, Destroy}; //WARN: Noop may not actually be noop

	private long userId;
	private long vmId;
	private String isoPath;
	private VmOp operation;
	protected long eventId;

	
	public VMOperationParam() {
	}
	
	public VMOperationParam(long userId, long vmId, String isoPath, long eventId) {
		this.userId = userId;
		this.vmId = vmId;
		this.isoPath = isoPath;
		this.operation  = VmOp.Noop;
		this.eventId = eventId;
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
	
	public String getIsoPath() {
		return isoPath;
	}
	
	public void setIsoPath(String isoPath) {
		this.isoPath = isoPath;
	}

	public void setOperation(VmOp operation) {
		this.operation = operation;
	}

	public VmOp getOperation() {
		return operation;
	}

    public long getEventId() {
        return eventId;
    }
}
