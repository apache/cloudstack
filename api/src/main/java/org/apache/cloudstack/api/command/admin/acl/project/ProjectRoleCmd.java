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
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;

public abstract class ProjectRoleCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, required = true, entityType = ProjectResponse.class,
            description = "ID of project where role is being created", validations = {ApiArgValidator.NotNullOrEmpty})
    private Long projectId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = BaseCmd.CommandType.STRING, description = "The description of the Project role")
    private String projectRoleDescription;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectRoleDescription() {
        return projectRoleDescription;
    }

    protected void setupProjectRoleResponse(final ProjectRole role) {
        final ProjectRoleResponse response = new ProjectRoleResponse();
        response.setId(role.getUuid());
        response.setProjectId(_projectService.getProject(role.getProjectId()).getUuid());
        response.setRoleName(role.getName());
        response.setDescription(role.getDescription());
        response.setResponseName(getCommandName());
        response.setObjectName("projectrole");
        setResponseObject(response);
    }
}
