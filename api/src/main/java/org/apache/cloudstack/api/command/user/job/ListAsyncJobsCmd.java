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
package org.apache.cloudstack.api.command.user.job;

import java.util.Date;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;

@APICommand(name = "listAsyncJobs", description = "Lists all pending asynchronous jobs for the account.", responseObject = AsyncJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAsyncJobsCmd extends BaseListAccountResourcesCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "The start date of the async job (use format \"yyyy-MM-dd'T'HH:mm:ss'+'SSSS\")")
    private Date startDate;

    @Parameter(name = ApiConstants.MANAGEMENT_SERVER_ID, type = CommandType.UUID, entityType = ManagementServerResponse.class, description = "The id of the management server", since="4.19")
    private Long managementServerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Date getStartDate() {
        return startDate;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {

        ListResponse<AsyncJobResponse> response = _queryService.searchForAsyncJobs(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }
}
