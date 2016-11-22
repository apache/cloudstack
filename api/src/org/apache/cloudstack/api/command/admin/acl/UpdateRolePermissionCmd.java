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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = UpdateRolePermissionCmd.APINAME, description = "Updates a role permission order", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0",
        authorized = {RoleType.Admin})
public class UpdateRolePermissionCmd extends BaseCmd {
    public static final String APINAME = "updateRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ROLE_ID, type = CommandType.UUID, required = true, entityType = RoleResponse.class,
            description = "ID of the role", validations = {ApiArgValidator.PositiveNumber})
    private Long roleId;

    @Parameter(name = ApiConstants.RULE_ORDER, type = CommandType.LIST, collectionType = CommandType.UUID, required = true, entityType = RolePermissionResponse.class,
            description = "The parent role permission uuid, use 0 to move this rule at the top of the list")
    private List<Long> rulePermissionOrder;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getRoleId() {
        return roleId;
    }

    public List<Long> getRulePermissionOrder() {
        return rulePermissionOrder;
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
        final Role role = roleService.findRole(getRoleId());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
        }
        CallContext.current().setEventDetails("Reordering permissions for role id: " + role.getId());
        final List<RolePermission> rolePermissionsOrder = new ArrayList<>();
        for (Long rolePermissionId : getRulePermissionOrder()) {
            final RolePermission rolePermission = roleService.findRolePermission(rolePermissionId);
            if (rolePermission == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Provided role permission(s) do not exist");
            }
            rolePermissionsOrder.add(rolePermission);
        }
        boolean result = roleService.updateRolePermission(role, rolePermissionsOrder);
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }
}