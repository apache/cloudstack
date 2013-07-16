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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.project.DeleteProjectCmd;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;

@APICommand(name = "deleteAccountFromProject", description="Deletes account from the project", responseObject=SuccessResponse.class, since="3.0.0")
public class DeleteAccountFromProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteProjectCmd.class.getName());

    private static final String s_name = "deleteaccountfromprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            required=true, description="id of the project to remove the account from")
    private Long projectId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, required=true, description="name of the account to be removed from the project")
    private String accountName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////




    @Override
    public String getCommandName() {
        return s_name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getProjectId() {
        return projectId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Project id: "+ projectId + "; accountName " + accountName);
        boolean result = _projectService.deleteAccountFromProject(projectId, accountName);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete account from the project");
        }
    }


    @Override
    public long getEntityOwnerId() {
        Project project= _projectService.getProject(projectId);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + projectId);
        }

        return _projectService.getProjectOwner(projectId).getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_ACCOUNT_REMOVE;
    }

    @Override
    public String getEventDescription() {
        return  "Removing account " + accountName + " from project: " + projectId;
    }
}
