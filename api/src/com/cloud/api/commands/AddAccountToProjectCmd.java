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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;
import com.cloud.user.UserContext;
import com.cloud.utils.AnnotationHelper;


@Implementation(description="Adds acoount to a project", responseObject=SuccessResponse.class, since="3.0.0")
public class AddAccountToProjectCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddAccountToProjectCmd.class.getName());

    private static final String s_name = "addaccounttoprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, required=true, description="id of the project to add the account to")
    private Long projectId;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="name of the account to be added to the project")
    private String accountName;
    
    @Parameter(name=ApiConstants.EMAIL, type=CommandType.STRING, description="email to which invitation to the project is going to be sent")
    private String email;

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

    @Override
    public String getCommandName() {
        return s_name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        if (accountName == null && email == null) {
            throw new InvalidParameterValueException("Either accountName or email is required");
        }
        
        UserContext.current().setEventDetails("Project id: "+ projectId + "; accountName " + accountName);
        boolean result = _projectService.addAccountToProject(getProjectId(), getAccountName(), getEmail());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add account to the project");
        }
    }
    
    @Override
    public long getEntityOwnerId() {
        Project project= _projectService.getProject(projectId);
        //verify input parameters
        if (project == null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
        	ex.addProxyObject(project, projectId, "projectId");            
            throw ex;
        } 
        
        return _projectService.getProjectOwner(projectId).getId(); 
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_ACCOUNT_ADD;
    }
    
    @Override
    public String getEventDescription() {
        if (accountName != null) {
            return  "Adding account " + accountName + " to project: " + projectId;
        } else {
            return  "Sending invitation to email " + email + " to join project: " + projectId;
        }  
    }
}