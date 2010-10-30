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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VMInstanceVO;

@Implementation(method="rebootSystemVM", manager=ManagementServer.class, description="Reboots a system VM.")
public class RebootSystemVmCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RebootSystemVmCmd.class.getName());

    private static final String s_name = "rebootsystemvmresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the system virtual machine")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SSVM_REBOOT;
    }

    @Override
    public String getEventDescription() {
        return  "rebooting system vm: " + getId();
    }

    @Override @SuppressWarnings("unchecked")
	public SystemVmResponse getResponse() {
        VMInstanceVO instance = (VMInstanceVO)getResponseObject();

        SystemVmResponse response = new SystemVmResponse();
        response.setId(instance.getId());
        response.setName(instance.getName());
        response.setZoneId(instance.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(instance.getDataCenterId()).getName());
        response.setPodId(instance.getPodId());
        response.setHostId(instance.getHostId());
        if (response.getHostId() != null) {
            response.setHostName(ApiDBUtils.findHostById(instance.getHostId()).getName());
        }
        
        response.setPrivateIp(instance.getPrivateIpAddress());
        response.setPrivateMacAddress(instance.getPrivateMacAddress());
        response.setPrivateNetmask(instance.getPrivateNetmask());
        response.setTemplateId(instance.getTemplateId());
        response.setCreated(instance.getCreated());
        response.setState(instance.getState().toString());

        if (instance instanceof SecondaryStorageVmVO) {
            SecondaryStorageVmVO ssVm = (SecondaryStorageVmVO) instance;
            response.setDns1(ssVm.getDns1());
            response.setDns2(ssVm.getDns2());
            response.setNetworkDomain(ssVm.getDomain());
            response.setGateway(ssVm.getGateway());

            response.setPublicIp(ssVm.getPublicIpAddress());
            response.setPublicMacAddress(ssVm.getPublicMacAddress());
            response.setPublicNetmask(ssVm.getPublicNetmask());
        } else if (instance instanceof ConsoleProxyVO) {
            ConsoleProxyVO proxy = (ConsoleProxyVO)instance;
            response.setDns1(proxy.getDns1());
            response.setDns2(proxy.getDns2());
            response.setNetworkDomain(proxy.getDomain());
            response.setGateway(proxy.getGateway());
            
            response.setPublicIp(proxy.getPublicIpAddress());
            response.setPublicMacAddress(proxy.getPublicMacAddress());
            response.setPublicNetmask(proxy.getPublicNetmask());
            response.setActiveViewerSessions(proxy.getActiveSession());
        }

        response.setResponseName(getName());
        return response;
	}
}
