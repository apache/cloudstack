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
package org.apache.cloudstack.api.command.user.account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListDomainResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listAccounts", description="Lists accounts and provides detailed account information for listed accounts", responseObject=AccountResponse.class)
public class ListAccountsCmd extends BaseListDomainResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListAccountsCmd.class.getName());
    private static final String s_name = "listaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.LONG, description="list accounts by account type. Valid account types are 1 (admin), 2 (domain-admin), and 0 (user).")
    private Long accountType;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = AccountResponse.class,
            description="list account by account ID")
    private Long id;

    @Parameter(name=ApiConstants.IS_CLEANUP_REQUIRED, type=CommandType.BOOLEAN, description="list accounts by cleanuprequred attribute (values are true or false)")
    private Boolean cleanupRequired;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list account by account name")
    private String searchName;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="list accounts by state. Valid states are enabled, disabled, and locked.")
    private String state;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getAccountType() {
        return accountType;
    }

    public Long getId() {
        return id;
    }

    public Boolean isCleanupRequired() {
        return cleanupRequired;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getState() {
        return state;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ListResponse<AccountResponse> response = _queryService.searchForAccounts(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
