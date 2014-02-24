//
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
//

package org.apache.cloudstack.network.opendaylight.api.commands;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.opendaylight.agent.OpenDaylightControllerResourceManager;
import org.apache.cloudstack.network.opendaylight.api.responses.OpenDaylightControllerResponse;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerVO;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "listOpenDaylightControllers", responseObject = OpenDaylightControllerResponse.class, description = "Lists OpenDyalight controllers",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListOpenDaylightControllersCmd extends BaseCmd {
    @Inject
    private OpenDaylightControllerResourceManager resourceManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = false,
            description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OpenDaylightControllerResponse.class, required = false,
            description = "the ID of a OpenDaylight Controller")
    private Long Id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return "listOpenDaylightControllers";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Long getId() {
        return Id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
    NetworkRuleConflictException {
        List<OpenDaylightControllerVO> controllers = resourceManager.listControllers(this);

        List<OpenDaylightControllerResponse> controllerList = new ArrayList<OpenDaylightControllerResponse>();
        for (OpenDaylightControllerVO controller: controllers) {
            OpenDaylightControllerResponse responseObject = resourceManager.createResponseFromVO(controller);
            controllerList.add(responseObject);
        }
        ListResponse<OpenDaylightControllerResponse> responseList = new ListResponse<OpenDaylightControllerResponse>();
        responseList.setResponseName(getCommandName());
        responseList.setResponses(controllerList);
        setResponseObject(responseList);
    }

}
