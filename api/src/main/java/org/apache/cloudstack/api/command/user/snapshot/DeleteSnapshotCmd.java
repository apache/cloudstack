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
package org.apache.cloudstack.api.command.user.snapshot;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;

@APICommand(name = "deleteSnapshot", description = "Deletes a snapshot of a disk volume.", responseObject = SuccessResponse.class, entityType = {Snapshot.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteSnapshotCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSnapshotCmd.class.getName());
    private static final String s_name = "deletesnapshotresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = SnapshotResponse.class,
            required=true, description="The ID of the snapshot")
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

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Snapshot snapshot = _entityMgr.findById(Snapshot.class, getId());
        if (snapshot != null) {
            return snapshot.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting snapshot: " + this._uuidMgr.getUuid(Snapshot.class, getId());
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Snapshot;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Snapshot Id: " + this._uuidMgr.getUuid(Snapshot.class, getId()));
        boolean result = _snapshotService.deleteSnapshot(getId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete snapshot");
        }
    }
}
