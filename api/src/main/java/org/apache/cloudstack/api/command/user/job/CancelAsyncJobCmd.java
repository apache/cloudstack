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


import com.cloud.event.EventTypes;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AsyncJobResponse;

import com.cloud.user.Account;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.jobs.AsyncJobService;

import javax.inject.Inject;

@APICommand(name = CancelAsyncJobCmd.APINAME, description = "Cancels the asynchronous job.", responseObject = AsyncJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin}, since = "4.23")
public class CancelAsyncJobCmd extends BaseCmd {
    public static final String APINAME = "cancelAsyncJob";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.JOB_ID, type = CommandType.UUID, entityType = AsyncJobResponse.class, required = true, description = "the ID of the asynchronous job")
    private Long id;

    @Inject
    private AsyncJobService asyncJobService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    public String getEventType() {
        return EventTypes.EVENT_JOB_CANCEL;
    }

    public String getEventDescription() {
        return "Cancelling job with id: " + id;
    }

    public ApiCommandResourceType getInstanceType() {
        return ApiCommandResourceType.Job;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() {
        String status = asyncJobService.cancelAsyncJob(id, "Cancel requested by " + CallContext.current().getCallingUser().toString());
        if (StringUtils.isBlank(status)) {
            AsyncJobResponse response = _responseGenerator.cancelJobResponse(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, status);
        }
    }
}
