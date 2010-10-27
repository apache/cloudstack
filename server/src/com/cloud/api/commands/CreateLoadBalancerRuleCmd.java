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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;

@Implementation(method="createLoadBalancerRule", manager=NetworkManager.class, description="Creates a load balancer rule")
public class CreateLoadBalancerRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="algorithm", type=CommandType.STRING, required=true, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name="description", type=CommandType.STRING, description="the description of the load balancer rule")
    private String description;

    @Parameter(name="name", type=CommandType.STRING, required=true, description="name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name="privateport", type=CommandType.STRING, required=true, description="the private port of the private ip address/virtual machine where the network traffic will be load balanced to")
    private String privatePort;

    @Parameter(name="publicip", type=CommandType.STRING, required=true, description="public ip address from where the network traffic will be load balanced from")
    private String publicIp;

    @Parameter(name="publicport", type=CommandType.STRING, required=true, description="the public port from where the network traffic will be load balanced from")
    private String publicPort;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getPublicPort() {
        return publicPort;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public LoadBalancerResponse getResponse() {
        LoadBalancerVO responseObj = (LoadBalancerVO)getResponseObject();

        LoadBalancerResponse response = new LoadBalancerResponse();
        response.setAlgorithm(responseObj.getAlgorithm());
        response.setDescription(responseObj.getDescription());
        response.setId(responseObj.getId());
        response.setName(responseObj.getName());
        response.setPrivatePort(responseObj.getPrivatePort());
        response.setPublicIp(responseObj.getIpAddress());
        response.setPublicPort(responseObj.getPublicPort());
        response.setAccountName(responseObj.getAccountName());
        response.setDomainId(responseObj.getDomainId());
        response.setDomainName(ApiDBUtils.findDomainById(responseObj.getDomainId()).getName());

        response.setResponseName(getName());
        return response;
    }
}
