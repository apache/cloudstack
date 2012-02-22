/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.usage;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="usage_ip_address")
public class UsageIPAddressVO {
	@Column(name="account_id")
    private long accountId;

    @Column(name="domain_id")
	private long domainId;

    @Column(name="zone_id")
    private long zoneId;

    @Column(name="id")
    private long id;
    
	@Column(name="public_ip_address")
    private String address = null;
	
	@Column(name="is_source_nat")
	private boolean isSourceNat = false;

	@Column(name="is_system")
	private boolean isSystem = false;
	
	@Column(name="assigned")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date assigned = null;

	@Column(name="released")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date released = null;

	protected UsageIPAddressVO() {
	}

	public UsageIPAddressVO(long id, long accountId, long domainId, long zoneId, String address, boolean isSourceNat, boolean isSystem, Date assigned, Date released) {
	    this.id = id;
		this.accountId = accountId;
		this.domainId = domainId;
		this.zoneId = zoneId;
		this.address = address;
		this.isSourceNat = isSourceNat;
		this.isSystem = isSystem;
		this.assigned = assigned;
		this.released = released;
	}
	
	public UsageIPAddressVO(long accountId, String address, Date assigned, Date released) {
        this.accountId = accountId;
        this.address = address;
        this.assigned = assigned;
        this.released = released;
    }

	public long getAccountId() {
		return accountId;
	}

	public long getDomainId() {
	    return domainId;
	}

	public long getZoneId() {
	    return zoneId;
	}

	public long getId() {
	    return id;
	}
	
	public String getAddress() {
		return address;
	}
	
	public boolean isSourceNat() {
	    return isSourceNat;
	}
	
	public boolean isSystem() {
	    return isSystem;
	}

	public Date getAssigned() {
		return assigned;
	}

	public Date getReleased() {
		return released;
	}
	public void setReleased(Date released) {
	    this.released = released;
	}
}
