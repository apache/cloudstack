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
import com.google.common.base.Strings;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.response.RolePermissionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = UpdateRolePermissionCmd.APINAME, description = "Updates a role permission", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class UpdateRolePermissionCmd extends BaseCmd {
    public static final String APINAME = "updateRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = RolePermissionResponse.class,
            description = "ID of the role permission", validations = {ApiArgValidator.PositiveNumber})
    private Long rolePermissionId;

    @Parameter(name = ApiConstants.RULE, type = CommandType.STRING, required = true,
            description = "The name of the API or wildcard rule such as list*", validations = {ApiArgValidator.NotNullOrEmpty})
    private String rule;

    @Parameter(name = ApiConstants.PERMISSION, type = CommandType.STRING, required = true, description = "The rule permission, allow or deny. Default: deny.")
    private String permission;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the role permission")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getRolePermissionId() {
        return rolePermissionId;
    }

    public Rule getRule() {
        return new Rule(rule);
    }

    public RolePermission.Permission getPermission() {
        if (Strings.isNullOrEmpty(permission)) {
            return null;
        }
        return RolePermission.Permission.valueOf(permission.toUpperCase());
    }

    public String getDescription() {
        return description;
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

    @Override
    public void execute() {
        RolePermission rolePermission = roleService.findRolePermission(getRolePermissionId());
        if (rolePermission == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role permission id provided");
        }
        CallContext.current().setEventDetails("Role permission id: " + rolePermission.getId() + ", rule:" + getRule() + ", permission: " + getPermission() + ", description: " + getDescription());
        boolean result = roleService.updateRolePermission(rolePermission, getRule(), getPermission(), getDescription());
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }
}