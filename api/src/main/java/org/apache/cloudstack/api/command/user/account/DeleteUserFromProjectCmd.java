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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.project.DeleteProjectCmd;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;

@APICommand(name = DeleteUserFromProjectCmd.APINAME, description = "Deletes user from the project", responseObject = SuccessResponse.class, since = "4.15.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class DeleteUserFromProjectCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(DeleteProjectCmd.class.getName());
    public static final String APINAME = "deleteUserFromProject";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PROJECT_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = ProjectResponse.class,
            required = true,
            description = "ID of the project to remove the user from")
    private Long projectId;

    @Parameter(name = ApiConstants.USER_ID, type = BaseCmd.CommandType.UUID, entityType = UserResponse.class,
            required = true, description = "Id of the user to be removed from the project")
    private Long userId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public Long getUserId() {
        return userId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_USER_REMOVE;
    }

    @Override
    public String getEventDescription() {
        return "Removing user " + userId + " from project: " + projectId;
    }


    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        Project project = _projectService.getProject(projectId);
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by ID " + projectId);
        }
        return _projectService.getProjectOwner(projectId).getId();
    }

    @Override
    public List<Long> getEntityOwnerIds() {
        return _projectService.getProjectOwners(projectId);
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Project ID: " + projectId + "; user ID: " + userId);
        boolean result = _projectService.deleteUserFromProject(getProjectId(), getUserId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete account from the project");
        }
    }
}
