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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EntityReference(value = BackupReportResponse.class)
public class BackupReportResponse extends BaseResponse {
    @SerializedName(ApiConstants.DOMAIN_REPORT)
    @Param(description = "List of backup reports per domain.")
    private List<BackupReportDomainResponse> backupReportDomainResponseList;

    @SerializedName(ApiConstants.SCHEDULE_REPORT)
    @Param(description = "List of scheduled backups to be taken.")
    private List<BackupScheduleResponse> backupScheduleResponseList;

    @SerializedName(ApiConstants.PROVIDER_INFO)
    @Param(description = "Provider information.")
    private List<Object> providerInfo;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Start date of the report.")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "End date of the report.")
    private Date endDate;

    @SerializedName(ApiConstants.BACKUP_STORAGE_TOTAL)
    @Param(description = "Backup storage usage increase for the whole environment during the period.")
    private Double storageUsage;

    public BackupReportResponse(Date startDate, Date endDate) {
        this.backupReportDomainResponseList = new ArrayList<>();
        this.backupScheduleResponseList = new ArrayList<>();
        this.providerInfo = new ArrayList<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.storageUsage = 0D;
    }

    public List<BackupReportDomainResponse> getBackupReportDomainResponseList() {
        return backupReportDomainResponseList;
    }

    public void addBackupReportDomainResponse(BackupReportDomainResponse response) {
        this.backupReportDomainResponseList.add(response);
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Double getStorageUsage() {
        return storageUsage;
    }

    public void addStorageUsage(double storageUsage) {
        this.storageUsage += storageUsage;
    }

    public List<BackupScheduleResponse> getBackupScheduleResponseList() {
        return backupScheduleResponseList;
    }

    public void addBackupScheduleResponse(BackupScheduleResponse backupScheduleResponse) {
        this.backupScheduleResponseList.add(backupScheduleResponse);
    }

    public List<Object> getProviderInfo() {
        return providerInfo;
    }

    public void addProviderInfo(List<Object> providerInfo) {
        this.providerInfo.addAll(providerInfo);
    }
}
