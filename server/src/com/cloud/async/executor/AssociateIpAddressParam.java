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

public class AssociateIpAddressParam {
	private long userId;
	private long accountId;
	private long domainId;
	private long zoneId;
	
	public AssociateIpAddressParam() {
	}
	
	public AssociateIpAddressParam(long userId, long accountId, long domainId, long zoneId) {
		this.userId = userId;
		this.accountId = accountId;
		this.domainId = domainId;
		this.zoneId = zoneId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}
	
	public long getDomainId() {
		return domainId;
	}
	
	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}
	
	public long getZoneId() {
		return zoneId;
	}
	
	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}
}
