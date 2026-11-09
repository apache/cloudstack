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

import com.cloud.utils.db.GenericDao;

// A checkpoint of the KVM backup export: a libvirt checkpoint of the running VM and a persistent dirty bitmap of the
// same name in every disk image. It stays until a client deletes it, so several clients can keep their own chains.
@Entity
@Table(name = "vm_checkpoint")
public class VmCheckpointVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "checkpoint_id")
    private String checkpointId;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "backup_id")
    private Long backupId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public VmCheckpointVO() {
    }

    public VmCheckpointVO(long vmId, String checkpointId, Long createTime, Long backupId) {
        this.vmId = vmId;
        this.checkpointId = checkpointId;
        this.createTime = createTime;
        this.backupId = backupId;
    }

    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    // Creation time of the checkpoint on the host in epoch seconds, as libvirt records it.
    public Long getCreateTime() {
        return createTime;
    }

    public Long getBackupId() {
        return backupId;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return String.format("VmCheckpoint {\"id\": %d, \"vmId\": %d, \"checkpointId\": \"%s\"}", id, vmId, checkpointId);
    }
}
