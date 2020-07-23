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

package org.apache.cloudstack.api.command.user.account;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.EnumUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.ProjectAccount;
import com.google.common.base.Strings;

@APICommand(name = AddUserToProjectCmd.APINAME, description = "Adds user to a project", responseObject = SuccessResponse.class, since = "4.14",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class AddUserToProjectCmd extends BaseAsyncCmd {
    public static final String APINAME = "addUserToProject";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = ProjectResponse.class,
            required = true,
            description = "ID of the project to add the user to")
    private Long projectId;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Name of the user to be added to the project")
    private String username;

    @Parameter(name = ApiConstants.EMAIL, type = CommandType.STRING, description = "email ID of user to which invitation to the project is going to be sent")
    private String email;

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = BaseCmd.CommandType.UUID, entityType = ProjectRoleResponse.class,
            description = "ID of the project role", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRoleId;

    @Parameter(name = ApiConstants.ROLE_TYPE, type = BaseCmd.CommandType.STRING,
            description = "Project role type to be assigned to the user - Admin/Regular")
    private String roleType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() { return email; }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public ProjectAccount.Role getRoleType() {
        if (!Strings.isNullOrEmpty(roleType)) {
            String role = roleType.substring(0, 1).toUpperCase() + roleType.substring(1).toLowerCase();
            if (!EnumUtils.isValidEnum(ProjectAccount.Role.class, role)) {
                throw new InvalidParameterValueException("Only Admin or Regular project role types are valid");
            }
            return Enum.valueOf(ProjectAccount.Role.class, role);
        }
        return null;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_USER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding user "+getUsername()+" to Project "+getProjectId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute()  {
        validateInput();
        boolean result = _projectService.addUserToProject(getProjectId(),  getUsername(), getEmail(), getProjectRoleId(), getRoleType());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add account to the project");
        }

    }

    private void validateInput() {
        if (email == null && username == null) {
            throw new InvalidParameterValueException("Must specify atleast username");
        }
        if (email != null && username == null) {
            throw new InvalidParameterValueException("Must specify username for given email ID");
        }
        if (getProjectId() < 1L) {
            throw new InvalidParameterValueException("Invalid Project ID provided");
        }
        if (projectRoleId != null && projectRoleId < 1L) {
            throw new InvalidParameterValueException("Invalid Project role ID provided");
        }
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
