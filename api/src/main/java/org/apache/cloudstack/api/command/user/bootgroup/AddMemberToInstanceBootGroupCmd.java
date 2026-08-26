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
import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "addMemberToInstanceBootGroup",
        description = "Adds a VM or instance group to an instance boot group. Exactly one of virtualmachineid or instancegroupid must be specified.",
        responseObject = InstanceBootGroupMemberResponse.class,
        entityType = {InstanceBootGroupMember.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class AddMemberToInstanceBootGroupCmd extends BaseCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, required = true, description = "The ID of the instance boot group")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "The ID of the VM to add (exclusive with instancegroupid)")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.INSTANCE_GROUP_ID, type = CommandType.UUID, entityType = InstanceGroupResponse.class, description = "The ID of the instance group to add (exclusive with virtualmachineid)")
    private Long instanceGroupId;

    @Parameter(name = ApiConstants.BOOT_ORDER, type = CommandType.INTEGER, required = true,
            description = "The boot order value for this member (0 or greater; non-contiguous values are allowed). "
                    + "Any existing member already at or past this value is shifted one slot later to make room.")
    private int order;

    public Long getId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getInstanceGroupId() {
        return instanceGroupId;
    }

    public int getOrder() {
        return order;
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
        InstanceBootGroupMember result = instanceBootGroupService.addMemberToInstanceBootGroup(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add member to instance boot group");
        }
        InstanceBootGroupMemberResponse response = instanceBootGroupService.createInstanceBootGroupMemberResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
