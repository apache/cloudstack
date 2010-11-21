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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SnapshotResponse extends BaseResponse {
    @SerializedName("id") @Param(description="ID of the snapshot")
    private Long id;

    @SerializedName("account") @Param(description="the account associated with the snapshot")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain ID of the snapshot's account")
    private Long domainId;

    @SerializedName("domain") @Param(description="the domain name of the snapshot's account")
    private String domainName;

    @SerializedName("snapshottype") @Param(description="the type of the snapshot")
    private String snapshotType;

    @SerializedName("volumeid") @Param(description="ID of the disk volume")
    private Long volumeId;

    @SerializedName("volumename") @Param(description="name of the disk volume")
    private String volumeName;

    @SerializedName("volumetype") @Param(description="type of the disk volume")
    private String volumeType;

    @SerializedName("created") @Param(description="	the date the snapshot was created")
    private Date created;

    @SerializedName("name") @Param(description="name of the snapshot")
    private String name;

    @SerializedName("jobid") @Param(description="the job ID associated with the snapshot. This is only displayed if the snapshot listed is part of a currently running asynchronous job.")
    private Long jobId;

    @SerializedName("jobstatus") @Param(description="the job status associated with the snapshot.  This is only displayed if the snapshot listed is part of a currently running asynchronous job.")
    private Integer jobStatus;

    @SerializedName("intervaltype") @Param(description="valid types are hourly, daily, weekly, monthy, template, and none.")
    private String intervalType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }
}
