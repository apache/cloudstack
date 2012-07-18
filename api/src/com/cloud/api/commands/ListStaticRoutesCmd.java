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

import java.util.ArrayList;
import java.util.List;
import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.StaticRouteResponse;
import com.cloud.network.vpc.StaticRoute;

@Implementation(description="Lists all static routes", responseObject=StaticRouteResponse.class)
public class ListStaticRoutesCmd extends BaseListProjectAndAccountResourcesCmd {
    private static final String s_name = "liststaticroutesresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="static_routes")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list static route by id")
    private Long id;
    
    @IdentityMapper(entityTableName="vpc")
    @Parameter(name=ApiConstants.VPC_ID, type=CommandType.LONG, description="list static routes by vpc id")
    private Long vpcId;
    
    @IdentityMapper(entityTableName="vpc_gateways")
    @Parameter(name=ApiConstants.GATEWAY_ID, type=CommandType.LONG, description="list static routes by gateway id")
    private Long gatewayId;

    public Long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getGatewayId() {
        return gatewayId;
    }
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends StaticRoute> result = _vpcService.listStaticRoutes(this);
        ListResponse<StaticRouteResponse> response = new ListResponse<StaticRouteResponse>();
        List<StaticRouteResponse> routeResponses = new ArrayList<StaticRouteResponse>();
        
        for (StaticRoute route : result) {
            StaticRouteResponse ruleData = _responseGenerator.createStaticRouteResponse(route);
            routeResponses.add(ruleData);
        }
        response.setResponses(routeResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response); 
    }


}
