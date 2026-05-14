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
package com.cloud.vm;

import org.apache.cloudstack.vm.VmwareCbtMigration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vmware_cbt_migration")
public class VmwareCbtMigrationVO implements VmwareCbtMigration {

    public VmwareCbtMigrationVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VmwareCbtMigrationVO(long zoneId, long accountId, long userId, long destinationClusterId,
                                String displayName, String vcenter, String datacenter, String sourceHost,
                                String sourceCluster, String sourceVmName) {
        this();
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.userId = userId;
        this.destinationClusterId = destinationClusterId;
        this.displayName = displayName;
        this.vcenter = vcenter;
        this.datacenter = datacenter;
        this.sourceHost = sourceHost;
        this.sourceCluster = sourceCluster;
        this.sourceVmName = sourceVmName;
        this.state = State.Created;
        this.currentStep = "Created";
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

    @Column(name = "destination_cluster_id")
    private long destinationClusterId;

    @Column(name = "convert_host_id")
    private Long convertHostId;

    @Column(name = "storage_pool_id")
    private Long storagePoolId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "vcenter")
    private String vcenter;

    @Column(name = "datacenter")
    private String datacenter;

    @Column(name = "source_host")
    private String sourceHost;

    @Column(name = "source_cluster")
    private String sourceCluster;

    @Column(name = "source_vm_name")
    private String sourceVmName;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "completed_cycles")
    private int completedCycles;

    @Column(name = "quiet_cycles")
    private int quietCycles;

    @Column(name = "total_changed_bytes")
    private long totalChangedBytes;

    @Column(name = "last_changed_bytes")
    private Long lastChangedBytes;

    @Column(name = "last_dirty_rate")
    private Long lastDirtyRate;

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

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getUserId() {
        return userId;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public long getDestinationClusterId() {
        return destinationClusterId;
    }

    public Long getConvertHostId() {
        return convertHostId;
    }

    public void setConvertHostId(Long convertHostId) {
        this.convertHostId = convertHostId;
    }

    public Long getStoragePoolId() {
        return storagePoolId;
    }

    public void setStoragePoolId(Long storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public String getSourceCluster() {
        return sourceCluster;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public int getCompletedCycles() {
        return completedCycles;
    }

    public void setCompletedCycles(int completedCycles) {
        this.completedCycles = completedCycles;
    }

    public int getQuietCycles() {
        return quietCycles;
    }

    public void setQuietCycles(int quietCycles) {
        this.quietCycles = quietCycles;
    }

    public long getTotalChangedBytes() {
        return totalChangedBytes;
    }

    public void setTotalChangedBytes(long totalChangedBytes) {
        this.totalChangedBytes = totalChangedBytes;
    }

    public Long getLastChangedBytes() {
        return lastChangedBytes;
    }

    public void setLastChangedBytes(Long lastChangedBytes) {
        this.lastChangedBytes = lastChangedBytes;
    }

    public Long getLastDirtyRate() {
        return lastDirtyRate;
    }

    public void setLastDirtyRate(Long lastDirtyRate) {
        this.lastDirtyRate = lastDirtyRate;
    }

    public Date getCreated() {
        return created;
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
}
