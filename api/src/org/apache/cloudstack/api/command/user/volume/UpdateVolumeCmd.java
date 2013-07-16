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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

@APICommand(name = "updateVolume", description="Updates the volume.", responseObject=VolumeResponse.class)
public class UpdateVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVolumeCmd.class.getName());
    private static final String s_name = "addVolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class,
            required=true, description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.PATH, type=CommandType.STRING,
            required=true, description="the path of the volume")
    private String path;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getPath() {
        return path;
    }

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

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Volume;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _responseGenerator.findVolumeById(getId());
        if (volume == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return volume.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_ATTACH;
    }

    @Override
    public String getEventDescription() {
        return  "adding detail to the volume: " + getId();
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Volume Id: "+getId());
        Volume result = _volumeService.updateVolume(this);
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update volume");
        }
    }
}
