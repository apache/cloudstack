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

import com.cloud.api.response.SimulatorHAStateResponse;
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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.ha.SimulatorHAProvider;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListSimulatorHAStateTransitions.APINAME,
        description="list recent simulator HA state transitions for a host for probing and testing",
        responseObject=SimulatorHAStateResponse.class,
        since = "4.11", authorized = {RoleType.Admin})
public final class ListSimulatorHAStateTransitions extends BaseListCmd {
    public static final String APINAME = "listSimulatorHAStateTransitions";

    @Inject
    private HAManager haManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = BaseCmd.CommandType.UUID, entityType = HostResponse.class,
            description = "List by host ID", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long hostId;

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

        final SimulatorHAProvider simulatorHAProvider = (SimulatorHAProvider) haManager.getHAProvider(SimulatorHAProvider.class.getSimpleName().toLowerCase());
        List<SimulatorHAStateResponse> recentStates = new ArrayList<>();
        if (simulatorHAProvider != null) {
            recentStates = simulatorHAProvider.listHAStateTransitions(host.getId());
        }
        final ListResponse<SimulatorHAStateResponse> response = new ListResponse<>();
        response.setResponses(recentStates);
        response.setResponseName(getCommandName());
        response.setObjectName("simulatorhastatetransition");
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
