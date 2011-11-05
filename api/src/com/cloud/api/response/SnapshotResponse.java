/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.cloud.storage.Snapshot;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class SnapshotResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the snapshot")
    private IdentityProxy id = new IdentityProxy("snapshots");

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the snapshot")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the snapshot's account")
    private IdentityProxy domainId = new IdentityProxy("domain");
    
    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the snapshot's account")
    private String domainName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the snapshot")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the snapshot")
    private String projectName;

    @SerializedName(ApiConstants.SNAPSHOT_TYPE)
    @Param(description = "the type of the snapshot")
    private String snapshotType;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "ID of the disk volume")
    private IdentityProxy volumeId = new IdentityProxy("volumes");

    @SerializedName(ApiConstants.VOLUME_NAME)
    @Param(description = "name of the disk volume")
    private String volumeName;

    @SerializedName("volumetype")
    @Param(description = "type of the disk volume")
    private String volumeType;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "	the date the snapshot was created")
    private Date created;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the snapshot")
    private String name;

    @SerializedName(ApiConstants.INTERVAL_TYPE)
    @Param(description = "valid types are hourly, daily, weekly, monthy, template, and none.")
    private String intervalType;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the snapshot. BackedUp means that snapshot is ready to be used; Creating - the snapshot is being allocated on the primary storage; BackingUp - the snapshot is being backed up on secondary storage")
    private Snapshot.Status state;

    @Override
    public Long getObjectId() {
        return getId();
    }
   
    private Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId.getValue();
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId.setValue(volumeId);
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

    public void setState(Snapshot.Status state) {
        this.state = state;
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
