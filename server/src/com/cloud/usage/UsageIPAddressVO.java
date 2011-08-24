/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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

	@Column(name="assigned")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date assigned = null;

	@Column(name="released")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date released = null;

	protected UsageIPAddressVO() {
	}

	public UsageIPAddressVO(long id, long accountId, long domainId, long zoneId, String address, boolean isSourceNat, Date assigned, Date released) {
	    this.id = id;
		this.accountId = accountId;
		this.domainId = domainId;
		this.zoneId = zoneId;
		this.address = address;
		this.isSourceNat = isSourceNat;
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
