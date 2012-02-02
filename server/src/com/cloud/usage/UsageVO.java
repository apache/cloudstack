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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="cloud_usage")
public class UsageVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="zone_id")
    private Long zoneId = null;

    @Column(name="account_id")
	private Long accountId = null;

    @Column(name="domain_id")
    private Long domainId = null;

	@Column(name="description")
    private String description = null;
	
	@Column(name="usage_display")
    private String usageDisplay = null;

	@Column(name="usage_type")
	private int usageType;
	
    @Column(name="raw_usage")
    private Double rawUsage = null;

    @Column(name="vm_instance_id")
    private Long vmInstanceId;

    @Column(name="vm_name")
    private String vmName = null;

    @Column(name="offering_id")
    private Long offeringId = null;

    @Column(name="template_id")
    private Long templateId = null;

    @Column(name="usage_id")
    private Long usageId = null;
    
    @Column(name="type")
    private String type = null;

    @Column(name="size")
    private Long size = null;
    
    @Column(name="network_id")
    private Long networkId = null;

    
	@Column(name="start_date")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date startDate = null;
	
    @Column(name="end_date")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date endDate = null;

    public UsageVO() {
	}

	public UsageVO(Long zoneId, Long accountId, Long domainId, String description, String usageDisplay, 
	               int usageType, Double rawUsage, Long vmId, String vmName, Long offeringId, Long templateId, 
	               Long usageId, Long size, Date startDate, Date endDate) {
		this.zoneId = zoneId;
        this.accountId = accountId;
		this.domainId = domainId;
		this.description = description;
		this.usageDisplay = usageDisplay;
		this.usageType = usageType;
        this.rawUsage = rawUsage;
        this.vmInstanceId = vmId;
        this.vmName = vmName;
		this.offeringId = offeringId;
		this.templateId = templateId;
		this.usageId = usageId;
		this.size = size;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public UsageVO(Long zoneId, Long accountId, Long domainId, String description, String usageDisplay, 
	        int usageType, Double rawUsage, Long usageId, String type, Long networkId, Date startDate, Date endDate) {
	    this.zoneId = zoneId;
	    this.accountId = accountId;
	    this.domainId = domainId;
	    this.description = description;
	    this.usageDisplay = usageDisplay;
	    this.usageType = usageType;
	    this.rawUsage = rawUsage;
	    this.usageId = usageId;
	    this.type = type;
	    this.networkId = networkId;
	    this.startDate = startDate;
	    this.endDate = endDate;
	}

	public UsageVO(Long zoneId, Long accountId, Long domainId, String description, String usageDisplay, 
	        int usageType, Double rawUsage, Long vmId, String vmName, Long offeringId, Long templateId, 
	        Long usageId, Date startDate, Date endDate, String type) {
	    this.zoneId = zoneId;
	    this.accountId = accountId;
	    this.domainId = domainId;
	    this.description = description;
	    this.usageDisplay = usageDisplay;
	    this.usageType = usageType;
	    this.rawUsage = rawUsage;
	    this.vmInstanceId = vmId;
	    this.vmName = vmName;
	    this.offeringId = offeringId;
	    this.templateId = templateId;
	    this.usageId = usageId;
	    this.type = type;
	    this.startDate = startDate;
	    this.endDate = endDate;
	}

	//IPAddress Usage
	public UsageVO(Long zoneId, Long accountId, Long domainId, String description, String usageDisplay, 
	        int usageType, Double rawUsage, Long usageId, long size, String type, Date startDate, Date endDate) {
	    this.zoneId = zoneId;
	    this.accountId = accountId;
	    this.domainId = domainId;
	    this.description = description;
	    this.usageDisplay = usageDisplay;
	    this.usageType = usageType;
	    this.rawUsage = rawUsage;
	    this.usageId = usageId;
	    this.size = size;
	    this.type = type;
	    this.startDate = startDate;
	    this.endDate = endDate;
	}
	
	public Long getId() {
		return id;
	}

	public Long getZoneId() {
	    return zoneId;
	}

	public Long getAccountId() {
		return accountId;
	}

    public Long getDomainId() {
        return domainId;
    }

	public String getDescription() {
		return description;
	}

	public String getUsageDisplay() {
		return usageDisplay;
	}

	public int getUsageType() {
	    return usageType;
	}

    public Double getRawUsage() {
        return rawUsage;
    }

    public Long getVmInstanceId() {
        return vmInstanceId;
    }

    public String getVmName() {
        return vmName;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getUsageId() {
        return usageId;
    }
    
    public String getType() {
        return type;
    }
    
    public Long getNetworkId() {
        return networkId;
    }

    public Long getSize() {
        return size;
    }
    
	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
        return endDate;
    }
}
