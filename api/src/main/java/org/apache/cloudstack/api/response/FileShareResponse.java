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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.fileshare.FileShare;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;


@EntityReference(value = FileShare.class)
public class FileShareResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the file share")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the file share")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "description of the file share")
    private String description;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "ID of the availability zone")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Name of the availability zone")
    private String zoneName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "id of the storage fs vm")
    private String virtualMachineId;

    @SerializedName(ApiConstants.VOLUME_NAME)
    @Param(description = "name of the storage fs data volume")
    private String volumeName;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "id of the storage fs data volume")
    private String volumeId;

    @SerializedName(ApiConstants.STORAGE)
    @Param(description = "name of the storage pool hosting the data volume")
    private String storagePoolName;

    @SerializedName(ApiConstants.STORAGE_ID)
    @Param(description = "id of the storage pool hosting the data volume")
    private String storagePoolId;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "size of the file share")
    private Long size;

    @SerializedName(ApiConstants.SIZEGB)
    @Param(description = "size of the file share in GiB")
    private String sizeGB;

    @SerializedName(ApiConstants.DISK_OFFERING_ID)
    @Param(description = "disk offering for the file share")
    private String diskOfferingId;

    @SerializedName("diskofferingname")
    @Param(description = "disk offering for the file share")
    private String diskOfferingName;

    @SerializedName("diskofferingdisplaytext")
    @Param(description = "disk offering display text for the file share")
    private String diskOfferingDisplayText;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "service offering for the file share")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "service offering for the file share")
    private String serviceOfferingName;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "Network ID of the fileshare")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "Network name of the fileshare")
    private String networkName;

    @SerializedName(ApiConstants.NIC)
    @Param(description = "the list of nics associated with vm", responseObject = NicResponse.class)
    private List<NicResponse> nics;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "ip address of the fileshare")
    private String ipAddress;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "path to mount the fileshare")
    private String path;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the file share")
    private String state;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "the file share provider")
    private String provider;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the file share")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the file share")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the file share")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the file share")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the file share")
    private String domainName;

    @SerializedName(ApiConstants.PROVISIONINGTYPE)
    @Param(description = "provisioning type used in the file share")
    private String provisioningType;

    @SerializedName(ApiConstants.DISK_IO_READ)
    @Param(description = "the read (IO) of disk on the file share")
    private Long diskIORead;

    @SerializedName(ApiConstants.DISK_IO_WRITE)
    @Param(description = "the write (IO) of disk on the file share")
    private Long diskIOWrite;

    @SerializedName(ApiConstants.DISK_KBS_READ)
    @Param(description = "the file share's disk read in KiB")
    private Long diskKbsRead;

    @SerializedName(ApiConstants.DISK_KBS_WRITE)
    @Param(description = "the file share's disk write in KiB")
    private Long diskKbsWrite;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "the bytes allocated")
    private Long virtualSize;

    @SerializedName(ApiConstants.PHYSICAL_SIZE)
    @Param(description = "the bytes actually consumed on disk")
    private Long physicalSize;

    @SerializedName(ApiConstants.UTILIZATION)
    @Param(description = "the disk utilization")
    private String utilization;

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public List<NicResponse> getNics() {
        return nics;
    }

    public void addNic(NicResponse nic) {
        if (this.nics == null) {
            this.nics = new ArrayList<>();
        }
        this.nics.add(nic);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public void setStoragePoolName(String storagePoolName) {
        this.storagePoolName = storagePoolName;
    }

    public void setStoragePoolId(String storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setSizeGB(Long size) {
        if (size != null) {
            this.sizeGB = String.format("%.2f GiB", size / (1024.0 * 1024.0 * 1024.0));
        }
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

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setProvisioningType(String provisioningType) {
        this.provisioningType = provisioningType;
    }

    public void setDiskIORead(Long diskIORead) {
        this.diskIORead = diskIORead;
    }

    public void setDiskIOWrite(Long diskIOWrite) {
        this.diskIOWrite = diskIOWrite;
    }

    public void setDiskKbsRead(Long diskKbsRead) {
        this.diskKbsRead = diskKbsRead;
    }

    public void setDiskKbsWrite(Long diskKbsWrite) {
        this.diskKbsWrite = diskKbsWrite;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public void setPhysicalSize(Long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public void setUtilization(String utilization) {
        this.utilization = utilization;
    }
}