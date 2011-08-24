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
@Table(name="usage_vm_instance")
public class UsageVMInstanceVO {
    @Column(name="usage_type")
    private int usageType;

    @Column(name="zone_id")
    private long zoneId;

	@Column(name="account_id")
    private long accountId;

	@Column(name="vm_instance_id")
    private long vmInstanceId;

	@Column(name="vm_name")
    private String vmName = null;

	@Column(name="service_offering_id")
	private long serviceOfferingId;

    @Column(name="template_id")
    private long templateId;
    
    @Column(name="hypervisor_type")
    private String hypervisorType;

	@Column(name="start_date")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date startDate = null;

    @Column(name="end_date")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date endDate = null;
   
	protected UsageVMInstanceVO() {
	}

	public UsageVMInstanceVO(int usageType, long zoneId, long accountId, long vmInstanceId, String vmName, long serviceOfferingId,
	        long templateId, String hypervisorType, Date startDate, Date endDate) {
	    this.usageType = usageType;
	    this.zoneId = zoneId;
		this.accountId = accountId;
		this.vmInstanceId = vmInstanceId;
		this.vmName = vmName;
		this.serviceOfferingId = serviceOfferingId;
		this.templateId = templateId;
		this.hypervisorType = hypervisorType;
		this.startDate = startDate;
        this.endDate = endDate;
	}

	public int getUsageType() {
	    return usageType;
	}

	public long getZoneId() {
	    return zoneId;
	}

	public long getAccountId() {
		return accountId;
	}

	public long getVmInstanceId() {
		return vmInstanceId;
	}

    public String getVmName() {
        return vmName;
    }

    public long getSerivceOfferingId() {
        return serviceOfferingId;
    }

    public long getTemplateId() {
        return templateId;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }
    
	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
	    this.endDate = endDate;
	}
}
