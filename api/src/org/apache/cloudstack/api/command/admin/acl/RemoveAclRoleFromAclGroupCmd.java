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

import org.apache.cloudstack.acl.AclGroup;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclGroupResponse;
import org.apache.cloudstack.api.response.AclRoleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "removeAclRoleFromAclGroup", description = "remove acl role to an acl group", responseObject = AclGroupResponse.class)
public class RemoveAclRoleFromAclGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveAclRoleFromAclGroupCmd.class.getName());
    private static final String s_name = "removeaclroletoaclgroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AclGroupResponse.class,
            required = true, description = "The ID of the acl group")
    private Long id;

    @ACL
    @Parameter(name = ApiConstants.ACL_ROLES, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = AclRoleResponse.class, description = "comma separated list of acl role id that are going to be applied to the acl group.")
    private List<Long> roleIdList;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }


    public List<Long> getRoleIdList() {
        return roleIdList;
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
        AclGroup result = _aclService.removeAclRolesFromGroup(roleIdList, id);
        if (result != null){
            AclGroupResponse response = _responseGenerator.createAclGroupResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add roles to acl group");
        }
    }


}
