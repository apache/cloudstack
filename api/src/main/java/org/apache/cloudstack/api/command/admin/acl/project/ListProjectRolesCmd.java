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
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = ListProjectRolesCmd.APINAME, description = "Lists Project roles in CloudStack", responseObject = ProjectRoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.15.0", authorized = {
        RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListProjectRolesCmd extends BaseCmd {
    public static final String APINAME = "listProjectRoles";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, entityType = ProjectRoleResponse.class, description = "List project role by project role ID.")
    private Long projectRoleId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "List project role by project ID.")
    private Long projectId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List project role by project role name.")
    private String roleName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getProjectRoleId() { return projectRoleId; }

    public Long getProjectId() {
        return projectId;
    }

    public String getRoleName() {
        return roleName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        List<ProjectRole> projectRoles;
        if (getProjectId() != null && getProjectRoleId() != null) {
            projectRoles = Collections.singletonList(projRoleService.findProjectRole(getProjectRoleId(), getProjectId()));
        } else if (StringUtils.isNotBlank(getRoleName())) {
            projectRoles = projRoleService.findProjectRolesByName(getProjectId(), getRoleName());
        } else {
            projectRoles = projRoleService.findProjectRoles(getProjectId());
        }
        final ListResponse<ProjectRoleResponse> response = new ListResponse<>();
        final List<ProjectRoleResponse> roleResponses = new ArrayList<>();
        for (ProjectRole role : projectRoles) {
            if (role == null) {
                continue;
            }
            roleResponses.add(setupProjectRoleResponse(role));
        }
        response.setResponses(roleResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    private ProjectRoleResponse setupProjectRoleResponse(final ProjectRole role) {
        final ProjectRoleResponse response = new ProjectRoleResponse();
        response.setId(role.getUuid());
        response.setProjectId(_projectService.getProject(role.getProjectId()).getUuid());
        response.setRoleName(role.getName());
        response.setDescription(role.getDescription());
        response.setObjectName("projectrole");
        return response;
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
