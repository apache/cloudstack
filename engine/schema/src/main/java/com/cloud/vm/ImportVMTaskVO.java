//
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
//
package com.cloud.vm;

import org.apache.cloudstack.vm.ImportVmTask;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "import_vm_task")
public class ImportVMTaskVO implements ImportVmTask {

    public ImportVMTaskVO(long zoneId, long accountId, long userId, String displayName,
                          String vcenter, String datacenter, String sourceVMName, long convertHostId, long importHostId) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.userId = userId;
        this.displayName = displayName;
        this.vcenter = vcenter;
        this.datacenter = datacenter;
        this.sourceVMName = sourceVMName;
        this.step = Step.Prepare;
        this.uuid = UUID.randomUUID().toString();
        this.convertHostId = convertHostId;
        this.importHostId = importHostId;
    }

    public ImportVMTaskVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "vm_id")
    private Long vmId;
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "vcenter")
    private String vcenter;

    @Column(name = "datacenter")
    private String datacenter;

    @Column(name = "source_vm_name")
    private String sourceVMName;

    @Column(name = "convert_host_id")
    private long convertHostId;

    @Column(name = "import_host_id")
    private long importHostId;

    @Column(name = "step")
    private Step step;

    @Column(name = "description")
    private String description;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVcenter() {
        return vcenter;
    }

    public void setVcenter(String vcenter) {
        this.vcenter = vcenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getSourceVMName() {
        return sourceVMName;
    }

    public void setSourceVMName(String sourceVMName) {
        this.sourceVMName = sourceVMName;
    }

    public long getConvertHostId() {
        return convertHostId;
    }

    public void setConvertHostId(long convertHostId) {
        this.convertHostId = convertHostId;
    }

    public long getImportHostId() {
        return importHostId;
    }

    public void setImportHostId(long importHostId) {
        this.importHostId = importHostId;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
