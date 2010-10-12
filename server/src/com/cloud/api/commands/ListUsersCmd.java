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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.user.UserAccountVO;

@Implementation(method="searchForUsers", description="Lists user accounts")
public class ListUsersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsersCmd.class.getName());

    private static final String s_name = "listusersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="List user by account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="accounttype", type=CommandType.LONG, description="List users by account type. Valid types include admin, domain-admin, read-only-admin, or user.")
    private Long accountType;

    @Parameter(name="domainid", type=CommandType.LONG, description="List all users in a domain. If used with the account parameter, lists an account in a specific domain.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, description="List user by ID.")
    private Long id;

    @Parameter(name="state", type=CommandType.STRING, description="List users by state of the user account.")
    private String state;

    @Parameter(name="username", type=CommandType.STRING, description="List user by the username")
    private String username;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getAccountType() {
        return accountType;
    }

    public Long getDomainId() {
        return domainId;
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
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<UserResponse> getResponse() {
        List<UserAccountVO> users = (List<UserAccountVO>)getResponseObject();

        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = new ArrayList<UserResponse>();
        for (UserAccountVO user : users) {
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setFirstname(user.getFirstname());
            userResponse.setLastname(user.getLastname());
            userResponse.setEmail(user.getEmail());
            userResponse.setCreated(user.getCreated());
            userResponse.setState(user.getState());
            userResponse.setAccountName(user.getAccountName());
            userResponse.setAccountType(user.getType());
            userResponse.setDomainId(user.getDomainId());
            userResponse.setDomainName(ApiDBUtils.findDomainById(user.getDomainId()).getName());
            userResponse.setTimezone(user.getTimezone());
            userResponse.setApiKey(user.getApiKey());
            userResponse.setSecretKey(user.getSecretKey());

            userResponse.setResponseName("user");
            userResponses.add(userResponse);
        }

        response.setResponses(userResponses);
        response.setResponseName(getName());
        return response;
    }
}
