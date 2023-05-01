
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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SslCertResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import org.apache.commons.lang.BooleanUtils;

import java.util.logging.Logger;

@APICommand(name = "assignCertToLoadBalancer", description = "Assigns a certificate to a load balancer rule", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AssignCertToLoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AssignCertToLoadBalancerCmd.class.getName());

    private static final String RESPONSE_NAME = "assigncerttoloadbalancerresponse";

    @Parameter(name = ApiConstants.LBID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the load balancer rule")
    Long lbRuleId;

    @Parameter(name = ApiConstants.CERTIFICATE_ID,
               type = CommandType.UUID,
               entityType = SslCertResponse.class,
               required = true,
               description = "the ID of the certificate")
    Long certId;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false,
        since = "4.17",
        description = "Force assign the certificate. If there is a certificate bound to the LB, it will be removed")
    private Boolean forced;


    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        //To change body of implemented methods use File | Settings | File Templates.
        if (_lbService.assignCertToLoadBalancer(getLbRuleId(), getCertId(), isForced())) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign certificate to load balancer");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LB_CERT_ASSIGN;
    }

    @Override
    public String getCommandName() {
        return RESPONSE_NAME;
    }

    @Override
    public String getEventDescription() {
        return "Assigning a certificate to a load balancer";
    }

    @Override
    public long getEntityOwnerId() {
        LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, getLbRuleId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    public Long getCertId() {
        return certId;
    }

    public Long getLbRuleId() {
        return lbRuleId;
    }

    public boolean isForced() {
        return BooleanUtils.toBoolean(forced);
    }

    public ApiCommandResourceType getInstanceType() {
        return ApiCommandResourceType.LoadBalancerRule;
    }

    public Long getInstanceId() {
        return lbRuleId;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
}

    @Override
    public Long getSyncObjId() {
        LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, getLbRuleId());
        return (lb != null )? lb.getNetworkId(): null;
    }
}
