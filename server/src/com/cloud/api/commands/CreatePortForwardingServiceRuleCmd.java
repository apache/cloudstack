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
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.PortForwardingServiceRuleResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.SecurityGroupVO;
import com.cloud.user.Account;

@Implementation(createMethod="createPortForwardingServiceRule", method="applyPortForwardingServiceRule", description="Creates a port forwarding service rule")
public class CreatePortForwardingServiceRuleCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreatePortForwardingServiceRuleCmd.class.getName());

    private static final String s_name = "createportforwardingserviceruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="portforwardingserviceid", type=CommandType.LONG, required=true, description="the ID of the port forwarding service the rule is being created for")
    private Long portForwardingServiceId;

    @Parameter(name="privateport", type=CommandType.STRING, required=true, description="the port of the private ip address/virtual machine to forward traffic to")
    private String privatePort;

    @Parameter(name="protocol", type=CommandType.STRING, description="TCP is default. UDP is the other supported protocol")
    private String protocol;

    @Parameter(name="publicport", type=CommandType.STRING, required=true, description="the port of the public ip address to forward traffic from")
    private String publicPort;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPortForwardingServiceId() {
        return portForwardingServiceId;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
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
    
    public static String getResultObjectName() {
    	return "portforwardingservicerule";
    }

    @Override
    public long getAccountId() {
        SecurityGroupVO portForwardingService = ApiDBUtils.findPortForwardingServiceById(getPortForwardingServiceId());
        if (portForwardingService != null) {
            return portForwardingService.getAccountId();
        }

        // bad id given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD; // FIXME:  Add a new event?
    }

    @Override
    public String getEventDescription() {
        return  "creating port forwarding rule on service: " + getPortForwardingServiceId() + ", public port: " + getPublicPort() +
                ", priv port: " + getPrivatePort() + ", protocol: " + ((getProtocol() == null) ? "TCP" : getProtocol());
    }

    @Override @SuppressWarnings("unchecked")
    public PortForwardingServiceRuleResponse getResponse() {
        NetworkRuleConfigVO netRule = (NetworkRuleConfigVO)getResponseObject();

        PortForwardingServiceRuleResponse response = new PortForwardingServiceRuleResponse();
        response.setRuleId(netRule.getId());
        response.setPortForwardingServiceId(netRule.getSecurityGroupId());
        response.setPrivatePort(netRule.getPrivatePort());
        response.setProtocol(netRule.getProtocol());
        response.setPublicPort(netRule.getPublicPort());

        response.setResponseName(getName());
        return response;
    }
}
