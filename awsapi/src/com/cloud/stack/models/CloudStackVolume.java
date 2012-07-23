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
package com.cloud.stack.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CloudStackVolume {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.ACCOUNT)
    private String accountName;
    @SerializedName(ApiConstants.ATTACHED)
    private String attached;
    @SerializedName(ApiConstants.CREATED)
    private String created;
    @SerializedName(ApiConstants.DESTROYED)
    private Boolean destroyed;
    @SerializedName(ApiConstants.DEVICE_ID)
    private String deviceId;
    @SerializedName(ApiConstants.DISK_OFFERING_DISPLAY_TEXT)
    private String diskOfferingDisplayText;
    @SerializedName(ApiConstants.DISK_OFFERING_ID)
    private String diskOfferingId;
    @SerializedName(ApiConstants.DISK_OFFERING_NAME)
    private String diskOfferingName;
    @SerializedName(ApiConstants.DOMAIN)
    private String domainName;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.HYPERVISOR)
    private String hypervisor;
    @SerializedName(ApiConstants.IS_EXTRACTABLE)
    private Boolean extractable;
    @SerializedName(ApiConstants.JOB_ID)
    private String jobId;
    @SerializedName(ApiConstants.JOB_STATUS)
    private Integer jobStatus;
    @SerializedName(ApiConstants.NAME)
    private String name;
    @SerializedName(ApiConstants.SERVICE_OFFERING_DISPLAY_TEXT)
    private String serviceOfferingDisplayText;
    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    private String serviceOfferingId;
    @SerializedName(ApiConstants.SERVICE_OFFERING_NAME)
    private String serviceOfferingName;
    @SerializedName(ApiConstants.SIZE)
    private Long size;
    @SerializedName(ApiConstants.SNAPSHOT_ID)
    private String snapshotId;
    @SerializedName(ApiConstants.STATE)
    private String state;
    @SerializedName(ApiConstants.STORAGE)
    private String storagePoolName;
    @SerializedName(ApiConstants.STORAGE_TYPE)
    private String storageType;
    @SerializedName(ApiConstants.TYPE)
    private String volumeType;
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    private String virtualMachineId;
    @SerializedName(ApiConstants.VM_DISPLAY_NAME)
    private String virtualMachineDisplayName;
    @SerializedName(ApiConstants.VM_NAME)
    private String virtualMachineName;
    @SerializedName(ApiConstants.VM_STATE)
    private String virtualMachineState;
    @SerializedName(ApiConstants.ZONE_ID)
    private String zoneId;
    @SerializedName(ApiConstants.ZONE_NAME)
    private String zoneName;
    @SerializedName(ApiConstants.TAGS)
    private List<CloudStackKeyValue> tags;

    
    public CloudStackVolume() {
    }


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @return the accountName
	 */
	public String getAccountName() {
		return accountName;
	}


	/**
	 * @return the attached
	 */
	public String getAttached() {
		return attached;
	}


	/**
	 * @return the created
	 */
	public String getCreated() {
		return created;
	}


	/**
	 * @return the destroyed
	 */
	public Boolean getDestroyed() {
		return destroyed;
	}


	/**
	 * @return the deviceId
	 */
	public String getDeviceId() {
		return deviceId;
	}


	/**
	 * @return the diskOfferingDisplayText
	 */
	public String getDiskOfferingDisplayText() {
		return diskOfferingDisplayText;
	}


	/**
	 * @return the diskOfferingId
	 */
	public String getDiskOfferingId() {
		return diskOfferingId;
	}


	/**
	 * @return the diskOfferingName
	 */
	public String getDiskOfferingName() {
		return diskOfferingName;
	}


	/**
	 * @return the domainName
	 */
	public String getDomainName() {
		return domainName;
	}


	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}


	/**
	 * @return the hypervisor
	 */
	public String getHypervisor() {
		return hypervisor;
	}


	/**
	 * @return the extractable
	 */
	public Boolean getExtractable() {
		return extractable;
	}


	/**
	 * @return the jobId
	 */
	public String getJobId() {
		return jobId;
	}


	/**
	 * @return the jobStatus
	 */
	public Integer getJobStatus() {
		return jobStatus;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @return the serviceOfferingDisplayText
	 */
	public String getServiceOfferingDisplayText() {
		return serviceOfferingDisplayText;
	}


	/**
	 * @return the serviceOfferingId
	 */
	public String getServiceOfferingId() {
		return serviceOfferingId;
	}


	/**
	 * @return the serviceOfferingName
	 */
	public String getServiceOfferingName() {
		return serviceOfferingName;
	}


	/**
	 * @return the size
	 */
	public Long getSize() {
		return size;
	}


	/**
	 * @return the snapshotId
	 */
	public String getSnapshotId() {
		return snapshotId;
	}


	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}


	/**
	 * @return the storagePoolName
	 */
	public String getStoragePoolName() {
		return storagePoolName;
	}


	/**
	 * @return the storageType
	 */
	public String getStorageType() {
		return storageType;
	}


	/**
	 * @return the volumeType
	 */
	public String getVolumeType() {
		return volumeType;
	}


	/**
	 * @return the virtualMachineId
	 */
	public String getVirtualMachineId() {
		return virtualMachineId;
	}


	/**
	 * @return the virtualMachineDisplayName
	 */
	public String getVirtualMachineDisplayName() {
		return virtualMachineDisplayName;
	}


	/**
	 * @return the virtualMachineName
	 */
	public String getVirtualMachineName() {
		return virtualMachineName;
	}


	/**
	 * @return the virtualMachineState
	 */
	public String getVirtualMachineState() {
		return virtualMachineState;
	}


	/**
	 * @return the zoneId
	 */
	public String getZoneId() {
		return zoneId;
	}


	/**
	 * @return the zoneName
	 */
	public String getZoneName() {
		return zoneName;
	}

    /**
     * @return all tags
     */
    public List<CloudStackKeyValue> getTags() {
        return tags;
    }
}
