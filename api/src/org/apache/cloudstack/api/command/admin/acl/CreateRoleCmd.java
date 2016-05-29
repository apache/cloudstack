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

import com.cloud.user.Account;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = CreateRoleCmd.APINAME, description = "Creates a role", responseObject = RoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class CreateRoleCmd extends BaseCmd {
    public static final String APINAME = "createRole";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "creates a role with this unique name", validations = {ApiArgValidator.NotNullOrEmpty})
    private String roleName;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true,
            description = "The type of the role, valid options are: Admin, ResourceAdmin, DomainAdmin, User",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String roleType;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the role")
    private String roleDescription;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getRoleName() {
        return roleName;
    }

    public RoleType getRoleType() {
        return RoleType.fromString(roleType);
    }

    public String getRoleDescription() {
        return roleDescription;
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

    private void setupResponse(final Role role) {
        final RoleResponse response = new RoleResponse();
        response.setId(role.getUuid());
        response.setRoleName(role.getName());
        response.setRoleType(role.getRoleType());
        response.setResponseName(getCommandName());
        response.setObjectName("role");
        setResponseObject(response);
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Role: " + getRoleName() + ", type:" + getRoleType() + ", description: " + getRoleDescription());
        final Role role = roleService.createRole(getRoleName(), getRoleType(), getRoleDescription());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create role");
        }
        setupResponse(role);
    }
}
