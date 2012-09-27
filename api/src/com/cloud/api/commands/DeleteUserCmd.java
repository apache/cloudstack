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
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;

@Implementation(description="Creates a user for an account", responseObject=UserResponse.class)
public class DeleteUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteUserCmd.class.getName());

    private static final String s_name = "deleteuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="user")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="Deletes a user")
    private Long id;

    @Parameter(name=ApiConstants.IS_PROPAGATE, type=CommandType.BOOLEAN, description="True if command is sent from another Region")
    private Boolean isPropagate;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

	public Boolean getIsPropagate() {
		return isPropagate;
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
        User user = _entityMgr.findById(User.class, getId());
        if (user != null) {
            return user.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("UserId: "+getId());
        boolean isPopagate = (getIsPropagate() != null ) ? getIsPropagate() : false;
        boolean result = false;
        if(isPopagate){
        	result = _accountService.deleteUser(this);
        } else {
        	result = _regionService.deleteUser(this);
        }
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete user");
        }
    }
}