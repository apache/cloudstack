//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "quotaCredits", responseObject = QuotaCreditsResponse.class, description = "Add +-credits to an account", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaCreditsCmd extends BaseCmd {

    @Inject
    QuotaService _quotaService;

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class.getName());

    private static final String s_name = "quotacreditsresponse";

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = true, description = "Account Id for which quota credits need to be added")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class, description = "Domain for which quota credits need to be added")
    private Long domainId;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.DOUBLE, required = true, description = "Value of the credits to be added+, subtracted-")
    private Double value;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public QuotaCreditsCmd() {
        super();
    }

    public QuotaCreditsCmd(final QuotaService quotaService) {
        super();
        _quotaService = quotaService;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "The account does not exists or has been removed/disabled");
        }
        if (value == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please send a valid non-empty quota value");
        }

        final QuotaCreditsResponse response = _quotaService.addQuotaCredits(accountId, domainId, value, CallContext.current().getCallingUserId());
        response.setResponseName(getCommandName());
        response.setObjectName("quotacredits");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
