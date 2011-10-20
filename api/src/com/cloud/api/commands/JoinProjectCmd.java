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

@Implementation(description="Makes account to join the project", responseObject=SuccessResponse.class)
public class JoinProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(JoinProjectCmd.class.getName());
    private static final String s_name = "joinprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.PROJECT_ID, required=true, type=CommandType.LONG, description="id of the project to join")
    private Long projectId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, required=true, description="account that is joining the project")
    private String accountName;
    
    @Parameter(name=ApiConstants.TOKEN, type=CommandType.STRING, description="list invitations for specified account; this parameter has to be specified with domainId")
    private String token;
   
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getProjectId() {
        return projectId;
    }

    public String getAccountName() {
        return accountName;
    }
    
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public String getToken() {
        return token;
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
        UserContext.current().setEventDetails("Project id: "+ projectId + "; accountName " + accountName);
        boolean result = _projectService.joinProject(projectId, accountName, token);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to join the project");
        }
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_JOIN;
    }
    
    @Override
    public String getEventDescription() {
        return  "Account " + accountName + " joining the project: " + projectId;
    }
}
