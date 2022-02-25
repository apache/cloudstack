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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

@APICommand(name = "destroyVolume", description = "Destroys a Volume.", responseObject = VolumeResponse.class, responseView = ResponseView.Restricted, entityType = {Volume.class},
            since = "4.14.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class DestroyVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DestroyVolumeCmd.class.getName());

    private static final String s_name = "destroyvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class,
            required=true, description="The ID of the volume")
    private Long id;

    @Parameter(name = ApiConstants.EXPUNGE,
               type = CommandType.BOOLEAN,
               description = "If true is passed, the volume is expunged immediately. False by default.",
               since = "4.6.0")
    private Boolean expunge;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean getExpunge() {
        if (expunge == null) {
            return false;
        }
        return expunge;
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
        Volume volume = _entityMgr.findById(Volume.class, getId());
        if (volume != null) {
            return volume.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_DESTROY;
    }

    @Override
    public String getEventDescription() {
        return  "destroying volume: " + getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Volume;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Volume Id: " + getId());
        Volume result = _volumeService.destroyVolume(getId(), CallContext.current().getCallingAccount(), getExpunge(), false);
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseView.Restricted, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to destroy volume");
        }
    }
}
