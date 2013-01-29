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
package com.cloud.api.commands;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.response.NetworkResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.user.UserContext;

//@APICommand(description="Creates a private network", responseObject=NetworkResponse.class)
public class CreatePrivateNetworkCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePrivateNetworkCmd.class.getName());

    private static final String s_name = "createnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the network")
    private String name;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the network")
    private String displayText;

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required=true, description="the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, required=true, description="the gateway of the network")
    private String gateway;

    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, required=true, description="the netmask of the network")
    private String netmask;

    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, required=true, description="the beginning IP address in the network IP range")
    private String startIp;

    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address in the network IP" +
            " range. If not specified, will be defaulted to startIP")
    private String endIp;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, required=true, description="the ID or VID of the network")
    private String vlan;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will own the network")
    private String accountName;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            description="an optional project for the ssh key")
    private Long projectId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="domain ID of the account owning a network")
    private Long domainId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getGateway() {
        return gateway;
    }

    public String getVlan() {
        return vlan;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getNetworkName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getProjectId() {
        return projectId;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getEndIp() {
        return endIp;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }


    @Override
    public void create() throws ResourceAllocationException {
        Network result = null;
        try {
            result = _networkService.createPrivateNetwork(getNetworkName(), getDisplayText(), getPhysicalNetworkId(), getVlan(),
                    getStartIp(), getEndIp(), getGateway(), getNetmask(), getEntityOwnerId(), null);
        } catch (InsufficientCapacityException ex){
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }

        if (result != null) {
            this.setEntityId(result.getId());
            this.setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a Private network");
        }
    }

    @Override
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException{
        Network result = _networkService.getNetwork(getEntityId());
        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create private network");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating private network";

    }

}
