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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.NetworkManager;

@Implementation(method="listPortForwardingRules", manager=NetworkManager.class, description="Lists all port forwarding rules for an IP address.")
public class ListPortForwardingRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPortForwardingRulesCmd.class.getName());

    private static final String s_name = "listportforwardingrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the IP address of the port forwarding services")
    private String ipAddress;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<FirewallRuleResponse> getResponse() {
        List<FirewallRuleVO> firewallRules = (List<FirewallRuleVO>)getResponseObject();        
        ListResponse<FirewallRuleResponse> response = new ListResponse<FirewallRuleResponse>();
        List<FirewallRuleResponse> fwResponses = new ArrayList<FirewallRuleResponse>();
        
        for (FirewallRuleVO fwRule : firewallRules) {
            FirewallRuleResponse ruleData = ApiResponseHelper.createFirewallRuleResponse(fwRule);
            ruleData.setResponseName("portforwardingrule");
            fwResponses.add(ruleData);
        }

        response.setResponses(fwResponses);
        response.setResponseName(getName());
        return response;
    }
}
