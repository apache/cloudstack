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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;

@APICommand(name = "createRole", description = "Creates a role", responseObject = RoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class CreateRoleCmd extends RoleCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Creates a role with this unique name")
    private String roleName;

    @Parameter(name = ApiConstants.ROLE_ID, type = CommandType.UUID, entityType = RoleResponse.class,
            description = "ID of the role to be cloned from. Either roleid or type must be passed in")
    private Long roleId;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "Indicates whether the role will be visible to all users (public) or only to root admins (private)." +
            " If this parameter is not specified during the creation of the role its value will be defaulted to true (public).")
    private boolean publicRole = true;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getRoleName() {
        return roleName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public boolean isPublicRole() {
        return publicRole;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        validateRoleParameters();

        Role role = null;
        if (getRoleId() != null) {
            Role existingRole = roleService.findRole(getRoleId());
            if (existingRole == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
            }

            CallContext.current().setEventDetails("Role: " + getRoleName() + ", from role: " + getRoleId() + ", description: " + getRoleDescription());
            role = roleService.createRole(getRoleName(), existingRole, getRoleDescription(), isPublicRole());
        } else {
            CallContext.current().setEventDetails("Role: " + getRoleName() + ", type: " + getRoleType() + ", description: " + getRoleDescription());
            role = roleService.createRole(getRoleName(), getRoleType(), getRoleDescription(), isPublicRole());
        }

        if (role == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create role");
        }
        setupResponse(role);
    }

    private void validateRoleParameters() {
        if (getRoleType() == null && getRoleId() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Neither role type nor role ID is provided");
        }

        if (getRoleType() != null && getRoleId() != null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Both role type and role ID should not be specified");
        }

        if (getRoleId() != null && getRoleId() < 1L) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Role;
    }
}
