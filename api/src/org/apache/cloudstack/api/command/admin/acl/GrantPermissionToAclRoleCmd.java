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
package org.apache.cloudstack.api.command.admin.acl;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclRoleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "grantPermissionToAclRole", description = "Grant api permission to an acl role", responseObject = AclRoleResponse.class)
public class GrantPermissionToAclRoleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(GrantPermissionToAclRoleCmd.class.getName());
    private static final String s_name = "grantpermissiontoroleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AclRoleResponse.class,
            required = true, description = "The ID of the acl role")
    private Long id;

    @ACL
    @Parameter(name = ApiConstants.ACL_APIS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "comma separated list of apis granted to the acl role. ")
    private List<String> apiList;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }


    public List<String> getApiList() {
        return apiList;
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
        CallContext.current().setEventDetails("Acl role Id: " + getId());
        AclRole result = _aclService.grantApiPermissionToAclRole(id, apiList);
        if (result != null) {
            AclRoleResponse response = _responseGenerator.createAclRoleResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to grant permission to acl role " + getId());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACL_ROLE_GRANT;
    }

    @Override
    public String getEventDescription() {
        return "granting permission to acl role";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclRole;
    }

}
