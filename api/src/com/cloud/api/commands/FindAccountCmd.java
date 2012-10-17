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
import com.cloud.api.response.FindAccountResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@Implementation(description="Find account by ID", responseObject=FindAccountResponse.class)
public class FindAccountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(FindAccountCmd.class.getName());

    private static final String s_name = "findaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="account")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required=true, description = "Id of the account")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

	public Long getId() {
		return id;
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
        Account result = _accountService.findAccount(getId());
        if(result != null){
        	FindAccountResponse response = _responseGenerator.createFindAccountResponse(result);
        	response.setResponseName(getCommandName());
        	this.setResponseObject(response);
        } else {
            throw new InvalidParameterValueException("Account with specified Id does not exist");
        }
    }
}
