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
package org.apache.cloudstack.api.command.user.vpn;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.RemoteAccessVpn;

@APICommand(name = "updateRemoteAccessVpn", description = "Updates remote access vpn", responseObject = RemoteAccessVpnResponse.class, since = "4.4",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateRemoteAccessVpnCmd extends BaseAsyncCustomIdCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = RemoteAccessVpnResponse.class, description = "id of the remote access vpn")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, expose = false)
    private Long ownerId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vpn to the end user or not", since = "4.4", authorized = {RoleType.Admin})
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

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            RemoteAccessVpn vpnEntity = _ravService.getRemoteAccessVpnById(id);
            if (vpnEntity != null)
                return vpnEntity.getAccountId();

            throw new InvalidParameterValueException("The specified id is invalid");
        }
        return ownerId;
    }

    @Override
    public String getEventDescription() {
        return "Updating remote access vpn id=" + id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_REMOTE_ACCESS_VPN_UPDATE;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        RemoteAccessVpn result = _ravService.updateRemoteAccessVpn(id, this.getCustomId(), getDisplay());
        RemoteAccessVpnResponse response = _responseGenerator.createRemoteAccessVpnResponse(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), RemoteAccessVpn.class);
        }
    }
}
