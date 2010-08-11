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

public class AttachISOParam {
	private long vmId;
	private long userId;
	private long isoId;
	private boolean attach;
	private long eventId;
	
	public AttachISOParam() {
	}
	
	public AttachISOParam(long vmId, long userId, long isoId, boolean attach) {
		this.vmId = vmId;
		this.userId = userId;
		this.isoId = isoId;
		this.attach = attach;
	}

	public long getVmId() {
		return vmId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public long getIsoId() {
		return isoId;
	}

	public boolean isAttach() {
	    return attach;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
}
