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
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;

@APICommand(name = "updateRole", description = "Updates a role", responseObject = RoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class UpdateRoleCmd extends RoleCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, required = true, entityType = RoleResponse.class,
            description = "ID of the role", validations = {ApiArgValidator.PositiveNumber})
    private Long roleId;

    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING, description = "creates a role with this unique name")
    private String roleName;

    @Parameter(name = ApiConstants.DESCRIPTION, type = BaseCmd.CommandType.STRING, description = "The description of the role")
    private String roleDescription;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "Indicates whether the role will be visible to all users (public) or only to root admins (private).")
    private Boolean publicRole;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public Boolean isPublicRole() {
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
        Role role = roleService.findRole(getRoleId());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
        }
        CallContext.current().setEventDetails("Role: " + getRoleName() + ", type:" + getRoleType() + ", description: " + getRoleDescription());
        role = roleService.updateRole(role, getRoleName(), getRoleType(), getRoleDescription(), isPublicRole());
        setupResponse(role);
    }

    @Override
    public Long getApiResourceId() {
        return getRoleId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Role;
    }
}
