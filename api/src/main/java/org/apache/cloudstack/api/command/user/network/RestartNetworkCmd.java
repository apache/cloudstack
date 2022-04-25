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
package org.apache.cloudstack.api.command.user.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;

@APICommand(name = "restartNetwork",
            description = "Restarts the network; includes 1) restarting network elements - virtual routers, DHCP servers 2) reapplying all public IPs 3) reapplying loadBalancing/portForwarding rules",
        responseObject = SuccessResponse.class, entityType = {Network.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class RestartNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RestartNetworkCmd.class.getName());
    private static final String s_name = "restartnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "The ID of the network to restart.")
    private Long id;

    @Parameter(name = ApiConstants.CLEANUP, type = CommandType.BOOLEAN, required = false, description = "If cleanup old network elements")
    private Boolean cleanup = false;

    @Parameter(name = ApiConstants.MAKEREDUNDANT, type = CommandType.BOOLEAN, required = false, description = "Turn the network into a network with redundant routers.", since = "4.11.1")
    private Boolean makeRedundant = false;

    @Parameter(name = ApiConstants.LIVE_PATCH, type = CommandType.BOOLEAN, required = false,
            description = "Live patches the router software before restarting it. This parameter will only work when 'cleanup' is false.",
            since = "4.17.0")
    private Boolean livePatch = false;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNetworkId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find network by ID " + id);
        } else {
            return network.getId();
        }
    }

    public Boolean getCleanup() {
        return cleanup;
    }

    public Boolean getMakeRedundant() {
        return makeRedundant;
    }

    public Boolean getLivePatch() { return livePatch; }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "addressinfo";
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        boolean result = _networkService.restartNetwork(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to restart network");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public String getEventDescription() {
        return "Restarting network: " + getNetworkId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_RESTART;
    }

    @Override
    public Long getInstanceId() {
        return getNetworkId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Network;
    }

    @Override
    public long getEntityOwnerId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Networkd ID=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }
}
