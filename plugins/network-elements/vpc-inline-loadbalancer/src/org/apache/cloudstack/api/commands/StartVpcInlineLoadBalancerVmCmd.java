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

package org.apache.cloudstack.api.commands;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.network.lb.VpcInlineLoadBalancerVmManager;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.context.CallContext;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;

@APICommand(name = "startVpcInlineLoadBalancerVM", responseObject=DomainRouterResponse.class,
        description="Starts an existing internal lb vm.", since="4.10.0", authorized = {RoleType.Admin})
public class StartVpcInlineLoadBalancerVmCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StartVpcInlineLoadBalancerVmCmd.class.getName());
    private static final String s_name = "startvpcinlinelbvmresponse";

    @Inject
    private VpcInlineLoadBalancerVmManager _vpcInlineLbVmMgr;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=DomainRouterResponse.class,
            required=true, description="the ID of the internal lb vm")
    private Long id;

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
    public long getEntityOwnerId() {
        VirtualRouter router = _entityMgr.findById(VirtualRouter.class, getId());
        if (router != null && router.getRole() == Role.LB) {
            return router.getAccountId();
        } else {
            throw new InvalidParameterValueException("Unable to find internal lb vm by id");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_INTERNAL_LB_VM_START;
    }

    @Override
    public String getEventDescription() {
        return  "starting internal lb vm: " + getId();
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        VirtualRouter router = _routerService.findRouter(getId());
        VirtualRouter result;
        if (router == null || router.getRole() != Role.LB) {
            throw new InvalidParameterValueException("Can't find vpc inline lb vm by id");
        } else {
            result = _vpcInlineLbVmMgr.startVpcInlineLbVm(getId(), CallContext.current().getCallingAccount(), CallContext.current().getCallingUserId());
        }

        if (result != null){
            DomainRouterResponse routerResponse = _responseGenerator.createDomainRouterResponse(result);
            routerResponse.setResponseName(getCommandName());
            routerResponse.setObjectName("vpcinlineloadbalancervm");
            this.setResponseObject(routerResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start internal lb vm");
        }
    }
}
