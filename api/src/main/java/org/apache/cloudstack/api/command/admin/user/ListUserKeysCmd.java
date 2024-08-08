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


import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListDomainResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ApiKeyPairResponse;
import org.apache.cloudstack.api.response.UserResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@APICommand(name = "listUserKeys",
        description = "This command allows the user to list the API Key pairs (api and secret keys) for a user",
        responseObject = ApiKeyPairResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.User, RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin},
        since = "4.10.0")

public class ListUserKeysCmd extends BaseListDomainResourcesCmd {

    @ACL
    @Parameter(name=ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "ID of the user that owns the keys.")
    private Long userId;

    @ACL
    @Parameter(name=ApiConstants.KEYPAIR_ID, type = CommandType.UUID, entityType = ApiKeyPairResponse.class, description = "ID of the keypair.")
    private Long keyPairId;

    protected Logger logger = LogManager.getLogger(getClass());

    public Long getUserId() {
        return userId;
    }

    public Long getKeyId() {
        return keyPairId;
    }

    public long getEntityOwnerId() {
        if (getKeyId() != null) {
            ApiKeyPair keypair = apiKeyPairService.findById(getKeyId());
            if (keypair != null) {
                return keypair.getAccountId();
            }
        } else if (getUserId() != null) {
            UserAccount userAccount = _accountService.getUserAccountById(getUserId());
            if (userAccount != null) {
                return userAccount.getAccountId();
            }
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public void execute() {
        ListResponse<ApiKeyPairResponse> finalResponse = _accountService.getKeys(this);
        finalResponse.setObjectName("userkeys");
        finalResponse.setResponseName(getCommandName());
        setResponseObject(finalResponse);
    }
}