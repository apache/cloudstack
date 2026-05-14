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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.VmwareCbtMigration;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VmwareCbtMigration.class)
public class VmwareCbtMigrationResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the VMware CBT migration")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the destination zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the destination zone name")
    private String zoneName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account ID")
    private String accountId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the imported VM after cutover")
    private String virtualMachineId;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "the destination KVM cluster ID")
    private String clusterId;

    @SerializedName(ApiConstants.CLUSTER_NAME)
    @Param(description = "the destination KVM cluster name")
    private String clusterName;

    @SerializedName(ApiConstants.CONVERT_INSTANCE_HOST_ID)
    @Param(description = "the KVM host selected for conversion and CBT replication")
    private String convertInstanceHostId;

    @SerializedName("convertinstancehostname")
    @Param(description = "the KVM host name selected for conversion and CBT replication")
    private String convertInstanceHostName;

    @SerializedName(ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID)
    @Param(description = "the storage pool selected for converted disks")
    private String storagePoolId;

    @SerializedName(ApiConstants.POOL_NAME)
    @Param(description = "the storage pool name selected for converted disks")
    private String storagePoolName;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the target VM")
    private String displayName;

    @SerializedName(ApiConstants.VCENTER)
    @Param(description = "the source VMware vCenter")
    private String vcenter;

    @SerializedName(ApiConstants.EXISTING_VCENTER_ID)
    @Param(description = "the linked existing vCenter ID, when used")
    private String existingVcenterId;

    @SerializedName(ApiConstants.DATACENTER_NAME)
    @Param(description = "the source VMware datacenter")
    private String datacenterName;

    @SerializedName(ApiConstants.SOURCE_HOST)
    @Param(description = "the source VMware ESXi host")
    private String sourceHost;

    @SerializedName(ApiConstants.SOURCE_CLUSTER)
    @Param(description = "the source VMware cluster")
    private String sourceCluster;

    @SerializedName(ApiConstants.SOURCE_VM_NAME)
    @Param(description = "the source VMware VM name")
    private String sourceVmName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the migration state")
    private String state;

    @SerializedName(ApiConstants.CURRENT_STEP)
    @Param(description = "the current migration step")
    private String currentStep;

    @SerializedName(ApiConstants.LAST_ERROR)
    @Param(description = "the last migration error")
    private String lastError;

    @SerializedName(ApiConstants.COMPLETED_CYCLES)
    @Param(description = "the number of completed CBT delta cycles")
    private int completedCycles;

    @SerializedName(ApiConstants.QUIET_CYCLES)
    @Param(description = "the number of consecutive quiet CBT delta cycles")
    private int quietCycles;

    @SerializedName(ApiConstants.TOTAL_CHANGED_BYTES)
    @Param(description = "the total changed bytes synchronized across CBT delta cycles")
    private long totalChangedBytes;

    @SerializedName(ApiConstants.LAST_CHANGED_BYTES)
    @Param(description = "the changed bytes observed in the last CBT delta cycle")
    private Long lastChangedBytes;

    @SerializedName(ApiConstants.LAST_DIRTY_RATE)
    @Param(description = "the dirty rate in bytes per second observed in the last CBT delta cycle")
    private Long lastDirtyRate;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the VMware CBT migration")
    private Date created;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the last updated date of the VMware CBT migration")
    private Date lastUpdated;

    public void setId(String id) {
        this.id = id;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setConvertInstanceHostId(String convertInstanceHostId) {
        this.convertInstanceHostId = convertInstanceHostId;
    }

    public void setConvertInstanceHostName(String convertInstanceHostName) {
        this.convertInstanceHostName = convertInstanceHostName;
    }

    public void setStoragePoolId(String storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public void setStoragePoolName(String storagePoolName) {
        this.storagePoolName = storagePoolName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setVcenter(String vcenter) {
        this.vcenter = vcenter;
    }

    public void setExistingVcenterId(String existingVcenterId) {
        this.existingVcenterId = existingVcenterId;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public void setSourceCluster(String sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    public void setSourceVmName(String sourceVmName) {
        this.sourceVmName = sourceVmName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setCompletedCycles(int completedCycles) {
        this.completedCycles = completedCycles;
    }

    public void setQuietCycles(int quietCycles) {
        this.quietCycles = quietCycles;
    }

    public void setTotalChangedBytes(long totalChangedBytes) {
        this.totalChangedBytes = totalChangedBytes;
    }

    public void setLastChangedBytes(Long lastChangedBytes) {
        this.lastChangedBytes = lastChangedBytes;
    }

    public void setLastDirtyRate(Long lastDirtyRate) {
        this.lastDirtyRate = lastDirtyRate;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
