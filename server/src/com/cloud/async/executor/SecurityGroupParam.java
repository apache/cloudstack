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

import java.util.List;

public class SecurityGroupParam {
	private Long userId;
	private Long securityGroupId;
    private List<Long> securityGroupIdList;
    private String publicIp;
    private Long instanceId;
    private long eventId;

    public SecurityGroupParam() {
    }

    public SecurityGroupParam(Long userId, Long securityGroupId, List<Long> securityGroupIdList, String publicIp, Long instanceId, long eventId) {
    	this.userId = userId;
    	this.securityGroupId = securityGroupId;
    	this.securityGroupIdList = securityGroupIdList;
    	this.publicIp = publicIp;
    	this.instanceId = instanceId;
    	this.eventId = eventId;
    }

    public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getSecurityGroupId() {
	    return securityGroupId;
	}

	public void setSecurityGroupId(Long securityGroupId) {
	    this.securityGroupId = securityGroupId;
	}

	public List<Long> getSecurityGroupIdList() {
		return securityGroupIdList;
	}

	public void setSecurityGroupIdList(List<Long> securityGroupIdList) {
		this.securityGroupIdList = securityGroupIdList;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public Long getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(Long instanceId) {
		this.instanceId = instanceId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
}
