/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.PortForwardingServiceRuleResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.network.NetworkRuleConfigVO;

@Implementation(method="searchForNetworkRules")
public class ListPortForwardingServiceRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPortForwardingServiceRulesCmd.class.getName());

    private static final String s_name = "listportforwardingservicerulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="portforwardingserviceid", type=CommandType.LONG)
    private Long portForwardingServiceId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public Long getPortForwardingServiceId() {
        return portForwardingServiceId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<NetworkRuleConfigVO> portForwardingServiceRules = (List<NetworkRuleConfigVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<PortForwardingServiceRuleResponse> ruleResponses = new ArrayList<PortForwardingServiceRuleResponse>();
        for (NetworkRuleConfigVO rule : portForwardingServiceRules) {
            PortForwardingServiceRuleResponse ruleResponse = new PortForwardingServiceRuleResponse();
            ruleResponse.setRuleId(rule.getId());
            ruleResponse.setPortForwardingServiceId(rule.getSecurityGroupId());
            ruleResponse.setPublicPort(rule.getPublicPort());
            ruleResponse.setPrivatePort(rule.getPrivatePort());
            ruleResponse.setProtocol(rule.getProtocol());

            AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("network_rule_config", rule.getId());
            if(asyncJob != null) {
                ruleResponse.setJobId(asyncJob.getId());
                ruleResponse.setJobStatus(asyncJob.getStatus());
            }

            ruleResponse.setResponseName("portforwardingservicerule");
            ruleResponses.add(ruleResponse);
        }

        response.setResponses(ruleResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
