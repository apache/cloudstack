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
package org.apache.cloudstack.api.command.admin.usage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.usage.Usage;
import org.apache.log4j.Logger;

import com.cloud.utils.Pair;

@APICommand(name = "listUsageRecords", description="Lists usage records for accounts", responseObject=UsageRecordResponse.class)
public class GetUsageRecordsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(GetUsageRecordsCmd.class.getName());

    private static final String s_name = "listusagerecordsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="List usage records for the specified user.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="List usage records for the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.END_DATE, type=CommandType.DATE, required=true, description="End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date endDate;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.DATE, required=true, description="Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01.")
    private Date startDate;

    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class,
            description="List usage records for the specified account")
    private Long accountId;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            description="List usage records for specified project")
    private Long projectId;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.LONG, description="List usage records for the specified usage type")
    private Long usageType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getUsageType() {
        return usageType;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        Pair<List<? extends Usage>, Integer> usageRecords = _usageService.getUsageRecords(this);
        ListResponse<UsageRecordResponse> response = new ListResponse<UsageRecordResponse>();
        List<UsageRecordResponse> usageResponses = new ArrayList<UsageRecordResponse>();
        if (usageRecords != null) {
            for(Usage usageRecord: usageRecords.first()){
                UsageRecordResponse usageResponse = _responseGenerator.createUsageResponse(usageRecord);
                usageResponse.setObjectName("usagerecord");
                usageResponses.add(usageResponse);
            }
            response.setResponses(usageResponses, usageRecords.second());
        }
                
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
