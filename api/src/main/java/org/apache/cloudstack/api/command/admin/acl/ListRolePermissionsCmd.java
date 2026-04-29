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

package org.apache.cloudstack.api.command.admin.acl;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RolePermissionResponse;
import org.apache.cloudstack.api.response.RoleResponse;

import java.util.ArrayList;
import java.util.List;


@APICommand(name = "listRolePermissions", description = "Lists role permissions", responseObject = RolePermissionResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class ListRolePermissionsCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ROLE_ID, type = CommandType.UUID, entityType = RoleResponse.class,
            description = "ID of the role", validations = {ApiArgValidator.PositiveNumber})
    private Long roleId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getRoleId() {
        return roleId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private void setupResponse(final List<RolePermission> rolePermissions, final Long roleId) {
        final Role roleProvided = roleService.findRole(roleId);
        final ListResponse<RolePermissionResponse> response = new ListResponse<>();
        final List<RolePermissionResponse> rolePermissionResponses = new ArrayList<>();
        for (final RolePermission rolePermission : rolePermissions) {
            final RolePermissionResponse rolePermissionResponse = new RolePermissionResponse();
            Role role = roleProvided;
            if (role == null) {
                role = roleService.findRole(rolePermission.getRoleId());
            }
            rolePermissionResponse.setRoleId(role.getUuid());
            rolePermissionResponse.setRoleName(role.getName());
            rolePermissionResponse.setId(rolePermission.getUuid());
            rolePermissionResponse.setRule(rolePermission.getRule());
            rolePermissionResponse.setRulePermission(rolePermission.getPermission());
            rolePermissionResponse.setDescription(rolePermission.getDescription());
            rolePermissionResponse.setObjectName("rolepermission");
            rolePermissionResponses.add(rolePermissionResponse);
        }
        response.setResponses(rolePermissionResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException {
        final List<RolePermission> rolePermissions = roleService.findAllPermissionsBy(getRoleId());
        setupResponse(rolePermissions, getRoleId());
    }
}
