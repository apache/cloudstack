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
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VpnUsersResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.network.VpnUser;
import com.cloud.user.Account;

@APICommand(name = "addVpnUser", description="Adds vpn users", responseObject=VpnUsersResponse.class)
public class AddVpnUserCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddVpnUserCmd.class.getName());

    private static final String s_name = "addvpnuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="username for the vpn user")
    private String userName;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=true, description="password for the username")
    private String password;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType=ProjectResponse.class,
            description="add vpn user to the specific project")
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

    public String getPassword() {
        return password;
    }

    public Long getProjectId() {
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
        return "Add Remote Access VPN user for account " + getEntityOwnerId() + " username= " + getUserName();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPN_USER_ADD;
    }

    @Override
    public void execute(){
        VpnUser vpnUser = _entityMgr.findById(VpnUser.class, getEntityId());
        Account account = _entityMgr.findById(Account.class, vpnUser.getAccountId());
        if (!_ravService.applyVpnUsers(vpnUser.getAccountId(), userName)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add vpn user");
        }

        VpnUsersResponse vpnResponse = new VpnUsersResponse();
        vpnResponse.setId(vpnUser.getUuid());
        vpnResponse.setUserName(vpnUser.getUsername());
        vpnResponse.setAccountName(account.getAccountName());

        Domain domain = _entityMgr.findById(Domain.class, account.getDomainId());
        if (domain != null) {
            vpnResponse.setDomainId(domain.getUuid());
            vpnResponse.setDomainName(domain.getName());
        }

        vpnResponse.setResponseName(getCommandName());
        vpnResponse.setObjectName("vpnuser");
        this.setResponseObject(vpnResponse);
    }

    @Override
    public void create() {
        Account owner = _accountService.getAccount(getEntityOwnerId());

        VpnUser vpnUser = _ravService.addVpnUser(owner.getId(), userName, password);
        if (vpnUser == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add vpn user");
        }
        setEntityId(vpnUser.getId());
        setEntityUuid(vpnUser.getUuid());
    }
}
