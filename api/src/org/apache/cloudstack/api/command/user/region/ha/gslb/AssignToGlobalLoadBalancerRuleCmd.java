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

import com.cloud.dao.EntityManager;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;
import com.cloud.utils.StringUtils;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import javax.inject.Inject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@APICommand(name = "assignToGlobalLoadBalancerRule", description="Assign load balancer rule or list of load " +
        "balancer rules to a global load balancer rules.", responseObject=SuccessResponse.class)
public class AssignToGlobalLoadBalancerRuleCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AssignToGlobalLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "assigntogloballoadbalancerruleresponse";

    @Inject public EntityManager _entityMgr;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = GlobalLoadBalancerResponse.class,
            required=true, description="the ID of the global load balancer rule")
    private Long id;

    @Parameter(name=ApiConstants.LOAD_BALANCER_RULE_LIST, type=CommandType.LIST, collectionType=CommandType.UUID,
            entityType = FirewallRuleResponse.class, required=true, description="the list load balancer rules that " +
            "will be assigned to gloabal load balacner rule")
    private List<Long> loadBalancerRulesIds;

    @Parameter(name=ApiConstants.GSLB_LBRULE_WEIGHT_MAP, type= CommandType.MAP,
            description = "Map of LB rule id's and corresponding weights (between 1-100) in the GSLB rule, if not specified weight of " +
                    "a LB rule is defaulted to 1. Specified as 'gslblbruleweightsmap[0].loadbalancerid=UUID" +
                    "&gslblbruleweightsmap[0].weight=10'", required=false)
    private Map gslbLbRuleWieghtMap;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getGlobalLoadBalancerRuleId() {
        return id;
    }

    public List<Long> getLoadBalancerRulesIds() {
        return loadBalancerRulesIds;
    }

    public Map<Long, Long> getLoadBalancerRuleWeightMap() {
        Map <Long, Long> lbRuleWeightMap = new HashMap<Long, Long>();

        if (gslbLbRuleWieghtMap == null || gslbLbRuleWieghtMap.isEmpty()) {
            return null;
        }

        Collection lbruleWeightsCollection = gslbLbRuleWieghtMap.values();
        Iterator iter = lbruleWeightsCollection.iterator();
        while (iter.hasNext()) {
            HashMap<String, String> map = (HashMap<String, String>) iter.next();
            Long weight;
            LoadBalancer lbrule = _entityMgr.findByUuid(LoadBalancer.class, map.get("loadbalancerid"));
            if (lbrule == null) {
                throw new InvalidParameterValueException("Unable to find load balancer rule with ID: " + map.get("loadbalancerid"));
            }
            try {
                weight = Long.parseLong(map.get("weight"));
                if (weight < 1 || weight > 100) {
                    throw new InvalidParameterValueException("Invalid weight " + weight + " given for the LB rule id: " + map.get("loadbalancerid"));
                }
            } catch (NumberFormatException nfe) {
                throw new InvalidParameterValueException("Unable to translate weight given for the LB rule id: " + map.get("loadbalancerid"));
            }
            lbRuleWeightMap.put(lbrule.getId(), weight);
        }

        return lbRuleWeightMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Inject
    public GlobalLoadBalancingRulesService _gslbService;

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        GlobalLoadBalancerRule globalLoadBalancerRule = _entityMgr.findById(GlobalLoadBalancerRule.class,
                getGlobalLoadBalancerRuleId());
        if (globalLoadBalancerRule == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return globalLoadBalancerRule.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ASSIGN_TO_GLOBAL_LOAD_BALANCER_RULE;
    }

    @Override
    public String getEventDescription() {
        return "assign load balancer rules " + StringUtils.join(getLoadBalancerRulesIds(), ",") +
                " to global load balancer rule " + getGlobalLoadBalancerRuleId();
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Global Load balancer rule Id: "+ getGlobalLoadBalancerRuleId()+ " VmIds: "
                + StringUtils.join(getLoadBalancerRulesIds(), ","));
        boolean result = _gslbService.assignToGlobalLoadBalancerRule(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign global load balancer rule");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.gslbSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        GlobalLoadBalancerRule gslb = _gslbService.findById(id);
        if(gslb == null){
            throw new InvalidParameterValueException("Unable to find load balancer rule: " + id);
        }
        return gslb.getId();
    }
}
