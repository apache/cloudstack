// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
@Table(name = "usage_event")
public class UsageEventVO implements UsageEvent {
    public enum DynamicParameters {
        cpuSpeed, cpuNumber, memory
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id = -1;

    @Column(name = "type")
    private String type;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date createDate;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "resource_id")
    private long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "offering_id")
    private Long offeringId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "size")
    private Long size;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "processed")
    boolean processed;

    @Column(name = "virtual_size")
    private Long virtualSize;

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
    public UsageEventVO(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType, boolean isSystem) {
        this.type = usageType;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.resourceId = ipAddressId;
        this.resourceName = ipAddress;
        this.size = (isSourceNat ? 1L : 0L);
        this.resourceType = guestType;
        this.templateId = (isSystem ? 1L : 0L);
    }

    //Snapshot usage event
    //Snapshots have size as the actual (physical) size and virtual_size as the allocated size
    public UsageEventVO(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size, Long virtualSize) {
        this.type = usageType;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.offeringId = offeringId;
        this.templateId = templateId;
        this.size = size;
        this.virtualSize = virtualSize;
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

    //Security Group usage event
    public UsageEventVO(String usageType, long accountId, long zoneId, long vmId, long securityGroupId) {
        this.type = usageType;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.resourceId = vmId;
        this.offeringId = securityGroupId;
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

    public Long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

}
