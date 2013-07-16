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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.PhysicalNetwork;
import com.cloud.user.Account;

@APICommand(name = "createPhysicalNetwork", description="Creates a physical network", responseObject=PhysicalNetworkResponse.class, since="3.0.0")
public class CreatePhysicalNetworkCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePhysicalNetworkCmd.class.getName());

    private static final String s_name = "createphysicalnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType= ZoneResponse.class,
            required=true, description="the Zone ID for the physical network")
    private Long zoneId;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the VLAN for the physical network")
    private String vlan;

    @Parameter(name=ApiConstants.NETWORK_SPEED, type=CommandType.STRING, description="the speed for the physical network[1G/10G]")
    private String speed;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="domain ID of the account owning a physical network")
    private Long domainId;

    @Parameter(name=ApiConstants.BROADCAST_DOMAIN_RANGE, type=CommandType.STRING, description="the broadcast domain range for the physical network[Pod or Zone]. In Acton release it can be Zone only in Advance zone, and Pod in Basic")
    private String broadcastDomainRange;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.LIST, collectionType=CommandType.STRING, description="Tag the physical network")
    private List<String> tags;

    @Parameter(name=ApiConstants.ISOLATION_METHODS, type=CommandType.LIST, collectionType=CommandType.STRING, description="the isolation method for the physical network[VLAN/L3/GRE]")
    private List<String> isolationMethods;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the physical network")
    private String networkName;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getTags() {
        return tags;
    }


    public Long getZoneId() {
        return zoneId;
    }

    public String getVlan() {
        return vlan;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getBroadcastDomainRange() {
        return broadcastDomainRange;
    }

    public List<String> getIsolationMethods() {
        return isolationMethods;
    }

    public String getNetworkSpeed() {
        return speed;
    }

    public String getNetworkName() {
        return networkName;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PHYSICAL_NETWORK_CREATE;
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_PHYSICAL_NETWORK_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating Physical Network";
    }

    @Override
    public String getEventDescription() {
        return  "creating Physical Network. Id: "+getEntityId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Physical Network Id: "+getEntityId());
        PhysicalNetwork result = _networkService.getCreatedPhysicalNetwork(getEntityId());
        if (result != null) {
            PhysicalNetworkResponse response = _responseGenerator.createPhysicalNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        PhysicalNetwork result = _networkService.createPhysicalNetwork(getZoneId(),getVlan(),getNetworkSpeed(), getIsolationMethods(),getBroadcastDomainRange(),getDomainId(), getTags(), getNetworkName());
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create physical network entity");
        }
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.PhysicalNetwork;
    }
}
