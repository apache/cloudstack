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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

@APICommand(name = "createLBHealthCheckPolicy",
            description = "Creates a load balancer health check policy",
            responseObject = LBHealthCheckResponse.class,
            since = "4.2.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
@SuppressWarnings("rawtypes")
public class CreateLBHealthCheckPolicyCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLBHealthCheckPolicyCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.LBID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the load balancer rule")
    private Long lbRuleId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the load balancer health check policy")
    private String description;

    @Parameter(name = ApiConstants.HEALTHCHECK_PINGPATH, type = CommandType.STRING, required = false, description = "HTTP ping path")
    private String pingPath;

    @Parameter(name = ApiConstants.HEALTHCHECK_RESPONSE_TIMEOUT,
               type = CommandType.INTEGER,
               required = false,
               description = "Time to wait when receiving a response from the health check (2sec - 60 sec)")
    private int responsTimeOut;

    @Parameter(name = ApiConstants.HEALTHCHECK_INTERVAL_TIME,
               type = CommandType.INTEGER,
               required = false,
               description = "Amount of time between health checks (1 sec - 20940 sec)")
    private int healthCheckInterval;

    @Parameter(name = ApiConstants.HEALTHCHECK_HEALTHY_THRESHOLD,
               type = CommandType.INTEGER,
               required = false,
               description = "Number of consecutive health check success before declaring an instance healthy")
    private int healthyThreshold;

    @Parameter(name = ApiConstants.HEALTHCHECK_UNHEALTHY_THRESHOLD,
               type = CommandType.INTEGER,
               required = false,
               description = "Number of consecutive health check failures before declaring an instance unhealthy")
    private int unhealthyThreshold;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Deprecated
    public Boolean getDisplay() {
        return display;
    }

    @Override
    public boolean isDisplay() {
        if(display == null)
            return true;
        else
            return display;
    }

    public Long getLbRuleId() {
        return lbRuleId;
    }

    public String getDescription() {
        return description;
    }

    public String getPingPath() {
        return pingPath;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    public int getResponsTimeOut() {
        return responsTimeOut;
    }

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public int getHealthyThreshold() {
        return healthyThreshold;
    }

    public int getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {
        HealthCheckPolicy policy = null;
        boolean success = false;

        try {
            CallContext.current().setEventDetails("Load balancer health check policy ID : " + getEntityId());
            success = _lbService.applyLBHealthCheckPolicy(this);
            if (success) {
                // State might be different after the rule is applied, so get new object here
                policy = _entityMgr.findById(HealthCheckPolicy.class, getEntityId());
                LoadBalancer lb = _lbService.findById(policy.getLoadBalancerId());
                LBHealthCheckResponse hcResponse = _responseGenerator.createLBHealthCheckPolicyResponse(policy, lb);
                setResponseObject(hcResponse);
                hcResponse.setResponseName(getCommandName());
            }
        } finally {
            if (!success || (policy == null)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create health check policy");
            }
        }
    }

    @Override
    public void create() {
        try {
            HealthCheckPolicy result = _lbService.createLBHealthCheckPolicy(this);
            this.setEntityId(result.getId());
            this.setEntityUuid(result.getUuid());
        } catch (InvalidParameterValueException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LB_HEALTHCHECKPOLICY_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Create load balancer health check policy";
    }
}
