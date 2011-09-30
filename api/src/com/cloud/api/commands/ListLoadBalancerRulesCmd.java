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
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.network.rules.LoadBalancer;

@Implementation(description = "Lists load balancer rules.", responseObject = LoadBalancerResponse.class)
public class ListLoadBalancerRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListLoadBalancerRulesCmd.class.getName());

    private static final String s_name = "listloadbalancerrulesresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account of the load balancer rule. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.LONG, description = "the domain ID of the load balancer rule. If used with the account parameter, lists load balancer rules for the account in the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "the ID of the load balancer rule")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name = ApiConstants.PUBLIC_IP_ID, type = CommandType.LONG, description = "the public IP address id of the load balancer rule	")
    private Long publicIpId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.LONG, description = "the ID of the virtual machine of the load balancer rule")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.LONG, description = "the availability zone ID")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list port forwarding rules by project")
    private Long projectId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public Long getPublicIpId() {
        return publicIpId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public Long getProjectId() {
        return projectId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        List<? extends LoadBalancer> loadBalancers = _lbService.searchForLoadBalancers(this);
        ListResponse<LoadBalancerResponse> response = new ListResponse<LoadBalancerResponse>();
        List<LoadBalancerResponse> lbResponses = new ArrayList<LoadBalancerResponse>();
        if (loadBalancers != null) {
            for (LoadBalancer loadBalancer : loadBalancers) {
                LoadBalancerResponse lbResponse = _responseGenerator.createLoadBalancerResponse(loadBalancer);
                lbResponse.setObjectName("loadbalancerrule");
                lbResponses.add(lbResponse);
            }
        }
        response.setResponses(lbResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
