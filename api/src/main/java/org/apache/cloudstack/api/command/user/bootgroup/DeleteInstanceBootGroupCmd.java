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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "deleteInstanceBootGroup",
        description = "Deletes an Instance Boot Group",
        responseObject = SuccessResponse.class,
        entityType = {InstanceBootGroup.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteInstanceBootGroupCmd extends BaseCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, required = true, description = "The ID of the Instance Boot Group")
    private Long id;

    public Long getId() {
        return id;
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
        boolean result = instanceBootGroupService.deleteInstanceBootGroup(this);
        if (!result) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete instance boot group");
        }
        SuccessResponse response = new SuccessResponse(getCommandName());
        setResponseObject(response);
    }
}
