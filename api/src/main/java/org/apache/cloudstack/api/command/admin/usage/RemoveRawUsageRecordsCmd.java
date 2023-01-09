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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

@APICommand(name = "removeRawUsageRecords", description = "Safely removes raw records from cloud_usage table", responseObject = SuccessResponse.class, since = "4.6.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RemoveRawUsageRecordsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveRawUsageRecordsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.INTERVAL, type = CommandType.INTEGER, required = true,
            description = "Specify the number of days (greater than zero) to remove records that are older than those number of days from today. For example, specifying 10 would result in removing all the records created before 10 days from today")
    private Integer interval;

    public Integer getInterval() {
        return interval;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

        boolean result = _usageService.removeRawUsageRecords(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to remove old raw usage records from cloud_usage table, a job is likely running at this time. Please try again after 15 mins.");
        }
    }
}
