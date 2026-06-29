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

import javax.inject.Inject;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaBalanceResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.commons.lang3.ObjectUtils;

@APICommand(name = "quotaBalance", responseObject = QuotaBalanceResponse.class, description = "Create Quota balance statements for the provided Accounts or Projects.", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        httpMethod = "GET")
public class QuotaBalanceCmd extends BaseCmd {

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Name of the Account for which balance statements will be generated. " +
            "Deprecated, please use 'accountid' instead.")
    private String accountName;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "ID of the domain which the Account identified by 'account' belongs to." +
            "Deprecated, please use 'accountid' instead.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "Date of the last Quota balance to be returned. Must be informed together with the " +
            "startdate parameter, and can not be before startdate. " + ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Date of the first Quota balance to be returned. Must be before today. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "ID of the Account for which balance statements will be generated.")
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "ID of the Project for which balance statements will be generated. Can not be specified with accountid.")
    private Long projectId;

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
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public long getEntityOwnerId() {
        if (ObjectUtils.allNull(accountId, accountName, domainId, projectId)) {
            return -1;
        }
        return _accountService.finalizeAccountId(accountId, accountName, domainId, projectId);
    }

    @Override
    public void execute() {
        QuotaBalanceResponse response = _responseBuilder.createQuotaBalanceResponse(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
