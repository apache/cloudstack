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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project;

@APICommand(name = "suspendProject", description = "Suspends a project", responseObject = ProjectResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class SuspendProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(SuspendProjectCmd.class.getName());

    private static final String s_name = "suspendprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "id of the project to be suspended")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long geId() {
        return id;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext.current().setEventDetails("Project Id: " + id);
        Project project = _projectService.suspendProject(id);
        if (project != null) {
            ProjectResponse response = _responseGenerator.createProjectResponse(project);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to suspend a project");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_SUSPEND;
    }

    @Override
    public String getEventDescription() {
        return "Suspending project: " + id;
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

}
