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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = DeleteProjectRolePermissionCmd.APINAME, description = "Deletes a project role permission in the project", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {
        RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.15.0")
public class DeleteProjectRolePermissionCmd extends BaseCmd {
    public static final String APINAME = "deleteProjectRolePermission";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ID, type = BaseCmd.CommandType.UUID, required = true, entityType = ProjectResponse.class,
            description = "ID of the project where the project role permission is to be deleted", validations = {ApiArgValidator.PositiveNumber})
    private Long projectId;

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, required = true, entityType = ProjectRolePermissionResponse.class,
            description = "ID of the project role permission to be deleted", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRolePermissionId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public Long getProjectRolePermissionId() {
        return projectRolePermissionId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ProjectRolePermission rolePermission = projRoleService.findProjectRolePermission(getProjectRolePermissionId());
        if (rolePermission == null || rolePermission.getProjectId() != getProjectId()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role permission id provided for the project");
        }
        CallContext.current().setEventDetails("Deleting Project Role permission with id: " + rolePermission.getId());
        boolean result = projRoleService.deleteProjectRolePermission(rolePermission);
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(result);
        setResponseObject(response);
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
