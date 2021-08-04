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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricInterfaceStaticRouteResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = AddTungstenFabricInterfaceStaticRouteCmd.APINAME, description = "add Tungsten-Fabric interface static route",
        responseObject = TungstenFabricInterfaceStaticRouteResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class AddTungstenFabricInterfaceStaticRouteCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddTungstenFabricInterfaceStaticRouteCmd.class.getName());
    public static final String APINAME = "addTungstenFabricInterfaceStaticRoute";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.TUNGSTEN_INTERFACE_ROUTE_TABLE_UUID, type = CommandType.STRING, required = true, description = "the uuid of the Tungsten-Fabric interface route table")
    private String tungstenInterfaceRouteTableUuid;

    @Parameter(name = ApiConstants.INTERFACE_ROUTE_PREFIX, type = CommandType.STRING, required = true, description = "the prefix of the Tungsten-Fabric interface static route")
    private String routePrefix;

    @Parameter(name = ApiConstants.INTERFACE_COMMUNITIES, type = CommandType.STRING, description = "the communities of the Tungsten-Fabric interface static route")
    private String communities;

    @Inject
    TungstenService tungstenService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricInterfaceStaticRouteResponse tungstenFabricInterfaceStaticRouteResponse =
                tungstenService.addTungstenFabricInterfaceStaticRoute(zoneId, tungstenInterfaceRouteTableUuid,
                        routePrefix, communities);
        if(tungstenFabricInterfaceStaticRouteResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add a Tungsten-Fabric interface static route");
        } else {
            tungstenFabricInterfaceStaticRouteResponse.setResponseName(getCommandName());
            setResponseObject(tungstenFabricInterfaceStaticRouteResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_ADD_STATIC_ROUTE;
    }

    @Override
    public String getEventDescription() {
        return "add Tungsten-Fabric interface static route";
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
