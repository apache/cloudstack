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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VpnUsersResponse;

import com.cloud.network.VpnUser;
import com.cloud.utils.Pair;

@APICommand(name = "listVpnUsers", description = "Lists vpn users", responseObject = VpnUsersResponse.class, entityType = {VpnUser.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVpnUsersCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVpnUsersCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VpnUsersResponse.class, description = "The uuid of the Vpn user")
    private Long id;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "the username of the vpn user.")
    private String userName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return userName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends VpnUser>, Integer> vpnUsers = _ravService.searchForVpnUsers(this);

        ListResponse<VpnUsersResponse> response = new ListResponse<VpnUsersResponse>();
        List<VpnUsersResponse> vpnResponses = new ArrayList<VpnUsersResponse>();
        for (VpnUser vpnUser : vpnUsers.first()) {
            vpnResponses.add(_responseGenerator.createVpnUserResponse(vpnUser));
        }

        response.setResponses(vpnResponses, vpnUsers.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
