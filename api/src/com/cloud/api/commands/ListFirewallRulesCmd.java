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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.rules.FirewallRule;

@Implementation(description="Lists all firewall rules for an IP address.", responseObject=FirewallResponse.class)
public class ListFirewallRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListFirewallRulesCmd.class.getName());

    private static final String s_name = "listfirewallrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="Lists rule with the specified ID.")
    private Long id;
    
    @Parameter(name=ApiConstants.IP_ADDRESS_ID, type=CommandType.LONG, description="the id of IP address of the firwall services")
    private Long ipAddressId;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists firewall rules for the specified account in this domain.")
    private Long domainId;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list firewall rules by project")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }
    
    public Long getIpAddressId() {
        return ipAddressId;
    }
    
    public Long getId() {
        return id;
    }
    
    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends FirewallRule> result = _firewallService.listFirewallRules(this);
        ListResponse<FirewallResponse> response = new ListResponse<FirewallResponse>();
        List<FirewallResponse> fwResponses = new ArrayList<FirewallResponse>();
        
        for (FirewallRule fwRule : result) {
            FirewallResponse ruleData = _responseGenerator.createFirewallResponse(fwRule);
            ruleData.setObjectName("firewallrule");
            fwResponses.add(ruleData);
        }
        response.setResponses(fwResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response); 
    }
}
