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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

@APICommand(name = "removeCertFromLoadBalancer", description = "Removes a certificate from a load balancer rule", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RemoveCertFromLoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(RemoveCertFromLoadBalancerCmd.class.getName());

    private static final String s_name = "removecertfromloadbalancerresponse";

    @Parameter(name = ApiConstants.LBID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the load balancer rule")
    Long lbRuleId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        boolean result = _lbService.removeCertFromLoadBalancer(getLbRuleId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove certificate from load balancer rule");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LB_CERT_REMOVE;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventDescription() {
        return "Removing a certificate from a load balancer with ID " + getLbRuleId();
    }

    @Override
    public long getEntityOwnerId() {
        LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, getLbRuleId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    public Long getLbRuleId() {
        return this.lbRuleId;
    }
}
