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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.network.FirewallRuleVO;
import com.cloud.uservm.UserVm;

@Implementation(method="createPortForwardingRule", manager=Manager.NetworkManager)
public class CreateIPForwardingRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIPForwardingRuleCmd.class.getName());

    private static final String s_name = "createportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="ipaddress", type=CommandType.STRING, required=true)
    private String ipAddress;

    @Parameter(name="privateport", type=CommandType.STRING, required=true)
    private String privatePort;

    @Parameter(name="protocol", type=CommandType.STRING, required=true)
    private String protocol;

    @Parameter(name="publicport", type=CommandType.STRING, required=true)
    private String publicPort;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=true)
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

    @Override
    public String getResponse() {
        FirewallRuleVO fwRule = (FirewallRuleVO)getResponseObject();

        FirewallRuleResponse fwResponse = new FirewallRuleResponse();
        fwResponse.setId(fwRule.getId());
        fwResponse.setPrivatePort(fwRule.getPrivatePort());
        fwResponse.setProtocol(fwRule.getProtocol());
        fwResponse.setPublicPort(fwRule.getPublicPort());

        UserVm vm = ApiDBUtils.findUserVmById(virtualMachineId);
        fwResponse.setVirtualMachineId(vm.getId());
        fwResponse.setVirtualMachineName(vm.getName());

        fwResponse.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(fwResponse);
    }
}
