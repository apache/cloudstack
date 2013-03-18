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
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.LBStickinessResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@APICommand(name = "listLBHealthCheckPolicies", description = "Lists load balancer HealthCheck policies.", responseObject = LBHealthCheckResponse.class, since="4.2.0")
public class ListLBHealthCheckPoliciesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger
            .getLogger(ListLBHealthCheckPoliciesCmd.class.getName());

    private static final String s_name = "listlbhealthcheckpoliciesresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.LBID, type = CommandType.UUID, entityType = FirewallRuleResponse.class,
            required = true, description = "the ID of the load balancer rule")
    private Long lbRuleId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Long getLbRuleId() {
        return lbRuleId;
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
        List<LBHealthCheckResponse> hcpResponses = new ArrayList<LBHealthCheckResponse>();
        LoadBalancer lb = _lbService.findById(getLbRuleId());
        ListResponse<LBHealthCheckResponse> response = new ListResponse<LBHealthCheckResponse>();

        if (lb != null) {
            List<? extends HealthCheckPolicy> healthCheckPolicies = _lbService.searchForLBHealthCheckPolicies(this);
            LBHealthCheckResponse spResponse = _responseGenerator.createLBHealthCheckPolicyResponse(healthCheckPolicies, lb);
            hcpResponses.add(spResponse);
            response.setResponses(hcpResponses);
        }

        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
