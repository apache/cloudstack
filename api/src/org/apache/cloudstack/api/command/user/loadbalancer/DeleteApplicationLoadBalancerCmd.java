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
package org.apache.cloudstack.api.command.user.loadbalancer;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "deleteLoadBalancer", description="Deletes a load balancer", responseObject=SuccessResponse.class, since="4.2.0")
public class DeleteApplicationLoadBalancerCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteApplicationLoadBalancerCmd.class.getName());
    private static final String s_name = "deleteloadbalancerresponse";
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = FirewallRuleResponse.class,
            required=true, description="the ID of the Load Balancer")
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
        ApplicationLoadBalancerRule lb = _entityMgr.findById(ApplicationLoadBalancerRule.class, getId());
        if (lb != null) {
            return lb.getAccountId();
        } else {
            throw new InvalidParameterValueException("Can't find load balancer by id specified");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting load balancer: " + getId();
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Load balancer Id: " + getId());
        boolean result = _appLbService.deleteApplicationLoadBalancer(getId());

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete load balancer");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        ApplicationLoadBalancerRule lb = _appLbService.getApplicationLoadBalancer(id);
        if(lb == null){
            throw new InvalidParameterValueException("Unable to find load balancer by id ");
        }
        return lb.getNetworkId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.FirewallRule;
    }
}
