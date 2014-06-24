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

import java.util.List;

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
import org.apache.cloudstack.api.response.iam.IAMGroupResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.iam.api.IAMGroup;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "removeIAMPolicyFromIAMGroup", description = "remove iam policy from an iam group", responseObject = IAMGroupResponse.class)
public class RemoveIAMPolicyFromIAMGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveIAMPolicyFromIAMGroupCmd.class.getName());
    private static final String s_name = "removeiampolicyfromiamgroupresponse";

    @Inject
    public IAMApiService _iamApiSrv;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = IAMGroupResponse.class,
 required = true, description = "The ID of the iam group")
    private Long id;

    @ACL
    @Parameter(name = ApiConstants.IAM_POLICIES, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = IAMPolicyResponse.class, description = "comma separated list of iam policy id that are going to be applied to the iam group.")
    private List<Long> policyIdList;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }


    public List<Long> getRoleIdList() {
        return policyIdList;
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
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException {
        CallContext.current().setEventDetails("IAM group Id: " + getId());
        IAMGroup result = _iamApiSrv.removeIAMPoliciesFromGroup(policyIdList, id);
        if (result != null){
            IAMGroupResponse response = _iamApiSrv.createIAMGroupResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add roles to iam group");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IAM_GROUP_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "removing IAM roles from IAM group";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IAMGroup;
    }

}
