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

import java.util.ArrayList;
import java.util.List;

public class BackupReportAccountResponse extends BaseResponse {

    @SerializedName(ApiConstants.SUCCESSFUL_BACKUP)
    @Param(description = "Successful backup.")
    private List<BackupResponse> successfulBackups;

    @SerializedName(ApiConstants.FAILED_BACKUP)
    @Param(description = "Failed backup.")
    private List<BackupResponse> failedBackups;

    @SerializedName(ApiConstants.DELETED_BACKUP)
    @Param(description = "Deleted backup.")
    private List<BackupResponse> deletedBackups;

    @SerializedName(ApiConstants.BACKUP_STORAGE_TOTAL)
    @Param(description = "Backup storage usage increase for the Account during the period.")
    private Double storageUsage;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "Account ID.")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "Account name.")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "Project ID.")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "Project name.")
    private String projectName;

    public BackupReportAccountResponse() {
        this.successfulBackups = new ArrayList<>();
        this.failedBackups = new ArrayList<>();
        this.deletedBackups = new ArrayList<>();
        this.storageUsage = 0D;
    }

    public void addSuccessfulBackup(BackupResponse successfulBackup) {
        this.successfulBackups.add(successfulBackup);
    }

    public void addFailedBackup(BackupResponse failedBackup) {
        this.failedBackups.add(failedBackup);
    }

    public void addDeletedBackup(BackupResponse deletedBackup) {
        this.deletedBackups.add(deletedBackup);
    }

    public List<BackupResponse> getSuccessfulBackups() {
        return successfulBackups;
    }

    public List<BackupResponse> getFailedBackups() {
        return failedBackups;
    }

    public List<BackupResponse> getDeletedBackups() {
        return deletedBackups;
    }

    public Double getStorageUsage() {
        return storageUsage;
    }

    public void addStorageUsage(double storageUsage) {
        this.storageUsage += storageUsage;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
