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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.SuccessResponse;

@Implementation(method="updateUser", manager=Manager.ManagementServer)
public class UpdateUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateUserCmd.class.getName());

    private static final String s_name = "updateuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="apikey", type=CommandType.STRING)
    private String apiKey;

    @Parameter(name="email", type=CommandType.STRING)
    private String email;

    @Parameter(name="firstname", type=CommandType.STRING)
    private String firstname;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="lastname", type=CommandType.STRING)
    private String lastname;

    @Parameter(name="password", type=CommandType.STRING)
    private String password;

    @Parameter(name="secretkey", type=CommandType.STRING)
    private String secretKey;

    @Parameter(name="timezone", type=CommandType.STRING)
    private String timezone;

    @Parameter(name="username", type=CommandType.STRING)
    private String username;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getApiKey() {
        return apiKey;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public Long getId() {
        return id;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public String getSecretKey() {
        return secretKey;
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

    public String getName() {
        return s_name;
    }
   
	@Override
	public ResponseObject getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
	}
}