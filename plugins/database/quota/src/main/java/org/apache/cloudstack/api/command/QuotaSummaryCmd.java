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
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaSummaryResponse;
import org.apache.cloudstack.quota.QuotaAccountStateFilter;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


import java.util.List;

import javax.inject.Inject;

@APICommand(name = "quotaSummary", responseObject = QuotaSummaryResponse.class, description = "Lists accounts' balance summary.", since = "4.7.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaSummaryCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = false, description = "Optional, Account Id for which statement needs to be generated")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = false, entityType = DomainResponse.class, description = "Optional, If domain Id is given and the caller is domain admin then the statement is generated for domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "False (default) lists balance summary for account. True lists balance summary for " +
            "accounts which the caller has access.")
    private Boolean listAll;

    @Parameter(name = ApiConstants.ACCOUNT_STATE_TO_SHOW, type = CommandType.STRING, description =  "Possible values are [ALL, ACTIVE, REMOVED]. ALL will list summaries for " +
            "active and removed accounts; ACTIVE will list summaries only for active accounts; REMOVED will list summaries only for removed accounts. The default value is ACTIVE.")
    private String accountStateToShow;

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    @Inject
    QuotaService quotaService;

    @Override
    public void execute() {
        Pair<List<QuotaSummaryResponse>, Integer> responses = quotaResponseBuilder.createQuotaSummaryResponse(this);
        ListResponse<QuotaSummaryResponse> response = new ListResponse<>();

        response.setResponses(responses.first(), responses.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
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

    public Boolean isListAll() {
        return ObjectUtils.defaultIfNull(listAll, Boolean.FALSE);
    }

    public void setListAll(Boolean listAll) {
        this.listAll = listAll;
    }

    public QuotaAccountStateFilter getAccountStateToShow() {
        if (StringUtils.isNotBlank(accountStateToShow)) {
            QuotaAccountStateFilter state = QuotaAccountStateFilter.getValue(accountStateToShow);
            if (state != null) {
                return state;
            }
        }

        return QuotaAccountStateFilter.ACTIVE;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
