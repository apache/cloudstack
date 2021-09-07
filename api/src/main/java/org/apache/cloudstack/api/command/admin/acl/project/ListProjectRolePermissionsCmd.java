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

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRolePermissionResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = ListProjectRolePermissionsCmd.APINAME, description = "Lists a project's project role permissions", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {
        RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.15.0")
public class ListProjectRolePermissionsCmd extends BaseCmd {
    public static final String APINAME = "listProjectRolePermissions";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "ID of the project")
    private Long projectId;

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, entityType = ProjectRoleResponse.class,
            description = "ID of the project role", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRoleId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public void execute() {
        List<ProjectRolePermission> projectRolePermissions = projRoleService.findAllProjectRolePermissions(getProjectId(), getProjectRoleId());
        final ProjectRole projectRole = projRoleService.findProjectRole(getProjectRoleId(), getProjectId());
        final ListResponse<ProjectRolePermissionResponse> response = new ListResponse<>();
        final List<ProjectRolePermissionResponse> rolePermissionResponses = new ArrayList<>();
        for (final ProjectRolePermission rolePermission : projectRolePermissions) {
            ProjectRole role = projectRole;
            if (role == null) {
                role = projRoleService.findProjectRole(rolePermission.getProjectRoleId(), rolePermission.getProjectId());
            }
            rolePermissionResponses.add(setupResponse(role, rolePermission));
        }
        response.setResponses(rolePermissionResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    private ProjectRolePermissionResponse setupResponse(ProjectRole role, ProjectRolePermission rolePermission) {
        final ProjectRolePermissionResponse rolePermissionResponse = new ProjectRolePermissionResponse();
        rolePermissionResponse.setProjectId(_projectService.getProject(rolePermission.getProjectId()).getUuid());
        rolePermissionResponse.setProjectRoleId(role.getUuid());
        rolePermissionResponse.setProjectRoleName(role.getName());
        rolePermissionResponse.setId(rolePermission.getUuid());
        rolePermissionResponse.setRule(rolePermission.getRule());
        rolePermissionResponse.setRulePermission(rolePermission.getPermission());
        rolePermissionResponse.setDescription(rolePermission.getDescription());
        rolePermissionResponse.setObjectName("projectrolepermission");
       return rolePermissionResponse;
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
