/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package com.cloud.api.commands;


import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.event.EventTypes;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.api.response.LBStickinessResponse;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;


@Implementation(description = "Creates a Load Balancer stickiness policy ", responseObject = LBStickinessResponse.class)
@SuppressWarnings("rawtypes")
public class CreateLBStickinessPolicyCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger
            .getLogger(CreateLBStickinessPolicyCmd.class.getName());

    private static final String s_name = "createLBStickinessPolicy";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="firewall_rules")
    @Parameter(name = ApiConstants.LBID, type = CommandType.LONG, required = true, description = "the ID of the load balancer rule")
    private Long lbRuleId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the LB Stickiness policy")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the LB Stickiness policy")
    private String lbStickinessPolicyName;

    @Parameter(name = ApiConstants.METHOD_NAME, type = CommandType.STRING, required = true, description = "name of the LB Stickiness policy method, possible values can be obtained from ListNetworks API ")
    private String stickinessMethodName;

    @Parameter(name = ApiConstants.PARAM_LIST, type = CommandType.MAP, description = "param list. Example: param[0].name=cookiename&param[0].value=LBCookie ")
    private Map paramList;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getLbRuleId() {
        return lbRuleId;
    }

    public String getDescription() {
        return description;
    }

    public String getLBStickinessPolicyName() {
        return lbStickinessPolicyName;
    }

    public String getStickinessMethodName() {
        return stickinessMethodName;
    }

    public Map getparamList() {
        return paramList;
    }

    public String getEntityTable() {
        return "firewall_rules";
    }
    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {
        StickinessPolicy policy = null;
        boolean success = false;
        
        try {
            UserContext.current().setEventDetails("Rule Id: " + getEntityId());
            success = _lbService.applyLBStickinessPolicy(this);
            if (success) {
                // State might be different after the rule is applied, so get new object here
                policy = _entityMgr.findById(StickinessPolicy.class, getEntityId());
                LoadBalancer lb = _lbService.findById(policy.getLoadBalancerId());
                LBStickinessResponse spResponse = _responseGenerator.createLBStickinessPolicyResponse(policy, lb);
                setResponseObject(spResponse);
                spResponse.setResponseName(getCommandName());
            }
        } finally {
            if (!success || (policy == null)) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create stickiness policy ");  
            }
        } 
    }

    @Override
    public void create() {
        try {
            StickinessPolicy result = _lbService.createLBStickinessPolicy(this);
            this.setEntityId(result.getId());
        } catch (NetworkRuleConflictException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating a Load Balancer Stickiness policy: " + getLBStickinessPolicyName();
    }

}

