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
// under the License.import org.apache.cloudstack.context.CallContext;
package org.apache.cloudstack.api.command.user.consoleproxy;

import org.apache.cloudstack.consoleproxy.ConsoleSession;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserAccount;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ConsoleSessionResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.consoleproxy.ConsoleAccessManager;

import javax.inject.Inject;
import java.util.Date;

@APICommand(name = "listConsoleSessions", description = "Lists console sessions.", responseObject = ConsoleSessionResponse.class,
        entityType = {ConsoleSession.class}, since = "4.21.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class ListConsoleSessionsCmd extends BaseListCmd {
    @Inject
    private AccountService accountService;

    @Inject
    private ConsoleAccessManager consoleAccessManager;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ConsoleSessionResponse.class, description = "The ID of the console session.")
    private Long id;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "The domain ID of the account that created the console endpoint.")
    private Long domainId;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "The ID of the account that created the console endpoint.")
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "The ID of the user that created the console endpoint.")
    private Long userId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, authorized = {RoleType.Admin}, description = "Lists console sessions from the specified host.")
    private Long hostId;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Lists console sessions generated from this date onwards. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "Lists console sessions generated up until this date. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "The ID of the virtual machine.")
    private Long vmId;

    @Parameter(name = ApiConstants.CONSOLE_ENDPOINT_CREATOR_ADDRESS, type = CommandType.STRING, description = "IP address of the creator of the console endpoint.")
    private String consoleEndpointCreatorAddress;

    @Parameter(name = ApiConstants.CLIENT_ADDRESS, type = CommandType.STRING, description = "IP address of the client that accessed the console session.")
    private String clientAddress;

    @Parameter(name = ApiConstants.ACTIVE_ONLY, type = CommandType.BOOLEAN,
            description = "Lists only active console sessions, defaults to true. Active sessions are the ones that have been acquired and have not been removed.")
    private boolean activeOnly = true;

    @Parameter(name = ApiConstants.ACQUIRED, type = CommandType.BOOLEAN,
        description = "Lists acquired console sessions, defaults to false. Acquired console sessions are the ones that have been accessed. " +
                "The 'activeonly' parameter has precedence over the 'acquired' parameter, i.e., when the 'activeonly' parameter is 'true', the 'acquired' parameter value will be ignored.")
    private boolean acquired = false;

    @Parameter(name = ApiConstants.IS_RECURSIVE, type = CommandType.BOOLEAN,
            description = "Lists console sessions recursively per domain. If an account ID is informed, only the account's console sessions will be listed. Defaults to false.")
    private boolean recursive = false;

    public Long getId() {
        return id;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getConsoleEndpointCreatorAddress() {
        return consoleEndpointCreatorAddress;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public boolean isActiveOnly() {
        return activeOnly;
    }

    public boolean getAcquired() {
        return acquired;
    }

    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public void execute() {
        ListResponse<ConsoleSessionResponse> response = consoleAccessManager.listConsoleSessions(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        if (getId() != null) {
            ConsoleSession consoleSession = consoleAccessManager.listConsoleSessionById(getId());
            if (consoleSession != null) {
                return consoleSession.getAccountId();
            }
        }

        if (getAccountId() != null) {
            return getAccountId();
        }

        if (getUserId() != null) {
            UserAccount userAccount = accountService.getUserAccountById(getUserId());
            if (userAccount != null) {
                return userAccount.getAccountId();
            }
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }
}
