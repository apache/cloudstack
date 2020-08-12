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
package org.apache.cloudstack.api.command.user.vpc;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.StaticRouteResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.vpc.StaticRoute;
import com.cloud.utils.Pair;

@APICommand(name = "listStaticRoutes", description = "Lists all static routes", responseObject = StaticRouteResponse.class, entityType = {StaticRoute.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListStaticRoutesCmd extends BaseListTaggedResourcesCmd {
    private static final String s_name = "liststaticroutesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = StaticRouteResponse.class, description = "list static route by id")
    private Long id;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "list static routes by vpc id")
    private Long vpcId;

    @Parameter(name = ApiConstants.GATEWAY_ID, type = CommandType.UUID, entityType = PrivateGatewayResponse.class, description = "list static routes by gateway id")
    private Long gatewayId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list static routes by state")
    private String state;

    public Long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getGatewayId() {
        return gatewayId;
    }

    public String getState() {
        return state;
    }

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends StaticRoute>, Integer> result = _vpcService.listStaticRoutes(this);
        ListResponse<StaticRouteResponse> response = new ListResponse<StaticRouteResponse>();
        List<StaticRouteResponse> routeResponses = new ArrayList<StaticRouteResponse>();

        for (StaticRoute route : result.first()) {
            StaticRouteResponse ruleData = _responseGenerator.createStaticRouteResponse(route);
            routeResponses.add(ruleData);
        }
        response.setResponses(routeResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
