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
import org.apache.cloudstack.backup.Backup;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

@EntityReference(value = Backup.class)
public class BackupResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the VM backup")
    private String id;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "ID of the VM")
    private String vmId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "name of the VM")
    private String vmName;

    @SerializedName(ApiConstants.EXTERNAL_ID)
    @Param(description = "external backup id")
    private String externalId;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "backup type")
    private String type;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "backup date")
    private Date date;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "backup size in bytes")
    private Long size;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "backup protected (virtual) size in bytes")
    private Long protectedSize;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "backup status")
    private Backup.Status status;

    @SerializedName(ApiConstants.VOLUMES)
    @Param(description = "backed up volumes")
    private String volumes;

    @SerializedName(ApiConstants.BACKUP_OFFERING_ID)
    @Param(description = "backup offering id")
    private String backupOfferingId;

    @SerializedName(ApiConstants.BACKUP_OFFERING_NAME)
    @Param(description = "backup offering name")
    private String backupOfferingName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "account id")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "account name")
    private String account;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "domain id")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "domain name")
    private String domain;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE)
    @Param(description = "zone name")
    private String zone;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Backup.Status getStatus() {
        return status;
    }

    public void setStatus(Backup.Status status) {
        this.status = status;
    }

    public String getVolumes() {
        return volumes;
    }

    public void setVolumes(String volumes) {
        this.volumes = volumes;
    }

    public String getBackupOfferingId() {
        return backupOfferingId;
    }

    public void setBackupOfferingId(String backupOfferingId) {
        this.backupOfferingId = backupOfferingId;
    }

    public String getBackupOffering() {
        return backupOfferingName;
    }

    public void setBackupOffering(String backupOfferingName) {
        this.backupOfferingName = backupOfferingName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
