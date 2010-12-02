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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

@Implementation(description="Creates a load balancer rule", responseObject=LoadBalancerResponse.class)
public class CreateLoadBalancerRuleCmd extends BaseCmd implements LoadBalancer {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALGORITHM, type=CommandType.STRING, required=true, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule")
    private String description;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.STRING, required=true, description="the private port of the private ip address/virtual machine where the network traffic will be load balanced to")
    private String privatePort;

    @Parameter(name=ApiConstants.PUBLIC_IP, type=CommandType.STRING, required=true, description="public ip address from where the network traffic will be load balanced from")
    private String publicIp;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.STRING, required=true, description="the public port from where the network traffic will be load balanced from")
    private String publicPort;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
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

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public void execute() {
        LoadBalancer result = null;
        try {
            result = _lbService.createLoadBalancerRule(this);
        } catch (NetworkRuleConflictException e) {
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
        LoadBalancerResponse response = _responseGenerator.createLoadBalancerResponse(result);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public String getXid() {
        // FIXME: Should fix this.
        return null;
    }

    @Override
    public Ip getSourceIpAddress() {
        return new Ip(publicIp);
    }

    @Override
    public int getSourcePortStart() {
        return Integer.parseInt(publicPort);
    }

    @Override
    public int getSourcePortEnd() {
        return Integer.parseInt(publicPort);
    }

    @Override
    public String getProtocol() {
        return NetUtils.TCP_PROTO;
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.LoadBalancing;
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public long getNetworkId() {
        return -1;
    }

    @Override
    public long getAccountId() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public long getDomainId() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public int getDefaultPortStart() {
        return Integer.parseInt(privatePort);
    }

    @Override
    public int getDefaultPortEnd() {
        return Integer.parseInt(privatePort);
    }

    @Override
    public List<? extends Destination> getDestinations() {
        throw new UnsupportedOperationException("not supported");
    }
}
