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
package org.apache.cloudstack.api.command.admin.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.network.PhysicalNetwork;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listPhysicalNetworks", description = "Lists physical networks", responseObject = PhysicalNetworkResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListPhysicalNetworksCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPhysicalNetworksCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "list physical network by id")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID for the physical network")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "search by name")
    private String networkName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getNetworkName() {
        return networkName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Pair<List<? extends PhysicalNetwork>, Integer> result =
            _networkService.searchPhysicalNetworks(getId(), getZoneId(), this.getKeyword(), this.getStartIndex(), this.getPageSizeVal(), getNetworkName());
        if (result != null) {
            ListResponse<PhysicalNetworkResponse> response = new ListResponse<PhysicalNetworkResponse>();
            List<PhysicalNetworkResponse> networkResponses = new ArrayList<PhysicalNetworkResponse>();
            for (PhysicalNetwork network : result.first()) {
                PhysicalNetworkResponse networkResponse = _responseGenerator.createPhysicalNetworkResponse(network);
                networkResponses.add(networkResponse);
            }
            response.setResponses(networkResponses, result.second());
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to search for physical networks");
        }
    }
}
