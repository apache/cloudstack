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


import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclGroup;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclGroupResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "revokePermissionFromAclGroup", description = "revoke entity permission from an acl group", responseObject = AclGroupResponse.class)
public class RevokePermissionFromAclGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RevokePermissionFromAclGroupCmd.class.getName());
    private static final String s_name = "revokepermissiontoaclgroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AclGroupResponse.class,
            required = true, description = "The ID of the acl group")
    private Long id;

    @Parameter(name = ApiConstants.ENTITY_TYPE, type = CommandType.STRING, required = true, description = "entity class simple name.")
    private String entityType;

    @Parameter(name = ApiConstants.ENTITY_ID, type = CommandType.UUID, required = true, description = "The ID of the entity")
    private Long entityId;

    @Parameter(name = ApiConstants.ACCESS_TYPE, type = CommandType.STRING, required = true, description = "access type for the entity")
    private String accessType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public AccessType getAccessType() {
        return AccessType.valueOf(accessType);
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
        CallContext.current().setEventDetails("Acl group Id: " + getId());
        AclGroup result = _aclService.revokeEntityPermissionFromAclGroup(id, entityType, entityId, getAccessType());
        if (result != null){
            AclGroupResponse response = _responseGenerator.createAclGroupResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to grant permission to acl group");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACL_GROUP_GRANT;
    }

    @Override
    public String getEventDescription() {
        return "granting permission to acl group";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclGroup;
    }

}
