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

import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclRoleResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;

@APICommand(name = "createAclRole", responseObject = AclRoleResponse.class, description = "Creates an acl role")
public class CreateAclRoleCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAclRoleCmd.class.getName());

    private static final String s_name = "createaclroleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, description = "domainId of the account owning the acl role", entityType = DomainResponse.class)
    private Long domainId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "optional description of the acl role")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the acl group")
    private String name;

    @ACL
    @Parameter(name = ApiConstants.ACL_PARENT_ROLE_ID, type = CommandType.UUID, description = "The ID of parent acl role.", entityType = AclRoleResponse.class)
    private Long parentRoleId;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////


    public String getDescription() {
        return description;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getName() {
        return name;
    }

    public Long getParentRoleId() {
        return parentRoleId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() {
        AclRole role = _entityMgr.findById(AclRole.class, getEntityId());
        if (role != null) {
            AclRoleResponse response = _responseGenerator.createAclRoleResponse(role);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create acl role:" + name);
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        AclRole result = _aclService.createAclRole(domainId, name, description, parentRoleId);
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create acl role entity" + name);
        }

    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACL_ROLE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating Acl role";
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_ACL_ROLE_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating acl role";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclRole;
    }

}
