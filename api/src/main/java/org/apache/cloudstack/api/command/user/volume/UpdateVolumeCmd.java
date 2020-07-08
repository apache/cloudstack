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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.Volume;

@APICommand(name = "updateVolume", description = "Updates the volume.", responseObject = VolumeResponse.class, responseView = ResponseView.Restricted, entityType = {Volume.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVolumeCmd extends BaseAsyncCustomIdCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVolumeCmd.class.getName());
    private static final String s_name = "updatevolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class, description="the ID of the disk volume")
    private Long id;

    @Parameter(name = ApiConstants.PATH, type = CommandType.STRING, description = "The path of the volume")
    private String path;

    @Parameter(name = ApiConstants.CHAIN_INFO,
            type = CommandType.STRING,
            description = "The chain info of the volume",
            since = "4.4")
    private String chainInfo;

    @Parameter(name = ApiConstants.STORAGE_ID,
               type = CommandType.UUID,
               entityType = StoragePoolResponse.class,
               description = "Destination storage pool UUID for the volume",
               since = "4.3")
    private Long storageId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "The state of the volume", since = "4.3")
    private String state;

    @Parameter(name = ApiConstants.DISPLAY_VOLUME,
               type = CommandType.BOOLEAN,
 description = "an optional field, whether to the display the volume to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVolume;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getPath() {
        return path;
    }

    public Long getId() {
        return id;
    }

    public Long getStorageId() {
        return storageId;
    }

    public String getState() {
        return state;
    }

    public Boolean getDisplayVolume() {
        return displayVolume;
    }

    public String getChainInfo() {
        return chainInfo;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Volume;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _responseGenerator.findVolumeById(getId());
        if (volume == null) {
            throw new InvalidParameterValueException("Invalid volume id was provided");
        }
        return volume.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_UPDATE;
    }

    @Override
    public String getEventDescription() {
        StringBuilder desc = new StringBuilder("Updating volume: ");
        desc.append(getId()).append(" with");
        if (getPath() != null) {
            desc.append(" path " + getPath());
        }
        if (getStorageId() != null) {
            desc.append(", storage id " + getStorageId());
        }

        if (getState() != null) {
            desc.append(", state " + getState());
        }
        return desc.toString();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Volume Id: " + this._uuidMgr.getUuid(Volume.class, getId()));
        Volume result = _volumeService.updateVolume(getId(), getPath(), getState(), getStorageId(), getDisplayVolume(),
                getCustomId(), getEntityOwnerId(), getChainInfo());
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(getResponseView(), result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update volume");
        }
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), Volume.class);
        }
    }

}
