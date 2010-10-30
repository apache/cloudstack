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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.NetworkManager;
import com.cloud.uservm.UserVm;

@Implementation(method="createPortForwardingRule", manager=NetworkManager.class, description="Creates a port forwarding rule")
public class CreateIPForwardingRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIPForwardingRuleCmd.class.getName());

    private static final String s_name = "createportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the IP address of the port forwarding rule")
    private String ipAddress;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.STRING, required=true, description="the private port of the port forwarding rule")
    private String privatePort;

    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, required=true, description="the protocol for the port fowarding rule. Valid values are TCP or UDP.")
    private String protocol;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.STRING, required=true, description="	the public port of the port forwarding rule")
    private String publicPort;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine for the port forwarding rule")
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
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

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public FirewallRuleResponse getResponse() {
        FirewallRuleVO fwRule = (FirewallRuleVO)getResponseObject();
        if (fwRule != null) {
            FirewallRuleResponse fwResponse = new FirewallRuleResponse();
            fwResponse.setId(fwRule.getId());
            fwResponse.setPrivatePort(fwRule.getPrivatePort());
            fwResponse.setProtocol(fwRule.getProtocol());
            fwResponse.setPublicPort(fwRule.getPublicPort());

            UserVm vm = ApiDBUtils.findUserVmById(virtualMachineId);
            fwResponse.setVirtualMachineId(vm.getId());
            fwResponse.setVirtualMachineName(vm.getName());

            fwResponse.setResponseName(getName());
            return fwResponse;
        }

        throw new ServerApiException(NET_CREATE_IPFW_RULE_ERROR, "An existing rule for ipAddress / port / protocol of " + ipAddress + " / " + publicPort + " / " + protocol + " exits.");
    }
}
