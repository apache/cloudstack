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
package org.apache.cloudstack.api.command.user.project;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;

@APICommand(name = "updateProject", description = "Updates a project", responseObject = ProjectResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateProjectCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "id of the project to be modified")
    private Long id;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "new Admin account for the project")
    private String accountName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "display text of the project")
    private String displayText;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "ID of the user to be promoted/demoted")
    private Long userId;

    @Parameter(name = ApiConstants.ROLE_TYPE, type = CommandType.STRING, description = "Account level role to be assigned to the user/account : Admin/Regular")
    private String roleType;

    @Parameter(name = ApiConstants.SWAP_OWNER, type = CommandType.BOOLEAN, description = "when true, it swaps ownership with the account/ user provided. " +
            "Ideally to be used when a single project administrator is present. In case of multiple project admins, swapowner is to be set to false," +
            "to promote or demote the user/account based on the roleType (Regular or Admin) provided. Defaults to true")
    private Boolean swapOwner;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the project", since = "4.19.0")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public ProjectAccount.Role getRoleType(String role) {
        String type = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
        if (!EnumUtils.isValidEnum(ProjectAccount.Role.class, type)) {
            throw new InvalidParameterValueException("Only Admin or Regular project role types are valid");
        }
        return Enum.valueOf(ProjectAccount.Role.class, type);
    }

    public ProjectAccount.Role getAccountRole() {
        if (StringUtils.isNotEmpty(roleType)) {
            return getRoleType(roleType);
        }
        return ProjectAccount.Role.Regular;
    }

    public Boolean isSwapOwner() {
        return swapOwner != null ? swapOwner : true;
    }

    @Override
    public long getEntityOwnerId() {
        Project project = _projectService.getProject(id);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + id);
        }

        return _projectService.getProjectOwner(id).getId();
    }

    @Override
    public List<Long> getEntityOwnerIds() {
        return _projectService.getProjectOwners(id);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceAllocationException {
        CallContext.current().setEventDetails("Project id: " + getId());
        if (getAccountName() != null && getUserId() != null) {
            throw new InvalidParameterValueException("Account name and user ID are mutually exclusive. Provide either account name" +
                    "to update account or user ID to update the user of the project");
        }

        Project project = null;
        if (isSwapOwner()) {
            project = _projectService.updateProject(getId(), getName(), getDisplayText(), getAccountName());
        }  else {
            project = _projectService.updateProject(getId(), getName(), getDisplayText(), getAccountName(), getUserId(), getAccountRole());
        }
        if (project != null) {
            ProjectResponse response = _responseGenerator.createProjectResponse(project);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update a project");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating project: " + id;
    }
}
