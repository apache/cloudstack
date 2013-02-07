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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.region.ha.GlobalLoadBalancer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import java.util.List;

@APICommand(name = "assignToGlobalLoadBalancerRule", description="Assign load balancer rule or list of load " +
        "balancer rules to a global load balancer rules.", responseObject=SuccessResponse.class)
public class AssignToGlobalLoadBalancerRuleCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AssignToGlobalLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "assigntogloballoadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = GlobalLoadBalancerResponse.class,
            required=true, description="the ID of the load balancer rule")
    private Long id;

    @Parameter(name=ApiConstants.LOAD_BALANCER_RULE_LIST, type=CommandType.LIST, collectionType=CommandType.UUID,
            entityType = LoadBalancerResponse.class, required=true, description="the list load balancer rules that " +
            "will be assigned to gloabal load balacner rule")
    private List<Long> loadBalancerRulesIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getGlobalLoadBalancerId() {
        return id;
    }

    public List<Long> getLoadBalancerRulesId() {
        return loadBalancerRulesIds;
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
        GlobalLoadBalancer globalLoadBalancer = _entityMgr.findById(GlobalLoadBalancer.class, getGlobalLoadBalancerId());
        if (globalLoadBalancer == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return globalLoadBalancer.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ASSIGN_TO_GLOBAL_LOAD_BALANCER_RULE;
    }

    @Override
    public String getEventDescription() {
        return "applying load balancer rules " + StringUtils.join(getLoadBalancerRulesId(), ",") +
                " to global load balancer rule " + getGlobalLoadBalancerId();
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Global Load balancer rule Id: "+ getGlobalLoadBalancerId()+ " VmIds: "
                + StringUtils.join(getLoadBalancerRulesId(), ","));
        boolean result = _lbService.assignToLoadBalancer(getGlobalLoadBalancerId(), loadBalancerRulesIds);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign global load balancer rule");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        LoadBalancer lb = _lbService.findById(id);
        if(lb == null){
            throw new InvalidParameterValueException("Unable to find load balancer rule: " + id);
        }
        return lb.getNetworkId();
    }
}
