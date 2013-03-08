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
package org.apache.cloudstack.api.command.admin.account;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@APICommand(name = "disableAccount", description="Disables an account", responseObject=AccountResponse.class)
public class DisableAccountCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DisableAccountCmd.class.getName());
    private static final String s_name = "disableaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=AccountResponse.class,
            description="Account id")
    private Long id;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="Disables specified account.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="Disables specified account in this domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.LOCK, type=CommandType.BOOLEAN, required=true, description="If true, only lock the account; else disable the account")
    private Boolean lockRequested;

    @Inject RegionService _regionService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Boolean getLockRequested() {
        return lockRequested;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACCOUNT_DISABLE;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = _entityMgr.findById(Account.class, getId());
        if (account != null) {
            return account.getAccountId();
        }

        account = _accountService.getActiveAccountByName(getAccountName(), getDomainId());
        if (account != null) {
            return account.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventDescription() {
        return  "disabling account: " + getAccountName() + " in domain: " + getDomainId();
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException{
        UserContext.current().setEventDetails("Account Name: "+getAccountName()+", Domain Id:"+getDomainId());
        Account result = _regionService.disableAccount(this);
        if (result != null){
            AccountResponse response = _responseGenerator.createAccountResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, lockRequested == true ? "Failed to lock account" : "Failed to disable account" );
        }
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Account;
    }
}
