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

package org.apache.cloudstack.api.command.user.region.ha.gslb;

import com.cloud.event.EventTypes;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "deleteGlobalLoadBalancerRule", description="Deletes a global load balancer rule.", responseObject=SuccessResponse.class)
public class DeleteGlobalLoadBalancerRuleCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(DeleteGlobalLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "deletegloballoadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = GlobalLoadBalancerResponse.class, required=true, description="the ID of the global load balancer rule")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getGlobalLoadBalancerId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Inject
    public GlobalLoadBalancingRulesService _gslbService;

    @Override
    public long getEntityOwnerId() {
        GlobalLoadBalancerRule lb = _entityMgr.findById(GlobalLoadBalancerRule.class, getGlobalLoadBalancerId());
        if (lb != null) {
            return lb.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GLOBAL_LOAD_BALANCER_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting global load balancer rule: " + getGlobalLoadBalancerId();
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Deleting global Load balancer rule Id: " + getGlobalLoadBalancerId());
        boolean result = _gslbService.deleteGlobalLoadBalancerRule(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete Global Load Balancer rule.");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.gslbSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return null;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.GlobalLoadBalancerRule;
    }
}
