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

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetwork;
import com.cloud.user.Account;

@APICommand(name = "updatePhysicalNetwork", description = "Updates a physical network", responseObject = PhysicalNetworkResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdatePhysicalNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePhysicalNetworkCmd.class.getName());

    private static final String s_name = "updatephysicalnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "physical network id")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_SPEED, type = CommandType.STRING, description = "the speed for the physical network[1G/10G]")
    private String speed;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "Tag the physical network")
    private List<String> tags;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "Enabled/Disabled")
    private String state;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, description = "the VLAN for the physical network")
    private String vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getTags() {
        return tags;
    }

    public String getNetworkSpeed() {
        return speed;
    }

    public String getState() {
        return state;
    }

    public Long getId() {
        return id;
    }

    public String getVlan() {
        return vlan;
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
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        PhysicalNetwork result = _networkService.updatePhysicalNetwork(getId(), getNetworkSpeed(), getTags(), getVlan(), getState());
        if (result != null) {
            PhysicalNetworkResponse response = _responseGenerator.createPhysicalNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }
    }

    @Override
    public String getEventDescription() {
        return "Updating Physical network: " + getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PHYSICAL_NETWORK_UPDATE;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.PhysicalNetwork;
    }
}
