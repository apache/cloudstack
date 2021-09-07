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

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RolePermissionResponse;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;

@APICommand(name = CreateRolePermissionCmd.APINAME, description = "Adds an API permission to a role", responseObject = RolePermissionResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class CreateRolePermissionCmd extends BaseRolePermissionCmd {
    public static final String APINAME = "createRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ROLE_ID, type = CommandType.UUID, required = true, entityType = RoleResponse.class,
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
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private void setupResponse(final RolePermission rolePermission, final Role role) {
        final RolePermissionResponse response = new RolePermissionResponse();
        response.setId(rolePermission.getUuid());
        response.setRoleId(role.getUuid());
        response.setRule(rolePermission.getRule());
        response.setRulePermission(rolePermission.getPermission());
        response.setDescription(rolePermission.getDescription());
        response.setResponseName(getCommandName());
        response.setObjectName("rolepermission");
        setResponseObject(response);
    }

    @Override
    public void execute() {
        final Role role = roleService.findRole(getRoleId());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
        }
        CallContext.current().setEventDetails("Role id: " + role.getId() + ", rule:" + getRule() + ", permission: " + getPermission() + ", description: " + getDescription());
        final RolePermission rolePermission = roleService.createRolePermission(role, getRule(), getPermission(), getDescription());
        if (rolePermission == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create role permission");
        }
        setupResponse(rolePermission, role);
     }
}
