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

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "updateAccount", description="Updates account information for the authenticated user", responseObject=AccountResponse.class)
public class UpdateAccountCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateAccountCmd.class.getName());
    private static final String s_name = "updateaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=AccountResponse.class,
            description="Account id")
    private Long id;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the current account name")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="the ID of the domain where the account exists")
    private Long domainId;

    @Parameter(name=ApiConstants.NEW_NAME, type=CommandType.STRING, required=true, description="new name for the account")
    private String newName;

    @Parameter(name=ApiConstants.NETWORK_DOMAIN, type=CommandType.STRING, description="Network domain for the account's networks; empty string will update domainName with NULL value")
    private String networkDomain;

    @Parameter(name = ApiConstants.ACCOUNT_DETAILS, type = CommandType.MAP, description = "details for account used to store specific parameters")
    private Map details;

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

    public String getNewName() {
        return newName;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public Map getDetails() {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Collection paramsCollection = details.values();
        Map params = (Map) (paramsCollection.toArray())[0];
        return params;
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
    public void execute(){
        Account result = _regionService.updateAccount(this);
        if (result != null){
            AccountResponse response = _responseGenerator.createAccountResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update account");
        }
    }
}
