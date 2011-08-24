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
@Table(name="usage_port_forwarding")
public class UsagePortForwardingRuleVO {
	
	@Column(name="zone_id")
    private long zoneId;
	
	@Column(name="account_id")
    private long accountId;

    @Column(name="domain_id")
	private long domainId;

	@Column(name="id")
    private long id;
	
	@Column(name="created")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date created = null;

	@Column(name="deleted")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date deleted = null;

	protected UsagePortForwardingRuleVO() {
	}

	public UsagePortForwardingRuleVO(long id, long zoneId, long accountId, long domainId, Date created, Date deleted) {
		this.zoneId = zoneId;
		this.accountId = accountId;
		this.domainId = domainId;
		this.id = id;
		this.created = created;
		this.deleted = deleted;
	}

	public long getZoneId() {
		return zoneId;
	}
	
	public long getAccountId() {
		return accountId;
	}

	public long getDomainId() {
	    return domainId;
	}

	public long getId() {
	    return id;
	}
	
	public Date getCreated() {
		return created;
	}

	public Date getDeleted() {
		return deleted;
	}
	public void setDeleted(Date deleted) {
	    this.deleted = deleted;
	}
}
