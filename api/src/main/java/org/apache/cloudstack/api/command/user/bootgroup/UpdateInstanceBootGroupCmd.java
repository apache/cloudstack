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
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "updateInstanceBootGroup",
        description = "Updates an Instance Boot Group",
        since = "4.24.0",
        responseObject = InstanceBootGroupResponse.class,
        entityType = {InstanceBootGroup.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateInstanceBootGroupCmd extends BaseCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, required = true, description = "The ID of the Instance Boot Group")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "New name for the Instance Boot Group")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "New description for the Instance Boot Group")
    private String description;

    @Parameter(name = ApiConstants.READINESS_ATTEMPT_TIMEOUT_SECONDS, type = CommandType.LONG,
            description = "Per-boot-group override of the global timeout (seconds) for each readiness retry attempt. Pass -1 to clear the override and fall back to the global setting")
    private Long readinessAttemptTimeoutSeconds;

    @Parameter(name = ApiConstants.READINESS_MAX_RETRY_ATTEMPTS, type = CommandType.LONG,
            description = "Per-boot-group override of the global maximum number of readiness retry attempts. Pass -1 to clear the override and fall back to the global setting")
    private Long readinessMaxRetryAttempts;

    @Parameter(name = ApiConstants.READINESS_REBOOT_ON_RETRY, type = CommandType.BOOLEAN,
            description = "Per-boot-group override of whether an instance is rebooted between readiness retry attempts")
    private Boolean readinessRebootOnRetry;

    @Parameter(name = ApiConstants.READINESS_INITIAL_DELAY_SECONDS, type = CommandType.LONG,
            description = "Per-boot-group override of the global delay (seconds) after starting or rebooting an instance before its first readiness check of that attempt. Pass -1 to clear the override and fall back to the global setting")
    private Long readinessInitialDelaySeconds;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.InstanceBootGroup;
    }

    @Override
    public void execute() {
        InstanceBootGroup result = instanceBootGroupService.updateInstanceBootGroup(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update instance boot group");
        }
        InstanceBootGroupResponse response = instanceBootGroupService.createInstanceBootGroupResponse(result.getId());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
