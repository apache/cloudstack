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

package org.apache.cloudstack.api.command.admin.acl.project;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRolePermissionResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.EnumUtils;

@APICommand(name = UpdateProjectRolePermissionCmd.APINAME, description = "Updates a project role permission and/or order", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {
        RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.15.0")
public class UpdateProjectRolePermissionCmd extends BaseCmd {
    public static final String APINAME = "updateProjectRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, required = true, entityType = ProjectRoleResponse.class,
            description = "ID of the project role", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRoleId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, required = true, entityType = ProjectResponse.class,
            description = "ID of project where project role permission is to be updated", validations = {ApiArgValidator.NotNullOrEmpty})
    private Long projectId;

    @Parameter(name = ApiConstants.RULE_ORDER, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = ProjectRolePermissionResponse.class,
            description = "The parent role permission uuid, use 0 to move this rule at the top of the list")
    private List<Long> projectRulePermissionOrder;

    @Parameter(name = ApiConstants.PROJECT_ROLE_PERMISSION_ID, type = CommandType.UUID, entityType = ProjectRolePermissionResponse.class,
            description = "Project Role permission rule id")
    private Long projectRuleId;

    @Parameter(name = ApiConstants.PERMISSION, type = CommandType.STRING,
            description = "Rule permission, can be: allow or deny")
    private String projectRolePermission;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public List<Long> getProjectRulePermissionOrder() {
        return projectRulePermissionOrder;
    }

    public Long getProjectRuleId() {
        return projectRuleId;
    }

    public Permission getProjectRolePermission() {
        if (this.projectRolePermission == null) {
            return null;
        }

        if (!EnumUtils.isValidEnum(Permission.class, projectRolePermission.toUpperCase())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Values for permission parameter should be: allow or deny");
        }

        return Permission.valueOf(projectRolePermission.toUpperCase());
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////////// API Implementation //////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        ProjectRole projectRole = projRoleService.findProjectRole(getProjectRoleId(), getProjectId());
        boolean result = false;
        if (projectRole == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role id provided");
        }
        if (getProjectRulePermissionOrder() != null) {
            if (getProjectRuleId() != null || getProjectRolePermission() != null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Parameters permission and ruleid must be mutually exclusive with ruleorder");
            }
            CallContext.current().setEventDetails("Reordering permissions for role id: " + projectRole.getId());
            result = updateProjectRolePermissionOrder(projectRole);

        } else if (getProjectRuleId() != null || getProjectRolePermission() != null ) {
            if (getProjectRulePermissionOrder() != null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Parameters permission and ruleid must be mutually exclusive with ruleorder");
            }
            ProjectRolePermission rolePermission = getValidProjectRolePermission();
            CallContext.current().setEventDetails("Updating project role permission for rule id: " + getProjectRuleId() + " to: " + getProjectRolePermission().toString());
            result = projRoleService.updateProjectRolePermission(projectId, projectRole, rolePermission, getProjectRolePermission());
        }
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }

    private ProjectRolePermission getValidProjectRolePermission() {
        ProjectRolePermission rolePermission = projRoleService.findProjectRolePermission(getProjectRuleId());
        if (rolePermission == null || rolePermission.getProjectId() != getProjectId()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Role permission doesn't exist in the project, probably because of invalid rule id");
        }
        return rolePermission;
    }

    private boolean updateProjectRolePermissionOrder(ProjectRole projectRole) {
        final List<ProjectRolePermission> rolePermissionsOrder = new ArrayList<>();
        for (Long rolePermissionId : getProjectRulePermissionOrder()) {
            final ProjectRolePermission rolePermission = projRoleService.findProjectRolePermission(rolePermissionId);
            if (rolePermission == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Provided project role permission(s) do not exist");
            }
            rolePermissionsOrder.add(rolePermission);
        }
        return projRoleService.updateProjectRolePermission(projectId, projectRole, rolePermissionsOrder);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
