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
package org.apache.cloudstack.api.command.user.volume;

import com.cloud.storage.StoragePool;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

@APICommand(name = "migrateVolume", description = "Migrate volume", responseObject = VolumeResponse.class, since = "3.0.0", responseView = ResponseView.Restricted, entityType = {
        Volume.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class MigrateVolumeCmd extends BaseAsyncCmd implements UserCmd {
    private static final String s_name = "migratevolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "the ID of the volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, required = true, description = "destination storage pool ID to migrate the volume to")
    private Long storageId;

    @Parameter(name = ApiConstants.LIVE_MIGRATE, type = CommandType.BOOLEAN, required = false, description = "if the volume should be live migrated when it is attached to a running vm")
    private Boolean liveMigrate;

    @Parameter(name = ApiConstants.NEW_DISK_OFFERING_ID, type = CommandType.STRING, description = "The new disk offering ID that replaces the current one used by the volume. This new disk offering is used to better reflect the new storage where the volume is going to be migrated to.")
    private String newDiskOfferingUuid;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    // TODO remove this in 5.0 and use id as param instead.
    public Long getVolumeId() {
        return volumeId;
    }

    public Long getId() {
        return getVolumeId();
    }

    public Long getStoragePoolId() {
        return storageId;
    }

    public boolean isLiveMigrate() {
        return (liveMigrate != null) ? liveMigrate : false;
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
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume != null) {
            return volume.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        return "Attempting to migrate volume Id: " + this._uuidMgr.getUuid(Volume.class, getVolumeId()) + " to storage pool Id: " + this._uuidMgr.getUuid(StoragePool.class, getStoragePoolId());
    }

    public String getNewDiskOfferingUuid() {
        return newDiskOfferingUuid;
    }

    @Override
    public void execute() {
        Volume result;

        result = _volumeService.migrateVolume(this);
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(getResponseView(), result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate volume");
        }
    }

    @Override
    public String getSyncObjType() {
        return (getSyncObjId() != null) ? BaseAsyncCmd.migrationSyncObject : null;
    }

    @Override
    public Long getSyncObjId() {
        if (getStoragePoolId() != null) {
            return getStoragePoolId();
        }
        return null;
    }
}
