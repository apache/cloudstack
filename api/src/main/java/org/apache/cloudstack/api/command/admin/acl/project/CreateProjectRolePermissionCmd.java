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

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.acl.BaseRolePermissionCmd;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRolePermissionResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = CreateProjectRolePermissionCmd.APINAME, description = "Adds API permissions to a project role", responseObject = ProjectRolePermissionResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {
        RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.15.0")
public class CreateProjectRolePermissionCmd extends BaseRolePermissionCmd {
    public static final String APINAME = "createProjectRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, required = true, entityType = ProjectRoleResponse.class,
            description = "ID of the project role", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRoleId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, required = true, entityType = ProjectResponse.class,
            description = "ID of project where project role permission is to be created", validations = {ApiArgValidator.NotNullOrEmpty})
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ProjectRole projectRole = projRoleService.findProjectRole(getProjectRoleId(), getProjectId());
        if (projectRole == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid project role ID provided");
        }
        CallContext.current().setEventDetails("Project Role ID: " + projectRole.getId() + ", Rule:" + getRule() + ", Permission: " + getPermission() + ", Description: " + getDescription());
        final ProjectRolePermission projectRolePermission = projRoleService.createProjectRolePermission(this);
        if (projectRolePermission == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create project role permission");
        }
        setupResponse(projectRolePermission, projectRole);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    private void setupResponse(final ProjectRolePermission rolePermission, final ProjectRole role) {
        final ProjectRolePermissionResponse response = new ProjectRolePermissionResponse();
        response.setId(rolePermission.getUuid());
        response.setProjectId(_projectService.getProject(rolePermission.getProjectId()).getUuid());
        response.setProjectRoleId(role.getUuid());
        response.setRule(rolePermission.getRule());
        response.setRulePermission(rolePermission.getPermission());
        response.setDescription(rolePermission.getDescription());
        response.setResponseName(getCommandName());
        response.setObjectName("projectrolepermission");
        setResponseObject(response);
    }
}
