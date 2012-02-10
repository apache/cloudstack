/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ProjectInvitationResponse;
import com.cloud.projects.ProjectInvitation;

@Implementation(description = "Lists projects and provides detailed information for listed projects", responseObject = ProjectInvitationResponse.class, since = "3.0.0")
public class ListProjectInvitationsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectInvitationsCmd.class.getName());
    private static final String s_name = "listprojectinvitationsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @IdentityMapper(entityTableName = "projects")
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.LONG, description = "list by project id")
    private Long projectId;

    @Parameter(name = ApiConstants.ACTIVE_ONLY, type = CommandType.BOOLEAN, description = "if true, list only active invitations - having Pending state and ones that are not timed out yet")
    private boolean activeOnly;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list invitations by state")
    private String state;

    @IdentityMapper(entityTableName = "project_invitations")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "list invitations by id")
    private Long id;

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

    @Override
    public String getCommandName() {
        return s_name;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends ProjectInvitation> invites = _projectService.listProjectInvitations(id, projectId, this.getAccountName(), this.getDomainId(), state, activeOnly, this.getStartIndex(), this.getPageSizeVal(),
                this.isRecursive(), this.listAll());
        ListResponse<ProjectInvitationResponse> response = new ListResponse<ProjectInvitationResponse>();
        List<ProjectInvitationResponse> projectInvitationResponses = new ArrayList<ProjectInvitationResponse>();
        for (ProjectInvitation invite : invites) {
            ProjectInvitationResponse projectResponse = _responseGenerator.createProjectInvitationResponse(invite);
            projectInvitationResponses.add(projectResponse);
        }
        response.setResponses(projectInvitationResponses);
        response.setResponseName(getCommandName());

        this.setResponseObject(response);
    }

}
