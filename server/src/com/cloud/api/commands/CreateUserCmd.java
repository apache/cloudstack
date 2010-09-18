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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.UserResponse;
import com.cloud.user.UserAccount;

@Implementation(method="createUser")
public class CreateUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateUserCmd.class.getName());

    private static final String s_name = "createuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="accounttype", type=CommandType.LONG, required=true)
    private Long accountType;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="email", type=CommandType.STRING, required=true)
    private String email;

    @Parameter(name="firstname", type=CommandType.STRING, required=true)
    private String firstname;

    @Parameter(name="lastname", type=CommandType.STRING, required=true)
    private String lastname;

    @Parameter(name="password", type=CommandType.STRING, required=true)
    private String password;

    @Parameter(name="timezone", type=CommandType.STRING)
    private String timezone;

    @Parameter(name="username", type=CommandType.STRING, required=true)
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

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public String getTimezone() {
        return timezone;
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

    @Override
    public String getResponse() {
        UserAccount user = (UserAccount)getResponseObject();

        // TODO:  user keys?
        UserResponse response = new UserResponse();
        response.setAccountName(user.getAccountName());
        response.setAccountType(user.getType());
        response.setCreated(user.getCreated());
        response.setDomainId(user.getDomainId());
        response.setDomainName(ApiDBUtils.findDomainById(user.getDomainId()).getName());
        response.setEmail(user.getEmail());
        response.setFirstname(user.getFirstname());
        response.setId(user.getId());
        response.setLastname(user.getLastname());
        response.setState(user.getState());
        response.setTimezone(user.getTimezone());
        response.setUsername(user.getUsername());

        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}