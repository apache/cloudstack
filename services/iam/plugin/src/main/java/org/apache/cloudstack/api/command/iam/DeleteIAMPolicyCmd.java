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
package org.apache.cloudstack.api.command.iam;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.iam.IAMApiService;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "deleteIAMPolicy", description = "Deletes iam policy", responseObject = SuccessResponse.class)
public class DeleteIAMPolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIAMPolicyCmd.class.getName());
    private static final String s_name = "deleteiampolicyresponse";

    @Inject
    public IAMApiService _iamApiSrv;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "The ID of the iam policy.", required = true, entityType = IAMPolicyResponse.class)
    private Long id;


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
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        boolean result = _iamApiSrv.deleteIAMPolicy(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete iam policy");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IAM_POLICY_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting IAM policy";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IAMPolicy;
    }
}
