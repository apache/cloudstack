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
package org.apache.cloudstack.api.command.user.vpn;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "removeVpnUser", description="Removes vpn user", responseObject=SuccessResponse.class)
public class RemoveVpnUserCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveVpnUserCmd.class.getName());

    private static final String s_name = "removevpnuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="username for the vpn user")
    private String userName;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType=ProjectResponse.class,
            description="remove vpn user from the project")
    private Long projectId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="an optional domainId for the vpn user. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getUserName() {
        return userName;
    }

    public Long getProjecId() {
        return projectId;
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
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventDescription() {
        return "Remove Remote Access VPN user for account " + getEntityOwnerId() + " username= " + getUserName();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPN_USER_REMOVE;
    }

    @Override
    public void execute(){
        Account owner = _accountService.getAccount(getEntityOwnerId());
        boolean result = _ravService.removeVpnUser(owner.getId(), userName, CallContext.current().getCallingAccount());
        if (!result) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove vpn user");
        }

        if (!_ravService.applyVpnUsers(owner.getId(), userName)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to apply vpn user removal");
        }
        SuccessResponse response = new SuccessResponse(getCommandName());
        setResponseObject(response);
    }
}
