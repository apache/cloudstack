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
@Table(name="usage_volume")
public class UsageVolumeVO {
	
	@Column(name="zone_id")
    private long zoneId;
	
	@Column(name="account_id")
    private long accountId;

    @Column(name="domain_id")
	private long domainId;

	@Column(name="id")
    private long id;

	@Column(name="disk_offering_id")
    private Long diskOfferingId;
	
	@Column(name="template_id")
	private Long templateId;
	
	@Column(name="size")
    private long size;

	@Column(name="created")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date created = null;

	@Column(name="deleted")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date deleted = null;

	protected UsageVolumeVO() {
	}

	public UsageVolumeVO(long id, long zoneId, long accountId, long domainId, Long diskOfferingId, Long templateId, long size, Date created, Date deleted) {
		this.id = id;
		this.zoneId = zoneId;
		this.accountId = accountId;
		this.domainId = domainId;
		this.diskOfferingId = diskOfferingId;
		this.templateId = templateId;
		this.size = size;
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
	
	public Long getDiskOfferingId() {
	    return diskOfferingId;
	}
	
	public Long getTemplateId() {
        return templateId;
    }
	
	public long getSize() {
        return size;
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
