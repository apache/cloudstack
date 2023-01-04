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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkPermissionsResponse;
import org.apache.cloudstack.api.response.NetworkResponse;

import com.cloud.network.Network;
import com.cloud.network.NetworkPermission;
import com.cloud.user.Account;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listNetworkPermissions", description = "List network visibility and all accounts that have permissions to view this network.",
        responseObject = NetworkPermissionsResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.17.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListNetworkPermissionsCmd extends BaseCmd implements UserCmd {
    public static final Logger LOGGER = Logger.getLogger(ListNetworkPermissionsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "Lists network permission by network ID")
    private Long networkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNetworkId() {
        return networkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        Network Network = _entityMgr.findById(Network.class, getNetworkId());
        if (Network != null) {
            return Network.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        List<? extends NetworkPermission> permissions = _networkService.listNetworkPermissions(this);
        ListResponse<NetworkPermissionsResponse> response = new ListResponse<>();
        List<NetworkPermissionsResponse> networkPermissionResponses = new ArrayList<>();
        for (NetworkPermission permission : permissions) {
            NetworkPermissionsResponse networkPermissionResponse = _responseGenerator.createNetworkPermissionsResponse(permission);
            networkPermissionResponses.add(networkPermissionResponse);
        }
        response.setResponses(networkPermissionResponses, permissions.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
