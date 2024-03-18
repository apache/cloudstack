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

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "archiveSnapshot", description = "Archives (moves) a snapshot on primary storage to secondary storage",
        responseObject = SnapshotResponse.class, entityType = {Snapshot.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ArchiveSnapshotCmd extends BaseAsyncCmd {
    private static final String s_name = "createsnapshotresponse";

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = SnapshotResponse.class,
            required=true, description="The ID of the snapshot")
    private Long id;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Archiving snapshot " + id + " to secondary storage";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        CallContext.current().setEventDetails("Snapshot Id: " + this._uuidMgr.getUuid(Snapshot.class,getId()));
        Snapshot snapshot = _snapshotService.archiveSnapshot(getId());
        if (snapshot != null) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to archive snapshot");
        }
    }

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

    public Long getId() {
        return id;
    }
}
