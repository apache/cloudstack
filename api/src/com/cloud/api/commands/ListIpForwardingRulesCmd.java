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

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.utils.net.Ip;

@Implementation(description="List the ip forwarding rules", responseObject=FirewallRuleResponse.class)
public class ListIpForwardingRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListIpForwardingRulesCmd.class.getName());

    private static final String s_name = "listipforwardingrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.IP_ADDRESS, required=true, type=CommandType.STRING, description="list the rule belonging to this public ip address")
    private String publicIpAddress;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the ip forwarding rule. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="Lists all rules for this id. If used with the account parameter, returns all rules for an account in the specified domain ID.")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
	public String getName() {
        return s_name;
    }
    
    public String getPublicIpAddress() {
		return publicIpAddress;
	}

	public void setPublicIpAddress(String publicIpAddress) {
		this.publicIpAddress = publicIpAddress;
	}

	public String getAccountName() {
		return accountName;
	}

	public Long getDomainId() {
		return domainId;
	}

	@Override
    public void execute(){
        List<? extends PortForwardingRule> result = _rulesService.searchForIpForwardingRules(new Ip(publicIpAddress), this.getStartIndex(), this.getPageSizeVal());
        ListResponse<IpForwardingRuleResponse> response = new ListResponse<IpForwardingRuleResponse>();
        List<IpForwardingRuleResponse> ipForwardingResponses = new ArrayList<IpForwardingRuleResponse>();
        for (PortForwardingRule rule : result) {
            IpForwardingRuleResponse resp = _responseGenerator.createIpForwardingRuleResponse(rule);
            if (resp != null) {
                ipForwardingResponses.add(resp);
            }
        }
        response.setResponses(ipForwardingResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
    
}
