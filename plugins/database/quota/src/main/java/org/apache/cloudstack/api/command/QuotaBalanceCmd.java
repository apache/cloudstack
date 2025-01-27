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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.cloud.user.Account;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaBalanceResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;

@APICommand(name = "quotaBalance", responseObject = QuotaStatementItemResponse.class, description = "Create a quota balance statement", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaBalanceCmd extends BaseCmd {



    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = true, description = "Account Id for which statement needs to be generated")
    private String accountName;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class, description = "If domain Id is given and the caller is domain admin then the statement is generated for domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "End of the period of the Quota balance." +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Start of the period of the Quota balance. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "List usage records for the specified account")
    private Long accountId;

    @Inject
    QuotaResponseBuilder _responseBuilder;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

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

    public Date getEndDate() {
        if (endDate == null){
            return null;
        }
        else{
            return _responseBuilder.startOfNextDay(new Date(endDate.getTime()));
        }
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
    }

    @Override
    public long getEntityOwnerId() {
        if (accountId != null) {
            return accountId;
        }
        Account account = _accountService.getActiveAccountByName(accountName, domainId);
        if (account != null) {
            return account.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<QuotaBalanceVO> quotaUsage = _responseBuilder.getQuotaBalance(this);

        QuotaBalanceResponse response;
        if (endDate == null) {
            response = _responseBuilder.createQuotaLastBalanceResponse(quotaUsage, getStartDate());
        } else {
            response = _responseBuilder.createQuotaBalanceResponse(quotaUsage, getStartDate(), new Date(endDate.getTime()));
        }
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
