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

import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;

@APICommand(name = "listProjectInvitations",
            description = "Lists project invitations and provides detailed information for listed invitations",
            responseObject = ProjectInvitationResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListProjectInvitationsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectInvitationsCmd.class.getName());
    private static final String s_name = "listprojectinvitationsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "list by project id")
    private Long projectId;

    @Parameter(name = ApiConstants.ACTIVE_ONLY,
               type = CommandType.BOOLEAN,
               description = "if true, list only active invitations - having Pending state and ones that are not timed out yet")
    private boolean activeOnly;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list invitations by state")
    private String state;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ProjectInvitationResponse.class, description = "list invitations by id")
    private Long id;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "list invitation by user ID")
    private Long userId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Long getProjectId() {
        return projectId;
    }

    public boolean isActiveOnly() {
        return activeOnly;
    }

    public String getState() {
        return state;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<ProjectInvitationResponse> response = _queryService.listProjectInvitations(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
