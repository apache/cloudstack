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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "usage_vmsnapshot")
public class UsageVMSnapshotVO implements InternalIdentity {

    @Column(name = "id")
    // volumeId
    private long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "disk_offering_id")
    private Long diskOfferingId;

    @Column(name = "size")
    private long size;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = "processed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date processed;

    protected UsageVMSnapshotVO() {
    }

    public UsageVMSnapshotVO(long id, long zoneId, long accountId, long domainId, long vmId, Long diskOfferingId, long size, Date created, Date processed) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.diskOfferingId = diskOfferingId;
        this.id = id;
        this.size = size;
        this.created = created;
        this.vmId = vmId;
        this.processed = processed;
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

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public long getSize() {
        return size;
    }

    public Date getProcessed() {
        return processed;
    }

    public void setProcessed(Date processed) {
        this.processed = processed;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public long getVmId() {
        return vmId;
    }

    @Override
    public long getId() {
        return this.id;
    }

}
