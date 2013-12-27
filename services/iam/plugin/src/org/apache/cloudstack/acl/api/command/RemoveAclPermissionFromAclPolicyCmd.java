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
package org.apache.cloudstack.acl.api.command;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclPolicy;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclPolicyResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "removeAclPermissionFromAclPolicy", description = "Remove acl permission from an acl policy", responseObject = AclPolicyResponse.class)
public class RemoveAclPermissionFromAclPolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveAclPermissionFromAclPolicyCmd.class.getName());
    private static final String s_name = "removeaclpermissionfromaclpolicyresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AclPolicyResponse.class,
            required = true, description = "The ID of the acl policy")
    private Long id;

    @Parameter(name = ApiConstants.ACL_ACTION, type = CommandType.STRING, required = true, description = "action api name.")
    private String action;

    @Parameter(name = ApiConstants.ENTITY_TYPE, type = CommandType.STRING, required = false, description = "entity class simple name.")
    private String entityType;

    @Parameter(name = ApiConstants.ACL_SCOPE, type = CommandType.STRING,
            required = false, description = "acl permission scope")
    private String scope;

    @Parameter(name = ApiConstants.ACL_SCOPE_ID, type = CommandType.UUID, required = false, description = "The ID of the permission scope id")
    private Long scopeId;


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
        return scopeId;
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
        CallContext.current().setEventDetails("Acl policy Id: " + getId());
        AclPolicy result = _aclService.removeAclPermissionFromAclPolicy(id, entityType, PermissionScope.valueOf(scope), scopeId, action);
        if (result != null) {
            AclPolicyResponse response = _responseGenerator.createAclPolicyResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove permission from acl policy " + getId());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACL_POLICY_REVOKE;
    }

    @Override
    public String getEventDescription() {
        return "removing permission from acl policy";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclPolicy;
    }

}
