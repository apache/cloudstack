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

import com.cloud.utils.Pair;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

@APICommand(name = "quotaCreditsList", responseObject = QuotaCreditsResponse.class, description = "Lists quota credits of an account.", since = "4.21.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaCreditsListCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "ID of the account for which the credit statement will be generated.")
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "ID of the domain for which credit statement will be generated. " +
            "Available only for administrators.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "End date of the credit statement. If not provided, the current date will be " +
            "considered as the end date. " + ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Start date of the credit statement. If not provided, the first day of the current month " +
            "will be considered as the start date. " + ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.IS_RECURSIVE, type = CommandType.BOOLEAN, description = "Whether to generate the credit statement for the provided domain and its children. " +
            "Defaults to false.")
    private Boolean recursive = false;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Date getEndDate() {
        return ObjectUtils.defaultIfNull(endDate, new Date());
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return ObjectUtils.defaultIfNull(startDate, DateUtils.truncate(new Date(), Calendar.MONTH));
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Boolean getRecursive() {
        return recursive;
    }

    public void setRecursive(Boolean recursive) {
        this.recursive = recursive;
    }

    @Override
    public void execute() {
        Pair<List<QuotaCreditsResponse>, Integer> responses = quotaResponseBuilder.createQuotaCreditsListResponse(this);
        ListResponse<QuotaCreditsResponse> response = new ListResponse<>();
        response.setResponses(responses.first(), responses.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return -1;
    }

}
