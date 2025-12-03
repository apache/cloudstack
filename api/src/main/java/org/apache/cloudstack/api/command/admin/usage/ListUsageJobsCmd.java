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

import java.util.Date;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UsageJobResponse;

@APICommand(name = "listUsageJobs", description = "Lists the usage jobs.", responseObject = UsageJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.23")
public class ListUsageJobsCmd extends BaseListCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "The start date from which the usage jobs should be listed. Only jobs started on or after this date will be included. (use format \"yyyy-MM-dd'T'HH:mm:ss'+'SSSS\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "The end date up to which the usage jobs should be listed. Only jobs started on or before this date will be included. (use format \"yyyy-MM-dd'T'HH:mm:ss'+'SSSS\")")
    private Date endDate;

    @Parameter(name = ApiConstants.USAGE_SERVER, type = CommandType.STRING, description = "The usage server host name or ip")
    private String usageServer;

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, description = "the duration in hours to list the usage jobs started or completed within that period up to now.")
    private Integer duration;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getUsageServer() {
        return usageServer;
    }

    public Integer getDuration() {
        return duration;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        ListResponse<UsageJobResponse> response = _usageService.getUsageJobs(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
