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
package org.apache.cloudstack.api.command.user.bucket;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.object.Bucket;

@APICommand(name = "revokeBucketKey", description = "Revokes the key pair in one slot of a bucket's dedicated credential. A bucket always keeps one active key, so the last active key cannot be revoked: create a key in the other slot first.",
        responseObject = SuccessResponse.class, entityType = {Bucket.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "24.0.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RevokeBucketKeyCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BucketResponse.class,
            required = true, description = "The ID of the Bucket")
    private Long id;

    @Parameter(name = ApiConstants.KEY_SLOT, type = CommandType.INTEGER, required = true,
            description = "The key slot (1 or 2) to revoke")
    private Integer keySlot;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getKeySlot() {
        return keySlot;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Bucket bucket = _entityMgr.findById(Bucket.class, getId());
        if (bucket != null) {
            return bucket.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Bucket;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Bucket ID: " + getResourceUuid(ApiConstants.ID) + " key slot: " + keySlot);
        boolean result;
        try {
            result = _bucketService.revokeBucketKey(this, CallContext.current().getCallingAccount());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error while revoking bucket key. " + e.getMessage());
        }
        if (result) {
            setResponseObject(new SuccessResponse(getCommandName()));
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to revoke bucket key");
        }
    }
}
