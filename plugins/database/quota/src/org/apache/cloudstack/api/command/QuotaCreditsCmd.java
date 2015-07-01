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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaManager;


@APICommand(name = "quotaCredits", responseObject = QuotaCreditsResponse.class, description = "Add +-credits to an account", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaCreditsCmd extends BaseListCmd {

public static final Logger s_logger = Logger
        .getLogger(QuotaStatementCmd.class.getName());

private static final String s_name = "quotacreditsresponse";

@Inject
private QuotaManager _quotaManager;

@Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account Id for which quota credits need to be added")
private String accountName;

@Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Domain for which quota credits need to be added")
private Long domainId;


@Parameter(name = ApiConstants.VALUE, type = CommandType.INTEGER, entityType = DomainResponse.class, description = "Value of the credits to be added+, subtracted-")
private Integer value;


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


public Integer getValue() {
    return value;
}


public void setValue(Integer value) {
    this.value = value;
}


public QuotaCreditsCmd() {
    super();
}


public QuotaCreditsCmd(final QuotaManager quotaManager) {
    super();
    _quotaManager = quotaManager;
}


@Override
public String getCommandName() {
    return s_name;
}


@Override
public void execute() {
    Long accountId = _accountService.finalyzeAccountId(accountName, domainId, null, true);
    if (accountId==null){
        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "The account does not exists or has been removed/disabled");
    }

    final QuotaCreditsResponse credit_response = _quotaManager.addQuotaCredits(accountId, domainId, value, CallContext.current().getCallingAccount().getId());

    setResponseObject(credit_response);
}


}
