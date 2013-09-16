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

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "createGlobalLoadBalancerRule", description="Creates a global load balancer rule",
        responseObject=GlobalLoadBalancerResponse.class)
public class CreateGlobalLoadBalancerRuleCmd extends BaseAsyncCreateCmd {

    public static final Logger s_logger = Logger.getLogger(CreateGlobalLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "creategloballoadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the load balancer rule")
    private String globalLoadBalancerRuleName;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule", length=4096)
    private String description;

    @Parameter(name=ApiConstants.REGION_ID, type=CommandType.INTEGER, entityType = RegionResponse.class, required=true, description="region where the global load balancer is going to be created.")
    private Integer regionId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the global load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class, description="the domain ID associated with the load balancer")
    private Long domainId;

    @Parameter(name=ApiConstants.GSLB_LB_METHOD, type=CommandType.STRING, required=false, description="load balancer algorithm (roundrobin, leastconn, proximity) " +
            "that method is used to distribute traffic across the zones participating in global server load balancing, if not specified defaults to 'round robin'")
    private String algorithm;

    @Parameter(name=ApiConstants.GSLB_STICKY_SESSION_METHOD, type=CommandType.STRING, required=false, description="session sticky method (sourceip) if not specified defaults to sourceip")
    private String stickyMethod;

    @Parameter(name=ApiConstants.GSLB_SERVICE_DOMAIN_NAME, type = CommandType.STRING, required = true, description = "domain name for the GSLB service.")
    private String serviceDomainName;

    @Parameter(name=ApiConstants.GSLB_SERVICE_TYPE, type = CommandType.STRING, required = true, description = "GSLB service type (tcp, udp)")
    private String serviceType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return globalLoadBalancerRuleName;
    }

    public String getDescription() {
        return description;
    }

    public String getAlgorithm() {
        if (algorithm != null) {
            return algorithm;
        } else {
            return GlobalLoadBalancerRule.Algorithm.RoundRobin.name();
        }
    }

    public String getGslbMethod() {
        return algorithm;
    }

    public String getStickyMethod() {
        if (stickyMethod == null) {
            return "sourceip";
        }
        return stickyMethod;
    }

    public String getServiceDomainName() {
        return serviceDomainName;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public String getServiceType() {
        return serviceType;
    }

    @Inject
    private GlobalLoadBalancingRulesService _gslbService;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {

        CallContext callerContext = CallContext.current();
        GlobalLoadBalancerRule rule = _entityMgr.findById(GlobalLoadBalancerRule.class, getEntityId());
        GlobalLoadBalancerResponse response = null;
        if (rule != null) {
            response = _responseGenerator.createGlobalLoadBalancerResponse(rule);
            setResponseObject(response);
        }
        response.setResponseName(getCommandName());
    }

    @Override
    public void create() {
        try {
            GlobalLoadBalancerRule gslbRule = _gslbService.createGlobalLoadBalancerRule(this);
            this.setEntityId(gslbRule.getId());
            this.setEntityUuid(gslbRule.getUuid());
            CallContext.current().setEventDetails("Rule Id: " + getEntityId());
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage());
        }finally {

        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GLOBAL_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating a global load balancer rule Id: " + getEntityId();

    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.GlobalLoadBalancerRule;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }
}
