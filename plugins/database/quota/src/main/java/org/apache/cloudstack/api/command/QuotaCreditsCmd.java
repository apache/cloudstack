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

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.QuotaService;

import javax.inject.Inject;

@APICommand(name = "quotaCredits", responseObject = QuotaCreditsResponse.class, description = "Add +-credits to an Account", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaCreditsCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Inject
    QuotaService _quotaService;

    @Deprecated
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Name of the Account for which Quota credits will be added. Deprecated, please use '" +
            ApiConstants.ACCOUNT_ID + "' instead.")
    private String accountName;

    @ACL
    @Deprecated
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "Domain of the Account specified by '" + ApiConstants.ACCOUNT + "' for which Quota credits will be added. " +
                    "Deprecated, please use '" + ApiConstants.ACCOUNT_ID + "' instead.")
    private Long domainId;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            description = "ID of the Account for which Quota credits will be added. Can not be specified with '" + ApiConstants.PROJECT_ID + "'.")
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "ID of the Project for which Quota credits will be added. Can not be specified with '" + ApiConstants.ACCOUNT_ID + "'.")
    private Long projectId;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.DOUBLE, required = true, description = "Amount of credits to be added (in case of a positive value) or subtracted (in case of a negative value).")
    private Double value;

    @Parameter(name = "min_balance", type = CommandType.DOUBLE, description = "An email will be sent to the Account when the Quota credits get below this threshold.")
    private Double minBalance;

    @Parameter(name = "quota_enforce", type = CommandType.BOOLEAN, description = "Whether to lock the Account when Quota credits are below zero.")
    private Boolean quotaEnforce;

    public Double getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(Double minBalance) {
        this.minBalance = minBalance;
    }

    public Boolean getQuotaEnforce() {
        return quotaEnforce;
    }

    public void setQuotaEnforce(Boolean quotaEnforce) {
        this.quotaEnforce = quotaEnforce;
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

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public QuotaCreditsCmd() {
        super();
    }

    @Override
    public void execute() {
        QuotaCreditsResponse response = _responseBuilder.addQuotaCredits(this);
        response.setResponseName(getCommandName());
        response.setObjectName("quotacredits");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
