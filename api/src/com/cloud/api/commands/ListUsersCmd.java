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
import com.cloud.api.response.UserResponse;
import com.cloud.user.UserAccount;

@Implementation(description="Lists user accounts", responseObject=UserResponse.class)
public class ListUsersCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsersCmd.class.getName());

    private static final String s_name = "listusersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.LONG, description="List users by account type. Valid types include admin, domain-admin, read-only-admin, or user.")
    private Long accountType;

    @IdentityMapper(entityTableName="user")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="List user by ID.")
    private Long id;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="List users by state of the user account.")
    private String state;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, description="List user by the username")
    private String username;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getAccountType() {
        return accountType;
    }

    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getUsername() {
        return username;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends UserAccount> result = _accountService.searchForUsers(this);
        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = new ArrayList<UserResponse>();
        for (UserAccount user : result) {
            UserResponse userResponse = _responseGenerator.createUserResponse(user);
            userResponses.add(userResponse);
        }
        response.setResponses(userResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
