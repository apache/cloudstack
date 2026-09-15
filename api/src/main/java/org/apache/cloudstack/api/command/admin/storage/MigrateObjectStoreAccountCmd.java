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

@APICommand(name = "migrateObjectStoreAccount", description = "Migrates an account's identity on an object store into the state its provider requires for per-bucket credentials. On Ceph RGW this creates an RGW account and adopts the account's existing RGW user into it as the account root, which transfers ownership of all its buckets to the RGW account and is permanent. Existing buckets keep working with their current keys; they gain dedicated credentials individually via migrateBucketCredential.",
        responseObject = SuccessResponse.class, entityType = {Account.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "24.0.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin})
public class MigrateObjectStoreAccountCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            required = true, description = "The ID of the account to migrate")
    private Long accountId;

    @Parameter(name = ApiConstants.OBJECT_STORAGE_ID, type = CommandType.UUID, entityType = ObjectStoreResponse.class,
            required = true, description = "The ID of the object store on which to migrate the account")
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
            result = _bucketService.migrateObjectStoreAccount(this, CallContext.current().getCallingAccount());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error while migrating account on object store. " + e.getMessage());
        }
        if (result) {
            setResponseObject(new SuccessResponse(getCommandName()));
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate account on object store");
        }
    }
}
