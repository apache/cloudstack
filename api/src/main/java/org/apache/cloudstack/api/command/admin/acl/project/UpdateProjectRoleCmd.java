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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectRoleResponse;

@APICommand(name = UpdateProjectRoleCmd.APINAME, description = "Creates a Project role", responseObject = ProjectRoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User}, since = "4.15.0")
public class UpdateProjectRoleCmd extends ProjectRoleCmd {
    public static final String APINAME = "updateProjectRole";

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = ProjectRoleResponse.class,
            description = "ID of the Project role", validations = {ApiArgValidator.PositiveNumber})
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING,
            description = "creates a project role with this unique name", validations = {ApiArgValidator.NotNullOrEmpty})
    private String projectRoleName;

    /////////////////////////////////////////////////////
    //////////////// Accessors //////////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getProjectRoleName() {
        return projectRoleName;
    }

    /////////////////////////////////////////////////////
    //////////////// API Implementation /////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ProjectRole role = projRoleService.findProjectRole(getId(), getProjectId());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid project role id provided");
        }
        role = projRoleService.updateProjectRole(role, getProjectId(), getProjectRoleName(), getProjectRoleDescription());
        setupProjectRoleResponse(role);

    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
