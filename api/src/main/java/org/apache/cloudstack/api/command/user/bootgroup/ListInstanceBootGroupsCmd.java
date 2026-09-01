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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "listInstanceBootGroups",
        description = "Lists Instance Boot Groups",
        responseObject = InstanceBootGroupResponse.class,
        entityType = {InstanceBootGroup.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListInstanceBootGroupsCmd extends BaseListProjectAndAccountResourcesCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, description = "List Instance Boot Group by ID")
    private Long id;

    @Parameter(name = ApiConstants.KEYWORD, type = CommandType.STRING, description = "List Instance Boot Groups by name keyword")
    private String keyword;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "List Instance Boot Groups that contain the Instance")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.INSTANCE_GROUP_ID, type = CommandType.UUID, entityType = InstanceGroupResponse.class, description = "List Instance Boot Groups that contain the Instance Group")
    private Long instanceGroupId;

    public Long getId() {
        return id;
    }

    @Override
    public String getKeyword() {
        return keyword;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getInstanceGroupId() {
        return instanceGroupId;
    }

    @Override
    public void execute() {
        ListResponse<InstanceBootGroupResponse> response = instanceBootGroupService.listInstanceBootGroups(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
