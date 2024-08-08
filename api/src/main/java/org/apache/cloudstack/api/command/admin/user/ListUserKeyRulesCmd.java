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

package org.apache.cloudstack.api.command.admin.user;


import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import java.util.List;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPairPermission;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ApiKeyPairResponse;
import org.apache.cloudstack.api.response.BaseRolePermissionResponse;
import org.apache.cloudstack.api.response.ListResponse;

@APICommand(name = "listUserKeyRules",
        description = "This command allows the user to query the rules defined for a API access keypair.",
        responseObject = BaseRolePermissionResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.18.0.11-scclouds")

public class ListUserKeyRulesCmd extends BaseCmd {

    @ACL
    @Parameter(name=ApiConstants.ID, type = CommandType.UUID,  entityType = ApiKeyPairResponse.class, description = "ID of the keypair.", required = true)
    private Long id;

    public Long getId() {
        return id;
    }

    public long getEntityOwnerId() {
        ApiKeyPair keyPair = apiKeyPairService.findById(getId());
        if (keyPair != null) {
            return keyPair.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException {
        ApiKeyPair keyPair = apiKeyPairService.findById(getId());
        if (keyPair == null) {
            throw new InvalidParameterValueException(String.format("No keypair found with the id [%s].", getId()));
        }
        apiKeyPairService.validateCallingUserHasAccessToDesiredUser(keyPair.getUserId());

        List<ApiKeyPairPermission> permissions = apiKeyPairService.findAllPermissionsByKeyPairId(keyPair.getId());
        ListResponse<BaseRolePermissionResponse> response = _responseGenerator.createKeypairPermissionsResponse(permissions);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
