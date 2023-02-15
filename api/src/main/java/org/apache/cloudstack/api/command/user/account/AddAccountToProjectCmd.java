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

import java.util.List;

import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;

@APICommand(name = "addAccountToProject", description = "Adds account to a project", responseObject = SuccessResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddAccountToProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddAccountToProjectCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ID,
               type = CommandType.UUID,
               entityType = ProjectResponse.class,
               required = true,
               description = "ID of the project to add the account to")
    private Long projectId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "name of the account to be added to the project")
    private String accountName;

    @Parameter(name = ApiConstants.EMAIL, type = CommandType.STRING, description = "email to which invitation to the project is going to be sent")
    private String email;

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, entityType = ProjectRoleResponse.class,
            description = "ID of the project role", validations = {ApiArgValidator.PositiveNumber})
    private Long projectRoleId;

    @Parameter(name = ApiConstants.ROLE_TYPE, type = BaseCmd.CommandType.STRING,
            description = "Project role type to be assigned to the user - Admin/Regular; default: Regular")
    private String roleType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getEmail() {
        return email;
    }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public ProjectAccount.Role getRoleType() {
        if (StringUtils.isNotEmpty(roleType)) {
            String role = roleType.substring(0, 1).toUpperCase() + roleType.substring(1).toLowerCase();
            if (!EnumUtils.isValidEnum(ProjectAccount.Role.class, role)) {
                throw new InvalidParameterValueException("Only Admin or Regular project role types are valid");
            }
            return Enum.valueOf(ProjectAccount.Role.class, role);
        }
        return null;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        if (accountName == null && email == null) {
            throw new InvalidParameterValueException("Either accountName or email is required");
        }

        CallContext.current().setEventDetails("Project ID: " + projectId + "; accountName " + accountName);
        boolean result = _projectService.addAccountToProject(getProjectId(), getAccountName(), getEmail(), getProjectRoleId(), getRoleType());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add account to the project");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Project project = _projectService.getProject(getProjectId());
        //verify input parameters
        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified ID");
            ex.addProxyObject(getProjectId().toString(), "projectId");
            throw ex;
        }

        return _projectService.getProjectOwner(getProjectId()).getId();
    }

    @Override
    public List<Long> getEntityOwnerIds() {
        return _projectService.getProjectOwners(projectId);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_ACCOUNT_ADD;
    }

    @Override
    public String getEventDescription() {
        if (accountName != null) {
            return "Adding account " + getAccountName() + " to project: " + getProjectId();
        } else {
            return "Sending invitation to email " + email + " to join project: " + getProjectId();
        }
    }

    @Override
    public Long getApiResourceId() {
        return projectId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Project;
    }
}
