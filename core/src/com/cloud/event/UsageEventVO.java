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

package com.cloud.event;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="usage_event")
public class UsageEventVO implements UsageEvent {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
	private long id = -1;

	@Column(name="type")
	private String type;
	
	@Column(name=GenericDao.CREATED_COLUMN)
	private Date createDate;

	@Column(name="account_id")
	private long accountId;

    @Column(name="zone_id")
	private long zoneId;

    @Column(name="resource_id")
    private long resourceId;

    @Column(name="resource_name")
    private String resourceName;
    
    @Column(name="offering_id")
    private Long offeringId;
    
    @Column(name="template_id")
    private Long templateId;

    @Column(name="size")
    private Long size;
    
    @Column(name="resource_type")
    private String resourceType;
    
    @Column(name="processed")
    boolean processed;

    
	public UsageEventVO() {
	}
	
	public UsageEventVO(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size) {
	    this.type = usageType;
	    this.accountId = accountId;
	    this.zoneId = zoneId;
	    this.resourceId = resourceId;
	    this.resourceName = resourceName;
	    this.offeringId = offeringId;
	    this.templateId = templateId;
	    this.size = size;
	}
	
    public UsageEventVO(String usageType, long accountId, long zoneId, long resourceId, String resourceName) {
        this.type = usageType;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }
	
    //IPAddress usage event
	public UsageEventVO(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType, boolean isElastic) {
	    this.type = usageType;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.resourceId = ipAddressId;
        this.resourceName = ipAddress;
        this.size = (isSourceNat ? 1L : 0L);
        this.resourceType = guestType;
        this.templateId = (isElastic ? 1L : 0L);
    }
	
	public UsageEventVO(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, String resourceType) {
	    this.type = usageType;
	    this.accountId = accountId;
	    this.zoneId = zoneId;
	    this.resourceId = resourceId;
	    this.resourceName = resourceName;
	    this.offeringId = offeringId;
	    this.templateId = templateId;
	    this.resourceType = resourceType;
	}
	
	@Override
    public long getId() {
		return id;
	}
	@Override
    public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	@Override
    public Date getCreateDate() {
		return createDate;
	}
	public void setCreatedDate(Date createdDate) {
	    createDate = createdDate;
	}
	
    @Override
    public long getAccountId() {
        return accountId;
    }
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }
    @Override
    public long getZoneId() {
        return zoneId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }
    @Override
    public long getResourceId() {
        return resourceId;
    }
    
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setOfferingId(long offeringId) {
        this.offeringId = offeringId;
    }
    @Override
    public Long getOfferingId() {
        return offeringId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }
    @Override
    public Long getTemplateId() {
        return templateId;
    }

    public void setSize(long size) {
        this.size = size;
    }
    @Override
    public Long getSize() {
        return size;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    public String getResourceType() {
        return resourceType;
    }

}
