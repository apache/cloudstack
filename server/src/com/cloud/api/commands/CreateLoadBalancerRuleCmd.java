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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.network.LoadBalancerVO;
import com.cloud.serializer.SerializerHelper;

@Implementation(method="createLoadBalancerRule", manager=Manager.NetworkManager)
public class CreateLoadBalancerRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="algorithm", type=CommandType.STRING, required=true)
    private String algorithm;

    @Parameter(name="description", type=CommandType.STRING)
    private String description;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String loadBalancerRuleName;

    @Parameter(name="privateport", type=CommandType.STRING, required=true)
    private String privatePort;

    @Parameter(name="publicip", type=CommandType.STRING, required=true)
    private String publicIp;

    @Parameter(name="publicport", type=CommandType.STRING, required=true)
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

    @Override
    public String getResponse() {
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
        // TODO:  implement
//        response.setDomainName(responseObj.getDomainName());

        return SerializerHelper.toSerializedString(response);
    }
}
