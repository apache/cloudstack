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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;

import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.Pair;

@APICommand(name = "listRemoteAccessVpns", description = "Lists remote access vpns", responseObject = RemoteAccessVpnResponse.class, entityType = {RemoteAccessVpn.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListRemoteAccessVpnsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListRemoteAccessVpnsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PUBLIC_IP_ID, type = CommandType.UUID, entityType = IPAddressResponse.class, description = "public ip address id of the vpn server")
    private Long publicIpId;

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = RemoteAccessVpnResponse.class,
               description = "Lists remote access vpn rule with the specified ID",
               since = "4.3")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "list remote access VPNs for certain network",
               since = "4.3")
    private Long networkId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPublicIpId() {
        return publicIpId;
    }

    public Long getId() {
        return id;
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
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
        setResponseObject(response);
    }
}
