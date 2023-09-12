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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.Snapshot;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Snapshot.class)
public class SnapshotResponse extends BaseResponseWithTagInformation implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the snapshot")
    private String id;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the snapshot")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the snapshot's account")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the snapshot's account")
    private String domainName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the snapshot")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the snapshot")
    private String projectName;

    @SerializedName(ApiConstants.SNAPSHOT_TYPE)
    @Param(description = "the type of the snapshot")
    private String snapshotType;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "ID of the disk volume")
    private String volumeId;

    @SerializedName(ApiConstants.VOLUME_NAME)
    @Param(description = "name of the disk volume")
    private String volumeName;

    @SerializedName("volumetype")
    @Param(description = "type of the disk volume")
    private String volumeType;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "  the date the snapshot was created")
    private Date created;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the snapshot")
    private String name;

    @SerializedName(ApiConstants.INTERVAL_TYPE)
    @Param(description = "valid types are hourly, daily, weekly, monthy, template, and none.")
    private String intervalType;

    @SerializedName(ApiConstants.LOCATION_TYPE)
    @Param(description = "valid location types are primary and secondary.")
    private String locationType;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the snapshot. BackedUp means that snapshot is ready to be used; Creating - the snapshot is being allocated on the primary storage; BackingUp - the snapshot is being backed up on secondary storage")
    private Snapshot.State state;

    @SerializedName(ApiConstants.PHYSICAL_SIZE)
    @Param(description = "physical size of backedup snapshot on image store")
    private long physicalSize;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "id of the availability zone")
    private String zoneId;

    @SerializedName(ApiConstants.REVERTABLE)
    @Param(description = "indicates whether the underlying storage supports reverting the volume to this snapshot")
    private boolean revertable;

    @SerializedName(ApiConstants.OS_TYPE_ID)
    @Param(description = "id of the os on volume", since = "4.10")
    private String osTypeId;

    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "display name of the os on volume")
    private String osDisplayName;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "virtual size of backedup snapshot on image store")
    private long virtualSize;

    public SnapshotResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

    private String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public void setState(Snapshot.State state) {
        this.state = state;
    }

    public void setPhysicaSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setRevertable(boolean revertable) {
        this.revertable = revertable;
    }

    public String getOsTypeId() {
        return osTypeId;
    }

    public void setOsTypeId(String osTypeId) {
        this.osTypeId = osTypeId;
    }

    public void setOsDisplayName(String osDisplayName) {
        this.osDisplayName = osDisplayName;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }
}
