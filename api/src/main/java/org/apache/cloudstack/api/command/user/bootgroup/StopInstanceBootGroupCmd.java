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
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

import com.cloud.event.EventTypes;

@APICommand(name = "stopInstanceBootGroup",
        description = "Stops all Instances in an Instance Boot Group in reverse order (highest boot order first). Continues through all tiers even if some Instances fail to stop",
        responseObject = InstanceBootGroupResponse.class,
        entityType = {InstanceBootGroup.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class StopInstanceBootGroupCmd extends BaseAsyncCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, required = true, description = "The ID of the Instance Boot Group")
    private Long id;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false,
            description = "Force stop every Instance in the Instance Boot Group (marked as Stopped even when the stop command fails to be sent to the backend, otherwise a force poweroff is attempted)")
    private Boolean forced;

    public Long getId() {
        return id;
    }

    public boolean isForced() {
        return forced != null && forced;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_INSTANCE_BOOT_GROUP_STOP;
    }

    @Override
    public String getEventDescription() {
        return "Stopping instance boot group with ID: " + getResourceUuid(ApiConstants.ID);
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
        InstanceBootGroup result = instanceBootGroupService.stopInstanceBootGroup(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to stop instance boot group");
        }
        InstanceBootGroupResponse response = instanceBootGroupService.createInstanceBootGroupResponse(result.getId());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
