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

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityMapper;
import com.cloud.user.Account;
import com.cloud.event.EventTypes;
import com.cloud.async.AsyncJob;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd;

@Implementation(description="Marks a default zone for this account", responseObject=AccountResponse.class, since="3.0.3")
public class MarkDefaultZoneForAccountCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MarkDefaultZoneForAccountCmd.class.getName());

    private static final String s_name = "markdefaultzoneforaccountresponse";

    /////////////////////////////////////////////////////
    ////////////////API parameters //////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="account")
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, required=true, description="Name of the account that is to be marked.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, required=true, description="Marks the account that belongs to the specified domain.")
    private Long domainId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="The Zone ID with which the account is to be marked.")
    private Long defaultZoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getDefaultZoneId() {
        return defaultZoneId;
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
    	return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public String getEventType() {
    	return EventTypes.EVENT_ACCOUNT_MARK_DEFAULT_ZONE;
    }
    
    @Override
    public String getEventDescription() {
    	return  "Marking account with the default zone: " + getDefaultZoneId();
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Account;
    }
    
    @Override
    public void execute(){
    	Account result = _configService.markDefaultZone(getAccountName(),getDomainId(), getDefaultZoneId());
    	if (result != null) {
    		AccountResponse response = _responseGenerator.createAccountResponse(result);
    		response.setResponseName(getCommandName());
    		this.setResponseObject(response);
    	}
    	else {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to mark the account with the default zone");
    	}
    }
}

