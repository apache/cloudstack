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

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.iam.IAMApiService;
import org.apache.cloudstack.iam.api.IAMPolicy;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "removeIAMPermissionFromIAMPolicy", description = "Remove iam permission from an iam policy", responseObject = IAMPolicyResponse.class)
public class RemoveIAMPermissionFromIAMPolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveIAMPermissionFromIAMPolicyCmd.class.getName());
    private static final String s_name = "removeiampermissionfromiampolicyresponse";

    @Inject
    public IAMApiService _iamApiSrv;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = IAMPolicyResponse.class,
            required = true, description = "The ID of the iam policy")
    private Long id;

    @Parameter(name = ApiConstants.IAM_ACTION, type = CommandType.STRING, required = true, description = "action api name.")
    private String action;

    @Parameter(name = ApiConstants.ENTITY_TYPE, type = CommandType.STRING, required = false, description = "entity class simple name.")
    private String entityType;

    @Parameter(name = ApiConstants.IAM_SCOPE, type = CommandType.STRING,
            required = false, description = "iam permission scope")
    private String scope;

    @Parameter(name = ApiConstants.IAM_SCOPE_ID, type = CommandType.STRING, required = false, description = "The ID of the permission scope id")
    private String scopeId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }


    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getScope() {
        return scope;
    }

    public Long getScopeId() {
        // here we will convert the passed String UUID to Long ID since internally we store it as entity internal ID.
        return _iamApiSrv.getPermissionScopeId(scope, entityType, scopeId);
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
        CallContext.current().setEventDetails("IAM policy Id: " + getId());
        IAMPolicy result = _iamApiSrv.removeIAMPermissionFromIAMPolicy(id, entityType, PermissionScope.valueOf(scope), getScopeId(), action);
        if (result != null) {
            IAMPolicyResponse response = _iamApiSrv.createIAMPolicyResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove permission from iam policy " + getId());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IAM_POLICY_REVOKE;
    }

    @Override
    public String getEventDescription() {
        return "removing permission from iam policy";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IAMPolicy;
    }

}
