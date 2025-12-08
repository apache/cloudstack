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

import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.storage.object.Bucket;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "deleteBucket", description = "Deletes an empty Bucket.", responseObject = SuccessResponse.class, entityType = {Bucket.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteBucketCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=BucketResponse.class,
            required=true, description="The ID of the Bucket")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public static String getResultObjectName() {
        return "bucket";
    }

    @Override
    public long getEntityOwnerId() {
        Bucket Bucket = _entityMgr.findById(Bucket.class, getId());
        if (Bucket != null) {
            return Bucket.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
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
    public void execute() throws ConcurrentOperationException {
        CallContext.current().setEventDetails("Bucket Id: " + this._uuidMgr.getUuid(Bucket.class, getId()));
        boolean result = _bucketService.deleteBucket(id, CallContext.current().getCallingAccount());
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }
}
