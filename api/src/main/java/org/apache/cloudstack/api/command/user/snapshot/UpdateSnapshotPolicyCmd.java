package org.apache.cloudstack.api.command.user.snapshot;

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

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.context.CallContext;


@APICommand(name = "updateSnapshotPolicy", description = "Updates the snapshot policy.", responseObject = SnapshotPolicyResponse.class, responseView = ResponseObject.ResponseView.Restricted, entityType = {Volume.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateSnapshotPolicyCmd extends BaseAsyncCustomIdCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name= ApiConstants.ID, type=CommandType.UUID, entityType=SnapshotPolicyResponse.class, description="the ID of the snapshot policy")
    private Long id;

    @Parameter(name = ApiConstants.FOR_DISPLAY,
            type = CommandType.BOOLEAN,
            description = "an optional field, whether to the display the snapshot policy to the end user or not.",
            since = "4.4",
            authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getDisplay() {
        return display;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {

        SnapshotPolicy policy = _entityMgr.findById(SnapshotPolicy.class, getId());
        if (policy == null) {
            throw new InvalidParameterValueException("Invalid snapshot policy id was provided");
        }
        Volume volume = _responseGenerator.findVolumeById(policy.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Snapshot policy's volume id doesn't exist");
        }else{
            return volume.getAccountId();
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_POLICY_UPDATE;
    }

    @Override
    public String getEventDescription() {
        StringBuilder desc = new StringBuilder("Updating snapshot policy: ");
        desc.append(getId());
        return desc.toString();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("SnapshotPolicy Id: " + getId());
        SnapshotPolicy result = _snapshotService.updateSnapshotPolicy(this);
        if (result != null) {
            SnapshotPolicyResponse response = _responseGenerator.createSnapshotPolicyResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update snapshot policy");
        }
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), SnapshotPolicy.class);
        }
    }

}
