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
package org.apache.cloudstack.api.command.user.network;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.Account;

import java.util.List;

@APICommand(name = RemoveNetworkPermissionsCmd.APINAME, description = "Removes network permissions.",
        responseObject = SuccessResponse.class,
        entityType = {Network.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.17.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RemoveNetworkPermissionsCmd extends BaseCmd {
    public static final Logger LOGGER = Logger.getLogger(RemoveNetworkPermissionsCmd.class.getName());

    public static final String APINAME = "removeNetworkPermissions";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNTS,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "a comma delimited list of accounts within owner's domain. If specified, \"op\" parameter has to be passed in.")
    private List<String> accountNames;

    @Parameter(name = ApiConstants.ACCOUNT_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = AccountResponse.class,
            description = "a comma delimited list of account IDs within owner's domain. If specified, \"op\" parameter has to be passed in.")
    private List<Long> accountIds;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "the network ID")
    private Long networkId;

    @Parameter(name = ApiConstants.PROJECT_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "a comma delimited list of projects within owner's domain. If specified, \"op\" parameter has to be passed in.")
    private List<Long> projectIds;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public List<String> getAccountNames() {
        return accountNames;
    }

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public List<Long> getProjectIds() {
        return projectIds;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        if (accountIds != null && accountNames != null) {
            throw new InvalidParameterValueException("Accounts and accountNames can't be specified together");
        }
        boolean result = _networkService.removeNetworkPermissions(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update network permissions");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Network network = _entityMgr.findById(Network.class, getNetworkId());
        if (network != null) {
            return network.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
