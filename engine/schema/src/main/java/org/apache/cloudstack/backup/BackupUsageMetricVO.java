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
package org.apache.cloudstack.backup;

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

/**
 * The backup usage metric last published for a VM and backup offering.
 */
@Entity
@Table(name = "backup_usage_metric")
public class BackupUsageMetricVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "backup_offering_id")
    private long backupOfferingId;

    @Column(name = "size")
    private long size;

    @Column(name = "protected_size")
    private long protectedSize;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    protected BackupUsageMetricVO() {
    }

    public BackupUsageMetricVO(long vmId, long backupOfferingId, long size, long protectedSize, Date updated) {
        this.vmId = vmId;
        this.backupOfferingId = backupOfferingId;
        this.size = size;
        this.protectedSize = protectedSize;
        this.updated = updated;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public long getBackupOfferingId() {
        return backupOfferingId;
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

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
