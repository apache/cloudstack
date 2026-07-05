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
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VmwareCbtMigrationPreflightDiskResponse extends BaseResponse {

    @SerializedName(ApiConstants.SOURCE_DISK_ID)
    @Param(description = "the source VMware disk identifier")
    private String sourceDiskId;

    @SerializedName(ApiConstants.SOURCE_DISK_DEVICE_KEY)
    @Param(description = "the source VMware disk device key")
    private Integer sourceDiskDeviceKey;

    @SerializedName(ApiConstants.SOURCE_DISK_PATH)
    @Param(description = "the source VMware disk path")
    private String sourceDiskPath;

    @SerializedName(ApiConstants.DATASTORE_NAME)
    @Param(description = "the source VMware datastore name")
    private String datastoreName;

    @SerializedName(ApiConstants.CAPACITY)
    @Param(description = "the source disk capacity in bytes")
    private Long capacityBytes;

    @SerializedName("backingtype")
    @Param(description = "the VMware disk backing class")
    private String backingType;

    @SerializedName("diskmode")
    @Param(description = "the VMware disk mode")
    private String diskMode;

    @SerializedName("rdmcompatibilitymode")
    @Param(description = "the RDM compatibility mode, when the disk is an RDM")
    private String rdmCompatibilityMode;

    @SerializedName("independentdisk")
    @Param(description = "whether the VMware disk is configured as independent")
    private boolean independentDisk;

    @SerializedName("physicalrdm")
    @Param(description = "whether the VMware disk is a physical-mode RDM")
    private boolean physicalRdm;

    @SerializedName("changeidpresent")
    @Param(description = "whether a VMware CBT change ID is currently visible for this disk")
    private boolean changeIdPresent;

    public void setSourceDiskId(String sourceDiskId) {
        this.sourceDiskId = sourceDiskId;
    }

    public void setSourceDiskDeviceKey(Integer sourceDiskDeviceKey) {
        this.sourceDiskDeviceKey = sourceDiskDeviceKey;
    }

    public void setSourceDiskPath(String sourceDiskPath) {
        this.sourceDiskPath = sourceDiskPath;
    }

    public void setDatastoreName(String datastoreName) {
        this.datastoreName = datastoreName;
    }

    public void setCapacityBytes(Long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public void setBackingType(String backingType) {
        this.backingType = backingType;
    }

    public void setDiskMode(String diskMode) {
        this.diskMode = diskMode;
    }

    public void setRdmCompatibilityMode(String rdmCompatibilityMode) {
        this.rdmCompatibilityMode = rdmCompatibilityMode;
    }

    public void setIndependentDisk(boolean independentDisk) {
        this.independentDisk = independentDisk;
    }

    public void setPhysicalRdm(boolean physicalRdm) {
        this.physicalRdm = physicalRdm;
    }

    public void setChangeIdPresent(boolean changeIdPresent) {
        this.changeIdPresent = changeIdPresent;
    }
}
