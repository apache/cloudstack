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

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AclRoleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "deleteAclRole", description = "Deletes acl role", responseObject = SuccessResponse.class)
public class DeleteAclRoleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteAclRoleCmd.class.getName());
    private static final String s_name = "deleteaclroleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "The ID of the acl role.", required = true, entityType = AclRoleResponse.class)
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
        boolean result = _aclService.deleteAclRole(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete acl role");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACL_ROLE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting Acl role";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclRole;
    }
}
