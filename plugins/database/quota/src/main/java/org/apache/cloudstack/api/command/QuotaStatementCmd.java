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
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;
import org.apache.cloudstack.api.response.QuotaStatementResponse;

import org.apache.commons.lang3.ObjectUtils;

@APICommand(name = "quotaStatement", responseObject = QuotaStatementItemResponse.class, description = "Create a Quota statement for the provided Account, Project, or Domain.",
        since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, httpMethod = "GET")
public class QuotaStatementCmd extends BaseCmd {

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING,
            description = "Name of the Account for which the Quota statement will be generated. Deprecated, please use accountid instead.")
    private String accountName;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "ID of the Domain for which the Quota statement will be generated. May be used individually or with account.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = true, description = "End of the period of the Quota statement. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "Start of the period of the Quota statement. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.INTEGER,
            description = "Consider only Quota usage records for the specified usage type in the statement.")
    private Integer usageType;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            description = "ID of the Account for which the Quota statement will be generated. Can not be specified with projectid.")
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "ID of the Project for which the Quota statement will be generated. Can not be specified with accountid.", , since = "4.23.0")
    private Long projectId;

    @Parameter(name = ApiConstants.SHOW_RESOURCES, type = CommandType.BOOLEAN, description = "List the resources of each Quota type in the period.", since = "4.23.0")
    private boolean showResources;

    @Inject
    QuotaResponseBuilder responseBuilder;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
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

    public boolean isShowResources() {
        return showResources;
    }

    public void setShowResources(boolean showResources) {
        this.showResources = showResources;
    }

    public Long getProjectId() {
        return projectId;
    }

    @Override
    public long getEntityOwnerId() {
        if (ObjectUtils.allNull(accountId, accountName, projectId)) {
            return -1;
        }
        return _accountService.finalizeAccountId(accountId, accountName, domainId, projectId);
    }

    @Override
    public void execute() {
        QuotaStatementResponse response = responseBuilder.createQuotaStatementResponse(this);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
