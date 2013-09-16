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
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.StaticRouteResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.StaticRoute;

@APICommand(name = "deleteStaticRoute", description="Deletes a static route", responseObject=SuccessResponse.class)
public class DeleteStaticRouteCmd extends BaseAsyncCmd{
    public static final Logger s_logger = Logger.getLogger(DeleteStaticRouteCmd.class.getName());
    private static final String s_name = "deletestaticrouteresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = StaticRouteResponse.class,
            required=true, description="the ID of the static route")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class,
            expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_STATIC_ROUTE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  ("Deleting static route id=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            StaticRoute route = _entityMgr.findById(StaticRoute.class, id);
            if (route == null) {
                throw new InvalidParameterValueException("Unable to find static route by id=" + id);
            } else {
                ownerId = route.getAccountId();
            }
        }
        return ownerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext.current().setEventDetails("Route Id: " + id);
        boolean result = _vpcService.revokeStaticRoute(id);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete static route");
        }
    }


    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        StaticRoute route =  _vpcService.getStaticRoute(id);
        if (route == null) {
            throw new InvalidParameterValueException("Invalid id is specified for the static route");
        }
        return route.getVpcId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.StaticRoute;
    }
}
