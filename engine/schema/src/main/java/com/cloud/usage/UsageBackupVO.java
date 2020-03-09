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

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "usage_backup")
public class UsageBackupVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "backup_offering_id")
    private long backupOfferingId;

    @Column(name = "size")
    private long size;

    @Column(name = "protected_size")
    private long protectedSize;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected UsageBackupVO() {
    }

    public UsageBackupVO(long zoneId, long accountId, long domainId, long vmId, long backupOfferingId, Date created) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.vmId = vmId;
        this.backupOfferingId = backupOfferingId;
        this.created = created;
    }

    public UsageBackupVO(long id, long zoneId, long accountId, long domainId, long vmId, long backupOfferingId, long size, long protectedSize, Date created, Date removed) {
        this.id = id;
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.vmId = vmId;
        this.backupOfferingId = backupOfferingId;
        this.size = size;
        this.protectedSize = protectedSize;
        this.created = created;
        this.removed = removed;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
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

    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public long getBackupOfferingId() {
        return backupOfferingId;
    }

    public void setBackupOfferingId(long backupOfferingId) {
        this.backupOfferingId = backupOfferingId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getProtectedSize() {
        return protectedSize;
    }

    public void setProtectedSize(long protectedSize) {
        this.protectedSize = protectedSize;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
