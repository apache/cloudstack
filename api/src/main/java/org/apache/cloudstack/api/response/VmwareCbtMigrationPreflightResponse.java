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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VmwareCbtMigrationPreflightResponse extends BaseResponse {

    @SerializedName("ready")
    @Param(description = "whether the source VM and destination are ready for VMware CBT migration start")
    private boolean ready;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the destination zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the destination zone name")
    private String zoneName;

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
    @Param(description = "the KVM host selected for conversion and CBT replication")
    private String convertInstanceHostName;

    @SerializedName(ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID)
    @Param(description = "the destination storage pool ID")
    private String storagePoolId;

    @SerializedName(ApiConstants.POOL_NAME)
    @Param(description = "the destination storage pool name")
    private String storagePoolName;

    @SerializedName("storagepooltype")
    @Param(description = "the destination storage pool type")
    private String storagePoolType;

    @SerializedName("storagewritertype")
    @Param(description = "the VMware CBT target storage writer type")
    private String storageWriterType;

    @SerializedName("storagewritersupported")
    @Param(description = "whether the target storage writer is implemented")
    private boolean storageWriterSupported;

    @SerializedName("storagerequiresinplacefinalization")
    @Param(description = "whether the target storage writer requires virt-v2v in-place finalization")
    private boolean storageRequiresInPlaceFinalization;

    @SerializedName("convertinstancehostinplacefinalizationsupported")
    @Param(description = "whether the selected KVM host reports VMware CBT in-place finalization support")
    private boolean convertInstanceHostInPlaceFinalizationSupported;

    @SerializedName("noninplacefinalizationfallbackallowed")
    @Param(description = "whether VMware CBT non-in-place fallback finalization is enabled by configuration")
    private boolean nonInPlaceFinalizationFallbackAllowed;

    @SerializedName("noninplacefinalizationfallbacksupported")
    @Param(description = "whether the selected target storage can use non-in-place fallback finalization")
    private boolean nonInPlaceFinalizationFallbackSupported;

    @SerializedName(ApiConstants.VCENTER)
    @Param(description = "the source VMware vCenter")
    private String vcenter;

    @SerializedName(ApiConstants.DATACENTER_NAME)
    @Param(description = "the source VMware datacenter")
    private String datacenterName;

    @SerializedName(ApiConstants.SOURCE_HOST)
    @Param(description = "the source VMware ESXi host")
    private String sourceHost;

    @SerializedName(ApiConstants.SOURCE_VM_NAME)
    @Param(description = "the source VMware VM name")
    private String sourceVmName;

    @SerializedName("sourcevmmor")
    @Param(description = "the source VMware VM managed object reference")
    private String sourceVmMor;

    @SerializedName("changetrackingsupported")
    @Param(description = "whether the source VM reports CBT support")
    private Boolean changeTrackingSupported;

    @SerializedName("changetrackingenabled")
    @Param(description = "whether CBT is currently enabled on the source VM")
    private Boolean changeTrackingEnabled;

    @SerializedName("consolidationneeded")
    @Param(description = "whether the source VM reports disk consolidation is needed")
    private Boolean consolidationNeeded;

    @SerializedName("existingsnapshotcount")
    @Param(description = "number of existing VMware snapshots on the source VM")
    private Integer existingSnapshotCount;

    @SerializedName("sourcecpunumber")
    @Param(description = "source VMware VM CPU count")
    private Integer sourceCpuNumber;

    @SerializedName("sourcecpuspeed")
    @Param(description = "source VMware VM CPU reservation in MHz used for offering validation, when available")
    private Integer sourceCpuSpeed;

    @SerializedName("sourcememory")
    @Param(description = "source VMware VM memory in MB")
    private Integer sourceMemory;

    @SerializedName("sourceguestosid")
    @Param(description = "source VMware VM guest OS identifier")
    private String sourceGuestOsId;

    @SerializedName("sourceguestos")
    @Param(description = "source VMware VM guest OS name")
    private String sourceGuestOs;

    @SerializedName(ApiConstants.DISK)
    @Param(description = "source VMware disk preflight information")
    private List<VmwareCbtMigrationPreflightDiskResponse> disks;

    @SerializedName("finding")
    @Param(description = "preflight findings")
    private List<VmwareCbtMigrationPreflightFindingResponse> findings;

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
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

    public void setStoragePoolType(String storagePoolType) {
        this.storagePoolType = storagePoolType;
    }

    public void setStorageWriterType(String storageWriterType) {
        this.storageWriterType = storageWriterType;
    }

    public void setStorageWriterSupported(boolean storageWriterSupported) {
        this.storageWriterSupported = storageWriterSupported;
    }

    public void setStorageRequiresInPlaceFinalization(boolean storageRequiresInPlaceFinalization) {
        this.storageRequiresInPlaceFinalization = storageRequiresInPlaceFinalization;
    }

    public void setConvertInstanceHostInPlaceFinalizationSupported(boolean convertInstanceHostInPlaceFinalizationSupported) {
        this.convertInstanceHostInPlaceFinalizationSupported = convertInstanceHostInPlaceFinalizationSupported;
    }

    public void setNonInPlaceFinalizationFallbackAllowed(boolean nonInPlaceFinalizationFallbackAllowed) {
        this.nonInPlaceFinalizationFallbackAllowed = nonInPlaceFinalizationFallbackAllowed;
    }

    public void setNonInPlaceFinalizationFallbackSupported(boolean nonInPlaceFinalizationFallbackSupported) {
        this.nonInPlaceFinalizationFallbackSupported = nonInPlaceFinalizationFallbackSupported;
    }

    public void setVcenter(String vcenter) {
        this.vcenter = vcenter;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public void setSourceVmName(String sourceVmName) {
        this.sourceVmName = sourceVmName;
    }

    public void setSourceVmMor(String sourceVmMor) {
        this.sourceVmMor = sourceVmMor;
    }

    public void setChangeTrackingSupported(Boolean changeTrackingSupported) {
        this.changeTrackingSupported = changeTrackingSupported;
    }

    public void setChangeTrackingEnabled(Boolean changeTrackingEnabled) {
        this.changeTrackingEnabled = changeTrackingEnabled;
    }

    public void setConsolidationNeeded(Boolean consolidationNeeded) {
        this.consolidationNeeded = consolidationNeeded;
    }

    public void setExistingSnapshotCount(Integer existingSnapshotCount) {
        this.existingSnapshotCount = existingSnapshotCount;
    }

    public void setSourceCpuNumber(Integer sourceCpuNumber) {
        this.sourceCpuNumber = sourceCpuNumber;
    }

    public void setSourceCpuSpeed(Integer sourceCpuSpeed) {
        this.sourceCpuSpeed = sourceCpuSpeed;
    }

    public void setSourceMemory(Integer sourceMemory) {
        this.sourceMemory = sourceMemory;
    }

    public void setSourceGuestOsId(String sourceGuestOsId) {
        this.sourceGuestOsId = sourceGuestOsId;
    }

    public void setSourceGuestOs(String sourceGuestOs) {
        this.sourceGuestOs = sourceGuestOs;
    }

    public void setDisks(List<VmwareCbtMigrationPreflightDiskResponse> disks) {
        this.disks = disks;
    }

    public void setFindings(List<VmwareCbtMigrationPreflightFindingResponse> findings) {
        this.findings = findings;
    }
}
