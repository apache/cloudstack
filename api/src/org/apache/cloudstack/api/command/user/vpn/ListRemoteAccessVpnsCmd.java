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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;
import org.apache.log4j.Logger;

import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.Pair;

@APICommand(name = "listRemoteAccessVpns", description="Lists remote access vpns", responseObject=RemoteAccessVpnResponse.class)
public class ListRemoteAccessVpnsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger (ListRemoteAccessVpnsCmd.class.getName());

    private static final String s_name = "listremoteaccessvpnsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.PUBLIC_IP_ID, type=CommandType.UUID, entityType=IPAddressResponse.class,
            required=true, description="public ip address id of the vpn server")
    private Long publicIpId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getPublicIpId() {
        return publicIpId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        Pair<List<? extends RemoteAccessVpn>, Integer> vpns = _ravService.searchForRemoteAccessVpns(this);
        ListResponse<RemoteAccessVpnResponse> response = new ListResponse<RemoteAccessVpnResponse>();
        List<RemoteAccessVpnResponse> vpnResponses = new ArrayList<RemoteAccessVpnResponse>();
        if (vpns.first() != null && !vpns.first().isEmpty()) {
            for (RemoteAccessVpn vpn : vpns.first()) {
                vpnResponses.add(_responseGenerator.createRemoteAccessVpnResponse(vpn));
            }
        }
        response.setResponses(vpnResponses, vpns.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
