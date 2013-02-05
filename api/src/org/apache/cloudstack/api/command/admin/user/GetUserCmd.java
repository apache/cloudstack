// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.UserAccount;

@APICommand(name = "getUser", description="Find user account by API key", responseObject=UserResponse.class)
public class GetUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetUserCmd.class.getName());

    private static final String s_name = "getuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.API_KEY, type=CommandType.STRING, required=true, description="API key of the user")
    private String apiKey;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getApiKey() {
        return apiKey;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }

    @Override
    public void execute(){
        UserAccount result = _accountService.getUserByApiKey(getApiKey());
        if(result != null){
            UserResponse response = _responseGenerator.createUserResponse(result);
            response.setResponseName(getCommandName());
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new InvalidParameterValueException("User with specified API key does not exist");
        }
    }
}
