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

package org.apache.cloudstack.api.command.user.bootgroup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "createInstanceBootGroup",
        description = "Creates an Instance Boot Group",
        responseObject = InstanceBootGroupResponse.class,
        entityType = {InstanceBootGroup.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateInstanceBootGroupCmd extends BaseCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "The name of the Instance Boot Group")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the Instance Boot Group")
    private String description;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "The account of the Instance Boot Group. Must be used with domainid")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "The domain ID of the account owning the Instance Boot Group")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "The project of the Instance Boot Group")
    private Long projectId;

    @Parameter(name = ApiConstants.READINESS_ATTEMPT_TIMEOUT_SECONDS, type = CommandType.LONG,
            description = "Per-boot-group override of the global timeout (seconds) for each readiness retry attempt")
    private Long readinessAttemptTimeoutSeconds;

    @Parameter(name = ApiConstants.READINESS_MAX_RETRY_ATTEMPTS, type = CommandType.LONG,
            description = "Per-boot-group override of the global maximum number of readiness retry attempts")
    private Long readinessMaxRetryAttempts;

    @Parameter(name = ApiConstants.READINESS_REBOOT_ON_RETRY, type = CommandType.BOOLEAN,
            description = "Per-boot-group override of whether an instance is rebooted between readiness retry attempts")
    private Boolean readinessRebootOnRetry;

    @Parameter(name = ApiConstants.READINESS_INITIAL_DELAY_SECONDS, type = CommandType.LONG,
            description = "Per-boot-group override of the global delay (seconds) after starting or rebooting an instance before its first readiness check of that attempt")
    private Long readinessInitialDelaySeconds;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getReadinessAttemptTimeoutSeconds() {
        return readinessAttemptTimeoutSeconds;
    }

    public Long getReadinessMaxRetryAttempts() {
        return readinessMaxRetryAttempts;
    }

    public Boolean getReadinessRebootOnRetry() {
        return readinessRebootOnRetry;
    }

    public Long getReadinessInitialDelaySeconds() {
        return readinessInitialDelaySeconds;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalizeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.InstanceBootGroup;
    }

    @Override
    public void execute() {
        InstanceBootGroup result = instanceBootGroupService.createInstanceBootGroup(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create instance boot group");
        }
        InstanceBootGroupResponse response = instanceBootGroupService.createInstanceBootGroupResponse(result.getId());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
