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

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.LBStickinessResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.user.Account;

@APICommand(name = "listLBStickinessPolicies", description = "Lists load balancer stickiness policies.", responseObject = LBStickinessResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListLBStickinessPoliciesCmd extends BaseListCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.LBID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               description = "the ID of the load balancer rule")
    private Long lbRuleId;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = LBStickinessResponse.class,
            description = "the ID of the load balancer stickiness policy")
    private Long id;


    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

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

        LoadBalancer lb = null;
        if (lbRuleId == null && id == null) {
            throw new InvalidParameterValueException("load balancer rule ID and stickiness policy ID can't be null");
        }

        if (id != null) {
            lb = _lbService.findLbByStickinessId(id);
            if (lb == null) {
                throw new InvalidParameterValueException("stickiness policy ID doesn't exist");
            }

            if ((lbRuleId != null) && (lbRuleId != lb.getId())) {
                throw new InvalidParameterValueException("stickiness policy ID doesn't belong to lbId" + lbRuleId);
            }
        }

        if (lbRuleId != null && lb == null) {
            lb = _lbService.findById(getLbRuleId());
        }

        List<LBStickinessResponse> spResponses = new ArrayList<LBStickinessResponse>();
        ListResponse<LBStickinessResponse> response = new ListResponse<LBStickinessResponse>();

        if (lb != null) {
            //check permissions
            Account caller = CallContext.current().getCallingAccount();
            _accountService.checkAccess(caller, null, true, lb);
            List<? extends StickinessPolicy> stickinessPolicies = _lbService.searchForLBStickinessPolicies(this);
            LBStickinessResponse spResponse = _responseGenerator.createLBStickinessPolicyResponse(stickinessPolicies, lb);
            spResponses.add(spResponse);
            response.setResponses(spResponses);
        }

        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
