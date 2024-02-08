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

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;

import com.cloud.event.EventTypes;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;

@APICommand(name = "updateGlobalLoadBalancerRule", description = "update global load balancer rules.", responseObject = GlobalLoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateGlobalLoadBalancerRuleCmd extends BaseAsyncCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = GlobalLoadBalancerResponse.class,
               required = true,
               description = "the ID of the global load balancer rule")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the load balancer rule", length = 4096)
    private String description;

    @Parameter(name = ApiConstants.GSLB_LB_METHOD,
               type = CommandType.STRING,
               required = false,
               description = "load balancer algorithm (roundrobin, leastconn, proximity) "
                   + "that is used to distributed traffic across the zones participating in global server load balancing, if not specified defaults to 'round robin'")
    private String algorithm;

    @Parameter(name = ApiConstants.GSLB_STICKY_SESSION_METHOD,
               type = CommandType.STRING,
               required = false,
               description = "session sticky method (sourceip) if not specified defaults to sourceip")
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
    public long getEntityOwnerId() {
        GlobalLoadBalancerRule lb = _entityMgr.findById(GlobalLoadBalancerRule.class, getId());
        if (lb != null) {
            return lb.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        org.apache.cloudstack.context.CallContext.current().setEventDetails("Global Load balancer Id: " + getId());
        GlobalLoadBalancerRule gslbRule = _gslbService.updateGlobalLoadBalancerRule(this);
        if (gslbRule != null) {
            GlobalLoadBalancerResponse response = _responseGenerator.createGlobalLoadBalancerResponse(gslbRule);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update global load balancer rule");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GLOBAL_LOAD_BALANCER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating global load balancer rule";
    }
}
