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
package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "rotateObjectStoreAccountKey", description = "Replaces the account-level key CloudStack holds for an account on an object store with a fresh one and revokes the old key. Completes the migration to per-bucket credentials: only allowed once no bucket of the account on that store still uses the account key. Any consumer still configured with the old account key loses access immediately.",
        responseObject = SuccessResponse.class, entityType = {Account.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "24.0.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin})
public class RotateObjectStoreAccountKeyCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            required = true, description = "The ID of the account whose key to rotate")
    private Long accountId;

    @Parameter(name = ApiConstants.OBJECT_STORAGE_ID, type = CommandType.UUID, entityType = ObjectStoreResponse.class,
            required = true, description = "The ID of the object store")
    private Long objectStoreId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getAccountId() {
        return accountId;
    }

    public Long getObjectStoreId() {
        return objectStoreId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account account = _entityMgr.findById(Account.class, getAccountId());
        if (account != null) {
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return accountId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Account;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Account ID: " + getResourceUuid(ApiConstants.ACCOUNT_ID) + " object store ID: " + getResourceUuid(ApiConstants.OBJECT_STORAGE_ID));
        boolean result;
        try {
            result = _bucketService.rotateObjectStoreAccountKey(this, CallContext.current().getCallingAccount());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error while rotating the account key on the object store. " + e.getMessage());
        }
        if (result) {
            setResponseObject(new SuccessResponse(getCommandName()));
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to rotate the account key on the object store");
        }
    }
}
