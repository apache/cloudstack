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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import com.cloud.network.rules.ApplicationLoadBalancerRule;
import com.cloud.utils.Pair;

@APICommand(name = "listLoadBalancers", description = "Lists Load Balancers", responseObject = ApplicationLoadBalancerResponse.class)
public class ListApplicationLoadBalancersCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListLoadBalancerRulesCmd.class.getName());

    private static final String s_name = "listloadbalancerssresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class,
            description = "the ID of the Load Balancer")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the Load Balancer")
    private String loadBalancerName;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerName;
    }


    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends ApplicationLoadBalancerRule>, Integer> loadBalancers = _appLbService.listApplicationLoadBalancers();
        ListResponse<ApplicationLoadBalancerResponse> response = new ListResponse<ApplicationLoadBalancerResponse>();
        List<ApplicationLoadBalancerResponse> lbResponses = new ArrayList<ApplicationLoadBalancerResponse>();
        for (ApplicationLoadBalancerRule loadBalancer : loadBalancers.first()) {
            ApplicationLoadBalancerResponse lbResponse = _responseGenerator.createLoadBalancerContainerReponse(loadBalancer, _lbService.getLbInstances(loadBalancer.getId()));
            lbResponse.setObjectName("loadbalancer");
            lbResponses.add(lbResponse);
        }
        response.setResponses(lbResponses, loadBalancers.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
