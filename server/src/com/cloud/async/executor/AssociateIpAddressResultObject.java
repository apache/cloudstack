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

import com.cloud.serializer.Param;

public class AssociateIpAddressResultObject {
	@Param(name="ipaddress")
	private String ipAddress;
	
	@Param(name="zoneid")
	private long zoneId;
	
	@Param(name="zonename")
	private String zoneName;
	
	@Param(name="issourcenat")
	private boolean sourceNat;
	
	@Param(name="allocated")
	private Date allocated;
	
	@Param(name="account")
	private String acountName;
	
	@Param(name="vlanId")
	private String vlanId;
	
	@Param(name="vlanDbId")
	private long vlanDbId;
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public long getZoneId() {
		return zoneId;
	}
	
	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}
	
	public String getZoneName() {
		return zoneName;
	}
	
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}
	
	public boolean isSourceNat() {
		return sourceNat;
	}
	
	public void setSourceNat(boolean sourceNat) {
		this.sourceNat = sourceNat;
	}
	
	public Date getAllocated() {
		return allocated;
	}

	public void setAllocated(Date allocated) {
		this.allocated = allocated;
	}
	
	public String getAcountName() {
		return acountName;
	}
	
	public void setAcountName(String acountName) {
		this.acountName = acountName;
	}
	
	public long getVlanDbId() {
		return vlanDbId;
	}
	
	public void setVlanDbId(long vlanDbId) {
		this.vlanDbId = vlanDbId;
	}
	
	public String getVlanId() {
		return vlanId;
	}
	
	public void setVlanId(String vlanId) {
		this.vlanId = vlanId;
	}
}
