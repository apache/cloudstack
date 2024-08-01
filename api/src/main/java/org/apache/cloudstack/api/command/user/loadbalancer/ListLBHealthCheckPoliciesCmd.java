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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.ListResponse;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;

@APICommand(name = "listLBHealthCheckPolicies", description = "Lists load balancer health check policies.", responseObject = LBHealthCheckResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListLBHealthCheckPoliciesCmd extends BaseListCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.LBID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               description = "the ID of the load balancer rule")
    private Long lbRuleId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = LBHealthCheckResponse.class, description = "the ID of the health check policy", since = "4.4")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Long getLbRuleId() {
        return lbRuleId;
    }

    public Long getId() {
        return id;
    }

    public boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return true;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<LBHealthCheckResponse> hcpResponses = new ArrayList<LBHealthCheckResponse>();
        ListResponse<LBHealthCheckResponse> response = new ListResponse<LBHealthCheckResponse>();
        Long lbRuleId = getLbRuleId();
        Long hId = getId();
        if(lbRuleId == null) {
            if(hId != null) {
                lbRuleId = _lbService.findLBIdByHealtCheckPolicyId(hId);
            } else {
                throw new InvalidParameterValueException("Either load balancer rule ID or health check policy ID should be specified");
            }
        }

        LoadBalancer lb = _lbService.findById(lbRuleId);
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
