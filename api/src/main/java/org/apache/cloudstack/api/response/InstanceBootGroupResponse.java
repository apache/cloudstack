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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = InstanceBootGroup.class)
public class InstanceBootGroupResponse extends BaseResponse implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the instance boot group")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the instance boot group")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the instance boot group")
    private String description;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date the instance boot group was created")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The account owning the instance boot group")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "The account ID owning the instance boot group")
    private String accountId;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the instance boot group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain name of the instance boot group")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "The path of the domain the instance boot group belongs to")
    private String domainPath;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the instance boot group")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the instance boot group")
    private String projectName;

    @SerializedName(ApiConstants.READINESS_ATTEMPT_TIMEOUT_SECONDS)
    @Param(description = "Effective timeout in seconds for each readiness retry attempt (per-boot-group override if set, else the global default)")
    private long readinessAttemptTimeoutSeconds;

    @SerializedName(ApiConstants.READINESS_MAX_RETRY_ATTEMPTS)
    @Param(description = "Effective maximum number of readiness retry attempts (per-boot-group override if set, else the global default)")
    private long readinessMaxRetryAttempts;

    @SerializedName(ApiConstants.READINESS_REBOOT_ON_RETRY)
    @Param(description = "Effective setting for whether an instance is rebooted between readiness retry attempts (per-boot-group override if set, else the global default)")
    private boolean readinessRebootOnRetry;

    @SerializedName(ApiConstants.READINESS_INITIAL_DELAY_SECONDS)
    @Param(description = "Effective delay in seconds after starting or rebooting an instance before its first readiness check of that attempt (per-boot-group override if set, else the global default)")
    private long readinessInitialDelaySeconds;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setReadinessAttemptTimeoutSeconds(long readinessAttemptTimeoutSeconds) {
        this.readinessAttemptTimeoutSeconds = readinessAttemptTimeoutSeconds;
    }

    public void setReadinessMaxRetryAttempts(long readinessMaxRetryAttempts) {
        this.readinessMaxRetryAttempts = readinessMaxRetryAttempts;
    }

    public void setReadinessRebootOnRetry(boolean readinessRebootOnRetry) {
        this.readinessRebootOnRetry = readinessRebootOnRetry;
    }

    public void setReadinessInitialDelaySeconds(long readinessInitialDelaySeconds) {
        this.readinessInitialDelaySeconds = readinessInitialDelaySeconds;
    }
}
