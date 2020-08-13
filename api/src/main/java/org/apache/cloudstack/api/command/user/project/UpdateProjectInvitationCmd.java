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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "updateProjectInvitation", description = "Accepts or declines project invitation", responseObject = SuccessResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateProjectInvitationCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateProjectInvitationCmd.class.getName());
    private static final String s_name = "updateprojectinvitationresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "id of the project to join")
    private Long projectId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account that is joining the project")
    private String accountName;

    @Parameter(name = ApiConstants.USER_ID, type = BaseCmd.CommandType.UUID, entityType = UserResponse.class,
            description = "User UUID, required for adding account from external provisioning system")
    private Long userId;

    @Parameter(name = ApiConstants.TOKEN,
               type = CommandType.STRING,
               description = "list invitations for specified account; this parameter has to be specified with domainId")
    private String token;

    @Parameter(name = ApiConstants.ACCEPT, type = CommandType.BOOLEAN, description = "if true, accept the invitation, decline if false. True by default")
    private Boolean accept;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Long getProjectId() {
        return projectId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getUserId() { return userId; }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public String getToken() {
        return token;
    }

    public Boolean getAccept() {
        if (accept == null) {
            return true;
        }
        return accept;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        // TODO - return project entity ownerId
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
// tracked
    }

    @Override
    public void execute() {
        String eventDetails = "Project id: " + projectId + ";";
        if (accountName != null) {
            eventDetails +=  " accountName: " + accountName + ";";
        } else if (userId != null) {
            eventDetails += " userId: " + userId + ";";
        }
        eventDetails += " accept " + getAccept();
        CallContext.current().setEventDetails(eventDetails);
        boolean result = _projectService.updateInvitation(projectId, accountName, userId, token, getAccept());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to join the project");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_INVITATION_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating project invitation for projectId " + projectId;
    }
}
