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
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.VmwareCbtMigrationDisk;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VmwareCbtMigrationDisk.class)
public class VmwareCbtMigrationDiskResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the VMware CBT migration disk")
    private String id;

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

    @SerializedName(ApiConstants.TARGET_PATH)
    @Param(description = "the KVM target disk path after initial full sync")
    private String targetPath;

    @SerializedName(ApiConstants.TARGET_FORMAT)
    @Param(description = "the KVM target disk format")
    private String targetFormat;

    @SerializedName(ApiConstants.CHANGE_ID)
    @Param(description = "the VMware CBT change ID used for the next delta query")
    private String changeId;

    @SerializedName(ApiConstants.SNAPSHOT_MOR)
    @Param(description = "the VMware snapshot managed object reference currently associated with this disk")
    private String snapshotMor;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the disk replication state")
    private String state;

    public void setId(String id) {
        this.id = id;
    }

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

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public void setSnapshotMor(String snapshotMor) {
        this.snapshotMor = snapshotMor;
    }

    public void setState(String state) {
        this.state = state;
    }
}
