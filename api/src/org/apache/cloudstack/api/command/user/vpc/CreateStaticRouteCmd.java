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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.StaticRouteResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.VpcGateway;

@APICommand(name = "createStaticRoute", description="Creates a static route", responseObject=StaticRouteResponse.class)
public class CreateStaticRouteCmd extends BaseAsyncCreateCmd{
    private static final String s_name = "createstaticrouteresponse";
    public static final Logger s_logger = Logger.getLogger(CreateStaticRouteCmd.class.getName());

    @Parameter(name=ApiConstants.GATEWAY_ID, type=CommandType.UUID, entityType=PrivateGatewayResponse.class,
            required=true, description="the gateway id we are creating static route for")
    private Long gatewayId;

    @Parameter(name = ApiConstants.CIDR, required = true, type = CommandType.STRING, description = "static route cidr")
    private String cidr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public long getGatewayId() {
        return gatewayId;
    }

    public String getCidr() {
        return cidr;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void create() throws ResourceAllocationException {
        try {
            StaticRoute result = _vpcService.createStaticRoute(getGatewayId(), getCidr());
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network rule conflict: ", ex);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }


    @Override
    public String getEventType() {
        return EventTypes.EVENT_STATIC_ROUTE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating static route";
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        boolean success = false;
        StaticRoute route = _entityMgr.findById(StaticRoute.class, getEntityId());
        try {
            CallContext.current().setEventDetails("Static route Id: " + getEntityId());
            success = _vpcService.applyStaticRoutes(route.getVpcId());

            // State is different after the route is applied, so get new object here
            route = _entityMgr.findById(StaticRoute.class, getEntityId());
            StaticRouteResponse routeResponse = new StaticRouteResponse();
            if (route != null) {
                routeResponse = _responseGenerator.createStaticRouteResponse(route);
                setResponseObject(routeResponse);
            }
            routeResponse.setResponseName(getCommandName());
        } finally {
            if (!success || route == null) {
                _vpcService.revokeStaticRoute(getEntityId());
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create static route");
            }
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
         VpcGateway gateway =  _vpcService.getVpcGateway(gatewayId);
         if (gateway == null) {
             throw new InvalidParameterValueException("Invalid gateway id is specified");
         }
         return _vpcService.getVpc(gateway.getVpcId()).getAccountId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        VpcGateway gateway =  _vpcService.getVpcGateway(gatewayId);
        if (gateway == null) {
            throw new InvalidParameterValueException("Invalid id is specified for the gateway");
        }
        return gateway.getVpcId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.StaticRoute;
    }
}
