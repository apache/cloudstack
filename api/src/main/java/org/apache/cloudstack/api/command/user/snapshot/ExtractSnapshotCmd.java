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
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "extractSnapshot", description = "Returns a download URL for extracting a snapshot. It must be in the Backed Up state.", since = "4.20.0",
        responseObject = ExtractResponse.class, entityType = {Snapshot.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ExtractSnapshotCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=SnapshotResponse.class, required=true, since="4.20.0", description="the ID of the snapshot")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, since="4.20.0",
            description = "the ID of the zone where the snapshot is located")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Snapshot;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    /**
     * @return ID of the snapshot to extract, if any. Otherwise returns the ACCOUNT_ID_SYSTEM, so ERROR events will be traceable.
     */
    @Override
    public long getEntityOwnerId() {
        Snapshot snapshot = _entityMgr.findById(Snapshot.class, getId());
        if (snapshot != null) {
            return snapshot.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_EXTRACT;
    }

    @Override
    public String getEventDescription() {
        return "Snapshot extraction job";
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Snapshot ID: " + this._uuidMgr.getUuid(Snapshot.class, getId()));
        String uploadUrl = _snapshotService.extractSnapshot(this);
        logger.info("Extract URL [{}] of snapshot [{}].", uploadUrl, id);
        if (uploadUrl != null) {
            ExtractResponse response = _responseGenerator.createSnapshotExtractResponse(id, zoneId, getEntityOwnerId(), uploadUrl);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to extract snapshot");
        }
    }
}
