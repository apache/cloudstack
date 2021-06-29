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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.Volume;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Volume.class)
@SuppressWarnings("unused")
public class VolumeResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the disk volume")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the disk volume")
    private String name;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "ID of the availability zone")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "name of the availability zone")
    private String zoneName;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "type of the disk volume (ROOT or DATADISK)")
    private String volumeType;

    @SerializedName(ApiConstants.DEVICE_ID)
    @Param(description = "the ID of the device on user vm the volume is attahed to. This tag is not returned when the volume is detached.")
    private Long deviceId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "id of the virtual machine")
    private String virtualMachineId;

    @SerializedName("isoid")
    @Param(description = "the ID of the ISO attached to the virtual machine")
    private String isoId;

    @SerializedName("isoname")
    @Param(description = "the name of the ISO attached to the virtual machine")
    private String isoName;

    @SerializedName("isodisplaytext")
    @Param(description = "an alternate display text of the ISO attached to the virtual machine")
    private String isoDisplayText;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file.")
    private String templateId;

    @SerializedName("templatename")
    @Param(description = "the name of the template for the virtual machine")
    private String templateName;

    @SerializedName("templatedisplaytext")
    @Param(description = " an alternate display text of the template for the virtual machine")
    private String templateDisplayText;

    @SerializedName("vmname")
    @Param(description = "name of the virtual machine")
    private String virtualMachineName;

    @SerializedName("vmdisplayname")
    @Param(description = "display name of the virtual machine")
    private String virtualMachineDisplayName;

    @SerializedName("vmstate")
    @Param(description = "state of the virtual machine")
    private String virtualMachineState;

    @SerializedName(ApiConstants.PROVISIONINGTYPE)
    @Param(description = "provisioning type used to create volumes.")
    private String provisioningType;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "size of the disk volume")
    private Long size;

    @SerializedName(ApiConstants.MIN_IOPS)
    @Param(description = "min iops of the disk volume")
    private Long minIops;

    @SerializedName(ApiConstants.MAX_IOPS)
    @Param(description = "max iops of the disk volume")
    private Long maxIops;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the disk volume was created")
    private Date created;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the disk volume")
    private String state;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the disk volume")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the vpn")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vpn")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the disk volume")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the disk volume")
    private String domainName;

    @SerializedName("storagetype")
    @Param(description = "shared or local storage")
    private String storageType;

    @SerializedName("diskBytesReadRate")
    @Param(description = "bytes read rate of the disk volume")
    private Long bytesReadRate;

    @SerializedName("diskBytesWriteRate")
    @Param(description = "bytes write rate of the disk volume")
    private Long bytesWriteRate;

    @SerializedName("diskIopsReadRate")
    @Param(description = "io requests read rate of the disk volume per the disk offering")
    private Long iopsReadRate;

    @SerializedName("diskIopsWriteRate")
    @Param(description = "io requests write rate of the disk volume per the disk offering")
    private Long iopsWriteRate;

    @SerializedName(ApiConstants.DISK_KBS_READ)
    @Param(description = "the read (bytes) of disk on the vm")
    private Long diskKbsRead;

    @SerializedName(ApiConstants.DISK_KBS_WRITE)
    @Param(description = "the write (bytes) of disk on the vm")
    private Long diskKbsWrite;

    @SerializedName(ApiConstants.DISK_IO_READ)
    @Param(description = "the read (io) of disk on the vm")
    private Long diskIORead;

    @SerializedName(ApiConstants.DISK_IO_WRITE)
    @Param(description = "the write (io) of disk on the vm")
    private Long diskIOWrite;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "Hypervisor the volume belongs to")
    private String hypervisor;

    @SerializedName(ApiConstants.DISK_OFFERING_ID)
    @Param(description = "ID of the disk offering")
    private String diskOfferingId;

    @SerializedName("diskofferingname")
    @Param(description = "name of the disk offering")
    private String diskOfferingName;

    @SerializedName("diskofferingdisplaytext")
    @Param(description = "the display text of the disk offering")
    private String diskOfferingDisplayText;

    @SerializedName("storage")
    @Param(description = "name of the primary storage hosting the disk volume")
    private String storagePoolName;

    @SerializedName(ApiConstants.SNAPSHOT_ID)
    @Param(description = "ID of the snapshot from which this volume was created")
    private String snapshotId;

    @SerializedName("attached")
    @Param(description = "the date the volume was attached to a VM instance")
    private Date attached;

    @SerializedName("destroyed")
    @Param(description = "the boolean state of whether the volume is destroyed or not")
    private Boolean destroyed;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "ID of the service offering for root disk")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "name of the service offering for root disk")
    private String serviceOfferingName;

    @SerializedName("serviceofferingdisplaytext")
    @Param(description = "the display text of the service offering for root disk")
    private String serviceOfferingDisplayText;

    @SerializedName("isextractable")
    @Param(description = "true if the volume is extractable, false otherwise")
    private Boolean extractable;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "the status of the volume")
    private String status;

    @SerializedName(ApiConstants.DISPLAY_VOLUME)
    @Param(description = "an optional field whether to the display the volume to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVolume;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "the path of the volume")
    private String path;

    @SerializedName(ApiConstants.STORAGE_ID)
    @Param(description = "id of the primary storage hosting the disk volume; returned to admin user only", since = "4.3")
    private String storagePoolId;

    @SerializedName(ApiConstants.CHAIN_INFO)
    @Param(description = "the chain info of the volume", since = "4.4")
    String chainInfo;

    @SerializedName(ApiConstants.SNAPSHOT_QUIESCEVM)
    @Param(description = "need quiesce vm or not when taking snapshot", since = "4.3")
    private boolean needQuiescevm;

    @SerializedName(ApiConstants.SUPPORTS_STORAGE_SNAPSHOT)
    @Param(description = "true if storage snapshot is supported for the volume, false otherwise", since = "4.16")
    private boolean supportsStorageSnapshot;

    @SerializedName(ApiConstants.PHYSICAL_SIZE)
    @Param(description = "the bytes allocated")
    private Long physicalsize;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "the bytes actually consumed on disk")
    private Long virtualsize;

    @SerializedName(ApiConstants.UTILIZATION)
    @Param(description = "the disk utilization")
    private String utilization;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "cluster id of the volume")
    private String clusterId;

    @SerializedName(ApiConstants.CLUSTER_NAME)
    @Param(description = "cluster name where the volume is allocated")
    private String clusterName;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "pod id of the volume")
    private String podId;

    @SerializedName(ApiConstants.POD_NAME)
    @Param(description = "pod name of the volume")
    private String podName;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public VolumeResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public Boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(Boolean destroyed) {
        this.destroyed = destroyed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public void setVirtualMachineDisplayName(String virtualMachineDisplayName) {
        this.virtualMachineDisplayName = virtualMachineDisplayName;
    }

    public void setVirtualMachineState(String virtualMachineState) {
        this.virtualMachineState = virtualMachineState;
    }

    public void setProvisioningType(String provisioningType) {
        this.provisioningType = provisioningType;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setMinIops(Long minIops) {
        this.minIops = minIops;
    }

    public void setMaxIops(Long maxIops) {
        this.maxIops = maxIops;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    public Long getDiskKbsRead() {
        return diskKbsRead;
    }

    public void setDiskKbsRead(Long diskKbsRead) {
        this.diskKbsRead = diskKbsRead;
    }

    public Long getDiskKbsWrite() {
        return diskKbsWrite;
    }

    public void setDiskKbsWrite(Long diskKbsWrite) {
        this.diskKbsWrite = diskKbsWrite;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public Long getDiskIORead() {
        return diskIORead;
    }

    public void setDiskIORead(Long diskIORead) {
        this.diskIORead = diskIORead;
    }

    public Long getDiskIOWrite() {
        return diskIOWrite;
    }

    public void setDiskIOWrite(Long diskIOWrite) {
        this.diskIOWrite = diskIOWrite;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public void setDiskOfferingId(String diskOfferingId) {
        this.diskOfferingId = diskOfferingId;
    }

    public void setDiskOfferingName(String diskOfferingName) {
        this.diskOfferingName = diskOfferingName;
    }

    public void setDiskOfferingDisplayText(String diskOfferingDisplayText) {
        this.diskOfferingDisplayText = diskOfferingDisplayText;
    }

    public void setStoragePoolName(String storagePoolName) {
        this.storagePoolName = storagePoolName;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public void setAttached(Date attached) {
        this.attached = attached;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setServiceOfferingDisplayText(String serviceOfferingDisplayText) {
        this.serviceOfferingDisplayText = serviceOfferingDisplayText;
    }

    public void setExtractable(Boolean extractable) {
        this.extractable = extractable;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setDisplayVolume(Boolean displayVm) {
        this.displayVolume = displayVm;
    }

    public void setStoragePoolId(String storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public String getChainInfo() {
        return chainInfo;
    }

    public void setChainInfo(String chainInfo) {
        this.chainInfo = chainInfo;
    }

    public String getStoragePoolId() {
        return storagePoolId;
    }

    public void setNeedQuiescevm(boolean quiescevm) {
        this.needQuiescevm = quiescevm;
    }

    public boolean isNeedQuiescevm() {
        return this.needQuiescevm;
    }

    public void setSupportsStorageSnapshot(boolean supportsStorageSnapshot) {
        this.supportsStorageSnapshot = supportsStorageSnapshot;
    }

    public boolean getSupportsStorageSnapshot() {
        return this.supportsStorageSnapshot;
    }

    public String getIsoId() {
        return isoId;
    }

    public void setIsoId(String isoId) {
        this.isoId = isoId;
    }

    public String getIsoName() {
        return isoName;
    }

    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }

    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public void setIsoDisplayText(String isoDisplayText) {
        this.isoDisplayText = isoDisplayText;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public void setTemplateDisplayText(String templateDisplayText) {
        this.templateDisplayText = templateDisplayText;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public String getVirtualMachineDisplayName() {
        return virtualMachineDisplayName;
    }

    public String getVirtualMachineState() {
        return virtualMachineState;
    }

    public String getProvisioningType() {
        return provisioningType;
    }

    public Long getSize() {
        return size;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Date getCreated() {
        return created;
    }

    public String getState() {
        return state;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getDiskOfferingDisplayText() {
        return diskOfferingDisplayText;
    }

    public String getStoragePoolName() {
        return storagePoolName;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public Date getAttached() {
        return attached;
    }

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public String getServiceOfferingDisplayText() {
        return serviceOfferingDisplayText;
    }

    public Boolean getExtractable() {
        return extractable;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getDisplayVolume() {
        return displayVolume;
    }

    public Long getPhysicalsize() {
        return physicalsize;
    }

    public void setPhysicalsize(Long physicalsize) {
        this.physicalsize = physicalsize;
    }

    public Long getVirtualsize() {
        return virtualsize;
    }

    public void setVirtualsize(Long virtualsize) {
        this.virtualsize = virtualsize;
    }

    public String getUtilization() {
        return utilization;
    }

    public void setUtilization(String utilization) {
        this.utilization = utilization;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPodId() {
        return podId;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }
}
