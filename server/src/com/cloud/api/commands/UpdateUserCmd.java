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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.server.ManagementServer;

@Implementation(method="updateUser", manager=ManagementServer.class, description="Updates a user account")
public class UpdateUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateUserCmd.class.getName());

    private static final String s_name = "updateuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.API_KEY, type=CommandType.STRING, description="The API key for the user.")
    private String apiKey;

    @Parameter(name=ApiConstants.EMAIL, type=CommandType.STRING, description="email")
    private String email;

    @Parameter(name=ApiConstants.FIRSTNAME, type=CommandType.STRING, description="first name")
    private String firstname;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="User id")
    private Long id;

    @Parameter(name=ApiConstants.LASTNAME, type=CommandType.STRING, description="last name")
    private String lastname;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, description="Hashed password (default is MD5). If you wish to use any other hasing algorithm, you would need to write a custom authentication adapter")
    private String password;

    @Parameter(name=ApiConstants.SECRET_KEY, type=CommandType.STRING, description="The secret key for the user.")
    private String secretKey;

    @Parameter(name=ApiConstants.TIMEZONE, type=CommandType.STRING, description="Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, description="Unique username")
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
   
	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
	}
}