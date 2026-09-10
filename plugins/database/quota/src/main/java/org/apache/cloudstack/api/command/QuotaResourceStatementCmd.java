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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaResourceStatementResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;

import java.util.Date;

@APICommand(name = "quotaResourceStatement", responseObject = QuotaResourceStatementResponse.class, since = "4.23.0.0",
        description = "Generates a detailed Quota statement for a specific resource.", requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class QuotaResourceStatementCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true, description = "ID of the resource.")
    private String id;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            description = "Generate the statement for this Account. A resource may have belonged to different owners at distinct points in time, " +
                    "so this parameter can be used to only consider the period for which it belonged to a specific Account.")
    private Long accountId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "Generate the statement for this Project. A resource may have belonged to different owners at distinct points in time, " +
                    "so this parameter can be used to only consider the period for which it belonged to a specific Project.")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "ID of the Domain for which the Quota statement will be generated.")
    private Long domainId;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = true, description = "Usage type of the Quota usage.")
    private Integer usageType;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "Start date for the query. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = true, description = "End date for the query. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.IS_RECURSIVE, type = CommandType.BOOLEAN, description = "Whether to include usage records from children of the filtered domain. "
            + " Defaults to false.")
    private Boolean recursive;

    @Inject
    QuotaResponseBuilder responseBuilder;

    @Override
    public void execute() {
        QuotaResourceStatementResponse response = responseBuilder.createQuotaResourceStatement(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        if (ObjectUtils.allNull(accountId, projectId)) {
            return -1;
        }
        return _accountService.finalizeAccountId(accountId, null, null, projectId);
    }

    public String getId() {
        return id;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public boolean isRecursive() {
        return BooleanUtils.isTrue(recursive);
    }

}
