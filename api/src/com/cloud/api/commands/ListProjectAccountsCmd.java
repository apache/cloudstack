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
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ProjectAccountResponse;
import com.cloud.api.response.ProjectResponse;
import com.cloud.projects.ProjectAccount;
import com.cloud.user.Account;

@Implementation(description="Lists project's accounts", responseObject=ProjectResponse.class, since="3.0.0")
public class ListProjectAccountsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectAccountsCmd.class.getName());

    private static final String s_name = "listprojectaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, required=true, description="id of the project")
    private Long projectId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="list accounts of the project by account name")
    private String accountName;
   
    @Parameter(name=ApiConstants.ROLE, type=CommandType.STRING, description="list accounts of the project by role")
    private String role;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
       //TODO - return project entity ownerId

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
 

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        List<? extends ProjectAccount> projectAccounts = _projectService.listProjectAccounts(projectId, accountName, role, this.getStartIndex(), this.getPageSizeVal());
        ListResponse<ProjectAccountResponse> response = new ListResponse<ProjectAccountResponse>();
        List<ProjectAccountResponse> projectResponses = new ArrayList<ProjectAccountResponse>();
        for (ProjectAccount projectAccount : projectAccounts) {
            ProjectAccountResponse projectAccountResponse = _responseGenerator.createProjectAccountResponse(projectAccount);
            projectResponses.add(projectAccountResponse);
        }
        response.setResponses(projectResponses);
        response.setResponseName(getCommandName());
        
        this.setResponseObject(response);
    }
}