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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;

@APICommand(name = "deleteRemoteAccessVpn", description="Destroys a l2tp/ipsec remote access vpn", responseObject=SuccessResponse.class)
public class DeleteRemoteAccessVpnCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteRemoteAccessVpnCmd.class.getName());

    private static final String s_name = "deleteremoteaccessvpnresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.PUBLIC_IP_ID, type=CommandType.UUID, entityType=IPAddressResponse.class,
            required=true, description="public ip address id of the vpn server")
    private Long publicIpId;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType=AccountResponse.class, expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            RemoteAccessVpn vpnEntity = _ravService.getRemoteAccessVpn(publicIpId);
            if(vpnEntity != null)
                return vpnEntity.getAccountId();

            throw new InvalidParameterValueException("The specified public ip is not allocated to any account");
        }
        return ownerId;
    }

    @Override
    public String getEventDescription() {
        return "Delete Remote Access VPN for account " + getEntityOwnerId() + " for  ip id=" + publicIpId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        _ravService.destroyRemoteAccessVpnForIp(publicIpId, CallContext.current().getCallingAccount());
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return _ravService.getRemoteAccessVpn(publicIpId).getNetworkId();
    }

}
