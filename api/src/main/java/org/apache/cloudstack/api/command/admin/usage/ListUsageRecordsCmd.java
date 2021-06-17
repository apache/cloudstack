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
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.usage.Usage;

import com.cloud.utils.Pair;

@APICommand(name = ListUsageRecordsCmd.APINAME,
        description = "Lists usage records for accounts",
        responseObject = UsageRecordResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class ListUsageRecordsCmd extends BaseListCmd {
    public static final String APINAME = "listUsageRecords";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "List usage records for the specified user.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "List usage records for the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE,
            type = CommandType.DATE,
            required = true,
            description = "End date range for usage record query (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\", e.g. startDate=2015-01-01 or startdate=2015-01-01 10:30:00).")
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE,
            type = CommandType.DATE,
            required = true,
            description = "Start date range for usage record query (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\", e.g. startDate=2015-01-01 or startdate=2015-01-01 11:00:00).")
    private Date startDate;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "List usage records for the specified account")
    private Long accountId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "List usage records for specified project")
    private Long projectId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.LONG, description = "List usage records for the specified usage type")
    private Long usageType;

    @Parameter(name = ApiConstants.USAGE_ID, type = CommandType.STRING, description = "List usage records for the specified usage UUID. Can be used only together with TYPE parameter.")
    private String usageId;

    @Parameter(name = ApiConstants.INCLUDE_TAGS, type = CommandType.BOOLEAN, description = "Flag to enable display of Tags for a resource")
    private Boolean includeTags;

    @Parameter(name = ApiConstants.OLD_FORMAT, type = CommandType.BOOLEAN, description = "Flag to enable description rendered in old format which uses internal database IDs instead of UUIDs. False by default.")
    private Boolean oldFormat;

    @Parameter(name = ApiConstants.IS_RECURSIVE, type = CommandType.BOOLEAN,
            description = "Specify if usage records should be fetched recursively per domain. If an account id is passed, records will be limited to that account.",
            since = "4.15")
    private Boolean recursive = false;

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

    public String getUsageId() {
        return usageId;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Boolean getIncludeTags() {
        return includeTags;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setUsageId(String usageId) {
        this.usageId = usageId;
    }

    public boolean getOldFormat() {
        return oldFormat != null && oldFormat;
    }

    public Boolean isRecursive() {
        return recursive;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        Pair<List<? extends Usage>, Integer> usageRecords = _usageService.getUsageRecords(this);
        ListResponse<UsageRecordResponse> response = new ListResponse<UsageRecordResponse>();
        List<UsageRecordResponse> usageResponses = new ArrayList<UsageRecordResponse>();
        Map<String, Set<ResourceTagResponse>> resourceTagResponseMap = null;
        if (usageRecords != null) {
            //read the resource tags details for all the resources in usage data and store in Map
            if (null != includeTags && includeTags) {
                resourceTagResponseMap = _responseGenerator.getUsageResourceTags();
            }
            for (Usage usageRecord : usageRecords.first()) {
                UsageRecordResponse usageResponse = _responseGenerator.createUsageResponse(usageRecord, resourceTagResponseMap, getOldFormat());
                if (usageResponse != null) {
                    usageResponse.setObjectName("usagerecord");
                    usageResponses.add(usageResponse);
                }
            }

            response.setResponses(usageResponses, usageRecords.second());
        }

        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
