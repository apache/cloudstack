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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

@APICommand(name = "createSnapshot", description = "Creates an instant snapshot of a volume.", responseObject = SnapshotResponse.class)
public class CreateSnapshotCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotCmd.class.getName());
    private static final String s_name = "createsnapshotresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "The account of the snapshot. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the disk volume.")
    private Long domainId;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class,
            required = true, description = "The ID of the disk volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.POLICY_ID, type = CommandType.UUID, entityType = SnapshotPolicyResponse.class,
            description = "policy id of the snapshot, if this is null, then use MANUAL_POLICY.")
    private Long policyId;

    private String syncObjectType = BaseAsyncCmd.snapshotHostSyncObject;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////


    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getPolicyId() {
        if (policyId != null) {
            return policyId;
        } else {
            return Snapshot.MANUAL_POLICY_ID;
        }
    }

    private Long getHostId() {
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume by id");
        }
        return _snapshotService.getHostIdForSnapshotOperation(volume);
    }


    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "snapshot";
    }

    @Override
    public long getEntityOwnerId() {

        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume by id=" + volumeId);
        }

        Account account = _accountService.getAccount(volume.getAccountId());
        //Can create templates for enabled projects/accounts only
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            Project project = _projectService.findByProjectAccountId(volume.getAccountId());
            if (project.getState() != Project.State.Active) {
                throw new PermissionDeniedException("Can't add resources to the project id=" + project.getId() + " in state=" + project.getState() + " as it's no longer active");
            }
        } else if (account.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of template is disabled: " + account);
        }

        return volume.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating snapshot for volume: " + getVolumeId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Snapshot;
    }

    @Override
    public void create() throws ResourceAllocationException {
        Snapshot snapshot = this._volumeService.allocSnapshot(getVolumeId(), getPolicyId());
        if (snapshot != null) {
            this.setEntityId(snapshot.getId());
            this.setEntityUuid(snapshot.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot");
        }
    }

    @Override
    public void execute() {
        s_logger.info("VOLSS: createSnapshotCmd starts:" + System.currentTimeMillis());
        CallContext.current().setEventDetails("Volume Id: "+getVolumeId());
        Snapshot snapshot;
        try {
            snapshot = _volumeService.takeSnapshot(this.getVolumeId(), this.getPolicyId(), this.getEntityId(), _accountService.getAccount(getEntityOwnerId()));
            if (snapshot != null) {
                SnapshotResponse response = _responseGenerator.createSnapshotResponse(snapshot);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot for volume " + volumeId);
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot for volume " + volumeId);
        }
    }


    @Override
    public String getSyncObjType() {
        if (getSyncObjId() != null) {
            return syncObjectType;
        }
        return null;
    }

    @Override
    public Long getSyncObjId() {
        if (getHostId() != null) {
            return getHostId();
        }
        return null;
    }
}
