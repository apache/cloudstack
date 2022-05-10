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

package org.apache.cloudstack.api.command.admin.zone;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "markDefaultZoneForAccount", description = "Marks a default zone for this account", responseObject = AccountResponse.class, since = "4.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class MarkDefaultZoneForAccountCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MarkDefaultZoneForAccountCmd.class.getName());

    private static final String s_name = "markdefaultzoneforaccountresponse";

    /////////////////////////////////////////////////////
    ////////////////API parameters //////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               entityType = AccountResponse.class,
               required = true,
               description = "Name of the account that is to be marked.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               required = true,
               description = "Marks the account that belongs to the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = true,
               description = "The Zone ID with which the account is to be marked.")
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
    public Long getApiResourceId() {
        Account account = _accountService.getActiveAccountByName(accountName, domainId);
        return account != null ? account.getId() : null;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Account;
    }

    @Override
    public void execute() {
        Account result = _configService.markDefaultZone(getAccountName(), getDomainId(), getDefaultZoneId());
        if (result != null) {
            AccountResponse response = _responseGenerator.createAccountResponse(ResponseView.Full, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        }
        else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to mark the account with the default zone");
        }
    }
}
