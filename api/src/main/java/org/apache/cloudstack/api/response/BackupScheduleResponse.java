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
import org.apache.cloudstack.backup.BackupSchedule;

import com.cloud.serializer.Param;
import com.cloud.utils.DateUtil;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = BackupSchedule.class)
public class BackupScheduleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the backup schedule.", since = "4.21.0")
    private String id;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "Name of the Instance")
    private String vmName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "ID of the Instance")
    private String vmId;

    @SerializedName(ApiConstants.SCHEDULE)
    @Param(description = "The time the backup is scheduled to be taken.")
    private String schedule;

    @SerializedName(ApiConstants.INTERVAL_TYPE)
    @Param(description = "The interval type of the backup schedule")
    private DateUtil.IntervalType intervalType;

    @SerializedName(ApiConstants.TIMEZONE)
    @Param(description = "The time zone of the backup schedule")
    private String timezone;

    @SerializedName(ApiConstants.MAX_BACKUPS)
    @Param(description = "maximum number of backups retained")
    private Integer maxBackups;

    @SerializedName(ApiConstants.ISOLATED)
    @Param(description = ApiConstants.PARAMETER_DESCRIPTION_ISOLATED_BACKUPS)
    private boolean isolated;

    @SerializedName(ApiConstants.QUIESCE_VM)
    @Param(description = "quiesce the instance before checkpointing the disks for backup")
    private Boolean quiesceVM;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account that the backup schedule is associated with")
    private String account;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the ID of the account that the backup schedule is associated with")
    private String accountId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the backup schedule")
    private String projectName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project ID of the backup schedule")
    private String projectId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain that the backup schedule is associated with")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID that the backup schedule is associated with")
    private String domainid;

    public void setId(String id) {
        this.id = id;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public DateUtil.IntervalType getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(DateUtil.IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setMaxBackups(Integer maxBackups) {
        this.maxBackups = maxBackups;
    }

    public void setQuiesceVM(Boolean quiesceVM) {
        this.quiesceVM = quiesceVM;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public String getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomainid() {
        return domainid;
    }

    public void setDomainid(String domainid) {
        this.domainid = domainid;
    }
}
