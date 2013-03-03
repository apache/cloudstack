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

import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "updateGlobalLoadBalancerRule", description = "update global load balancer rules.", responseObject = LoadBalancerResponse.class)
public class UpdateGlobalLoadBalancerRuleCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(GlobalLoadBalancerResponse.class.getName());

    private static final String s_name = "updategloballoadbalancerruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = GlobalLoadBalancerResponse.class,
            required=true, description="the ID of the global load balancer rule")
    private Long id;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule", length=4096)
    private String description;

    @Parameter(name=ApiConstants.GSLB_LB_METHOD, type=CommandType.STRING, required=false, description="load balancer algorithm (roundrobin, leastconn, proximity) " +
            "that is used to distributed traffic across the zones participating in global server load balancing, if not specified defaults to 'round robin'")
    private String algorithm;

    @Parameter(name=ApiConstants.GSLB_STICKY_SESSION_METHOD, type=CommandType.STRING, required=false, description="session sticky method (sourceip) if not specified defaults to sourceip")
    private String stickyMethod;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getGslbMethod() {
        return algorithm;
    }

    public String getStickyMethod() {
        return stickyMethod;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Inject
    public GlobalLoadBalancingRulesService _gslbService;

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        _gslbService.updateGlobalLoadBalancerRule(this);
    }

}
