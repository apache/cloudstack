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

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiKeyPairResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

@APICommand(name = "deleteUserKeys", description = "Deletes a keypair from a user", responseObject = SuccessResponse.class,
        since = "4.18.0.11-scclouds", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteUserKeysCmd extends BaseAsyncCmd {

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ApiKeyPairResponse.class, required = true, description = "ID of the keypair to be deleted.")
    private Long id;

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }

    @Override
    public long getEntityOwnerId() {
        ApiKeyPair keyPair = apiKeyPairService.findById(id);
        if (keyPair != null) {
            return keyPair.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private Long getId() {
        return id;
    }

    @Override
    public void execute() {
        ApiKeyPair keyPair = apiKeyPairService.findById(getId());
        if (keyPair == null) {
            throw new InvalidParameterValueException(String.format("No keypair found with the id [%s].", getId()));
        }
        apiKeyPairService.validateCallingUserHasAccessToDesiredUser(keyPair.getUserId());

        apiKeyPairService.deleteApiKey(keyPair);

        SuccessResponse response = new SuccessResponse(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DELETE_SECRET_API_KEY;
    }

    @Override
    public String getEventDescription() {
        return ("Deleting API keypair " + id);
    }

    @Override
    public Long getSyncObjId() {
        return getId();
    }
}
