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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.Volume;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=Volume.class)
@SuppressWarnings("unused")
public class VolumeResponse extends BaseResponse implements ControlledViewEntityResponse{
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

    @SerializedName("vmname")
    @Param(description = "name of the virtual machine")
    private String virtualMachineName;

    @SerializedName("vmdisplayname")
    @Param(description = "display name of the virtual machine")
    private String virtualMachineDisplayName;

    @SerializedName("vmstate")
    @Param(description = "state of the virtual machine")
    private String virtualMachineState;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "size of the disk volume")
    private Long size;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the disk volume was created")
    private Date created;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the disk volume")
    private String state;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the disk volume")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vpn")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vpn")
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
    @Param(description="the status of the volume")
    private String status;

    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with volume", responseObject = ResourceTagResponse.class)
    private Set<ResourceTagResponse> tags;

    public VolumeResponse(){
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public Boolean getDestroyed() {
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

    public void setSize(Long size) {
        this.size = size;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
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

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void addTag(ResourceTagResponse tag){
        this.tags.add(tag);
    }
}
