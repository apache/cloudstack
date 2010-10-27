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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.NetworkManager;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;

@Implementation(method="rebootRouter", manager=NetworkManager.class, description="Starts a router.")
public class RebootRouterCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RebootRouterCmd.class.getName());
    private static final String s_name = "rebootrouterresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the ID of the router")
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
        DomainRouterVO router = ApiDBUtils.findDomainRouterById(getId());
        if (router != null) {
            return router.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ROUTER_REBOOT;
    }

    @Override
    public String getEventDescription() {
        return  "rebooting router: " + getId();
    }

    @Override @SuppressWarnings("unchecked")
    public DomainRouterResponse getResponse() {
        DomainRouterResponse response = new DomainRouterResponse();
        DomainRouterVO router = (DomainRouterVO)getResponseObject();
      
        response.setId(router.getId());
        response.setZoneId(router.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(router.getDataCenterId()).getName());
        response.setDns1(router.getDns1());
        response.setDns2(router.getDns2());
        response.setNetworkDomain(router.getDomain());
        response.setGateway(router.getGateway());
        response.setName(router.getName());
        response.setPodId(router.getPodId());

        if (router.getHostId() != null) {
            response.setHostId(router.getHostId());
            response.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
        } 

        response.setPrivateIp(router.getPrivateIpAddress());
        response.setPrivateMacAddress(router.getPrivateMacAddress());
        response.setPrivateNetmask(router.getPrivateNetmask());
        response.setPublicIp(router.getPublicIpAddress());
        response.setPublicMacAddress(router.getPublicMacAddress());
        response.setPublicNetmask(router.getPublicNetmask());
        response.setGuestIpAddress(router.getGuestIpAddress());
        response.setGuestMacAddress(router.getGuestMacAddress());
        response.setGuestNetmask(router.getGuestNetmask());
        response.setTemplateId(router.getTemplateId());
        response.setCreated(router.getCreated());
        response.setState(router.getState());

        Account accountTemp = ApiDBUtils.findAccountById(router.getAccountId());
        if (accountTemp != null) {
            response.setAccountName(accountTemp.getAccountName());
            response.setDomainId(accountTemp.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }
        
        response.setResponseName(getName());
        return response;
    }
}
