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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.ha.SimulatorHAProvider;
import org.apache.cloudstack.ha.SimulatorHAState;

import javax.inject.Inject;

@APICommand(name = ConfigureSimulatorHAProviderState.APINAME,
        description="configures simulator HA provider state for a host for probing and testing",
        responseObject=SuccessResponse.class,
        since = "4.11", authorized = {RoleType.Admin})
public final class ConfigureSimulatorHAProviderState extends BaseCmd {
    public static final String APINAME = "configureSimulatorHAProviderState";

    @Inject
    private HAManager haManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = BaseCmd.CommandType.UUID, entityType = HostResponse.class,
            description = "List by host ID", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long hostId;

    @Parameter(name = ApiConstants.HEALTH, type = CommandType.BOOLEAN,
            description = "Set true is haprovider for simulator host should be healthy",
            required = true)
    private Boolean healthy;

    @Parameter(name = ApiConstants.ACTIVITY, type = CommandType.BOOLEAN,
            description = "Set true is haprovider for simulator host should have activity",
            required = true)
    private Boolean activity;

    @Parameter(name = ApiConstants.RECOVER, type = CommandType.BOOLEAN,
            description = "Set true is haprovider for simulator host should be recoverable",
            required = true)
    private Boolean recovery;

    @Parameter(name = ApiConstants.FENCE, type = CommandType.BOOLEAN,
            description = "Set true is haprovider for simulator host should be fence-able",
            required = true)
    private Boolean fenceable;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final Host host = _resourceService.getHost(getHostId());
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find host by ID: " + getHostId());
        }
        final SimulatorHAState haState = new SimulatorHAState(healthy, activity, recovery, fenceable);
        final SimulatorHAProvider simulatorHAProvider = (SimulatorHAProvider) haManager.getHAProvider(SimulatorHAProvider.class.getSimpleName().toLowerCase());
        if (simulatorHAProvider != null) {
            simulatorHAProvider.setHAStateForHost(host.getId(), haState);
        }
        final SuccessResponse response = new SuccessResponse();
        response.setSuccess(simulatorHAProvider != null);
        response.setResponseName(getCommandName());
        response.setObjectName("simulatorhaprovider");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
