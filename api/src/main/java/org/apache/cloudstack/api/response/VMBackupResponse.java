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
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.VMBackup;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VMBackup.class)
public class VMBackupResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "internal id of the backup")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id")
    private String zoneId;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "account id")
    private String accountId;

    @SerializedName(ApiConstants.EXTERNAL_ID)
    @Param(description = "external backup id")
    private String externalId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "backup name")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "backup description")
    private String description;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "backup vm id")
    private String vmId;

    @SerializedName(ApiConstants.VOLUME_IDS)
    @Param(description = "backup volume ids")
    private List<String> volumeIds;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "backup volume ids")
    private VMBackup.Status status;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "backup size in bytes")
    private Long size;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "backup protected (virtual) size in bytes")
    private Long protectedSize;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "backup creation date")
    private Date createdDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public VMBackup.Status getStatus() {
        return status;
    }

    public void setStatus(VMBackup.Status status) {
        this.status = status;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getProtectedSize() {
        return protectedSize;
    }

    public void setProtectedSize(Long protectedSize) {
        this.protectedSize = protectedSize;
    }

    public List<String> getVolumeIds() {
        return volumeIds;
    }

    public void setVolumeIds(List<String> volumeIds) {
        this.volumeIds = volumeIds;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
