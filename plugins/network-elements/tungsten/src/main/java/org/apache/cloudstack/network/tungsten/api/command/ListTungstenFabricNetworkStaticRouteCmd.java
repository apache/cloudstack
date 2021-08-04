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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkStaticRouteResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListTungstenFabricNetworkStaticRouteCmd.APINAME, description = "list Tungsten-Fabric static route",
        responseObject = TungstenFabricNetworkStaticRouteResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo =
        false)
public class ListTungstenFabricNetworkStaticRouteCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTungstenFabricNetworkStaticRouteCmd.class.getName());
    public static final String APINAME = "listTungstenFabricNetworkStaticRoute";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.TUNGSTEN_NETWORK_ROUTE_TABLE_UUID, type = CommandType.STRING, description = "the UUID of Tungsten-Fabric network route table")
    private String tungstenNetworkRouteTableUuid;

    @Parameter(name = ApiConstants.ROUTE_PREFIX, type = BaseCmd.CommandType.STRING, description = "the prefix of the Tungsten-Fabric network static route")
    private String routePrefix;

    @Inject
    TungstenService tungstenService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<TungstenFabricNetworkStaticRouteResponse> networkStaticRouteResponses = tungstenService
                .listTungstenFabricNetworkStaticRoute(zoneId, tungstenNetworkRouteTableUuid, routePrefix);
        ListResponse<TungstenFabricNetworkStaticRouteResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(networkStaticRouteResponses);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
