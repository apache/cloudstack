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
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ProjectResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Creates a project", responseObject=ProjectResponse.class)
public class CreateProjectCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateProjectCmd.class.getName());

    private static final String s_name = "createprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will be Admin for the project")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="domain ID of the account owning a project")
    private Long domainId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the project")
    private String name;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="display text of the project")
    private String displayText;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
    	return "projects";
    }
    
    public String getAccountName() {
        if (accountName != null) {
            return accountName;
        } else {
            return UserContext.current().getCaller().getAccountName();
        }
    }

    public Long getDomainId() {
        if (domainId != null) {
            return domainId;
        } else {
            return UserContext.current().getCaller().getDomainId();
        }
        
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        Account caller = UserContext.current().getCaller();
        
        if ((accountName != null && domainId == null) || (domainId != null && accountName == null)) {
            throw new InvalidParameterValueException("Account name and domain id must be specified together");
        }
        
        if (accountName != null) {
            return _accountService.finalizeOwner(caller, accountName, domainId, null).getId();
        }
        
        return caller.getId();
    }
 

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        Project project = _projectService.enableProject(this.getEntityId());
        if (project != null) {
            ProjectResponse response = _responseGenerator.createProjectResponse(project);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a project");
        }
    }
    
    @Override
    public void create() throws ResourceAllocationException{
        UserContext.current().setEventDetails("Project Name: "+ getName());
        Project project = _projectService.createProject(getName(), getDisplayText(), getAccountName(), getDomainId());
        if (project != null) {
            this.setEntityId(project.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a project");
        }
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_CREATE;
    }
    
    @Override
    public String getEventDescription() {
        return "creating project";
    }
}