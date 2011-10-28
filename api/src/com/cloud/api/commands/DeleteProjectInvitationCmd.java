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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Accepts or declines project invitation", responseObject=SuccessResponse.class)
public class DeleteProjectInvitationCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteProjectInvitationCmd.class.getName());
    private static final String s_name = "deleteprojectinvitationresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, required=true, type=CommandType.LONG, description="id of the invitation")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
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
    public long getEntityOwnerId() {
        //TODO - return project entity ownerId
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
 

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Project invitation id " + id);
        boolean result = _projectService.deleteProjectInvitation(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete the project invitation");
        }
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_INVITATION_REMOVE;
    }
    
    @Override
    public String getEventDescription() {
        return  "Project invitatino id " + id + " is being removed";
    }
}
