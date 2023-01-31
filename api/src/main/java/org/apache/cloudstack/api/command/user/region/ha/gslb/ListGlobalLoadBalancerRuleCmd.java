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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RegionResponse;

import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;

@APICommand(name = "listGlobalLoadBalancerRules", description = "Lists load balancer rules.", responseObject = GlobalLoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListGlobalLoadBalancerRuleCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListGlobalLoadBalancerRuleCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GlobalLoadBalancerResponse.class, description = "the ID of the global load balancer rule")
    private Long id;

    @Parameter(name = ApiConstants.REGION_ID, type = CommandType.INTEGER, entityType = RegionResponse.class, description = "region ID")
    private Integer regionId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getRegionId() {
        return regionId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Inject
    public GlobalLoadBalancingRulesService _gslbService;

    @Override
    public void execute() {
        List<GlobalLoadBalancerRule> globalLoadBalancers = _gslbService.listGlobalLoadBalancerRule(this);
        ListResponse<GlobalLoadBalancerResponse> gslbRuleResponse = new ListResponse<GlobalLoadBalancerResponse>();
        List<GlobalLoadBalancerResponse> gslbResponses = new ArrayList<GlobalLoadBalancerResponse>();
        if (globalLoadBalancers != null) {
            for (GlobalLoadBalancerRule gslbRule : globalLoadBalancers) {
                GlobalLoadBalancerResponse gslbResponse = _responseGenerator.createGlobalLoadBalancerResponse(gslbRule);
                gslbResponse.setObjectName("globalloadbalancerrule");
                gslbResponses.add(gslbResponse);
            }
        }
        gslbRuleResponse.setResponses(gslbResponses);
        gslbRuleResponse.setResponseName(getCommandName());
        this.setResponseObject(gslbRuleResponse);
    }

}
