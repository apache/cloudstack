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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;


@Implementation(method="startRouter", manager=Manager.NetworkManager)
public class StartRouterCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(StartRouterCmd.class.getName());
    private static final String s_name = "startrouterresponse";


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
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

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "router"; 
    }
    
	@Override @SuppressWarnings("unchecked")
	public DomainRouterResponse getResponse() {
        DomainRouterResponse routerResponse = new DomainRouterResponse();
        DomainRouterVO router = (DomainRouterVO)getResponseObject();
        
        routerResponse.setId(router.getId());
        routerResponse.setZoneId(router.getDataCenterId());
        routerResponse.setZoneName(ApiDBUtils.findZoneById(router.getDataCenterId()).getName());
        routerResponse.setDns1(router.getDns1());
        routerResponse.setDns2(router.getDns2());
        routerResponse.setNetworkDomain(router.getDomain());
        routerResponse.setGateway(router.getGateway());
        routerResponse.setName(router.getName());
        routerResponse.setPodId(router.getPodId());

        if (router.getHostId() != null) {
            routerResponse.setHostId(router.getHostId());
            routerResponse.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
        } 

        routerResponse.setPrivateIp(router.getPrivateIpAddress());
        routerResponse.setPrivateMacAddress(router.getPrivateMacAddress());
        routerResponse.setPrivateNetmask(router.getPrivateNetmask());
        routerResponse.setPublicIp(router.getPublicIpAddress());
        routerResponse.setPublicMacAddress(router.getPublicMacAddress());
        routerResponse.setPublicNetmask(router.getPublicNetmask());
        routerResponse.setGuestIpAddress(router.getGuestIpAddress());
        routerResponse.setGuestMacAddress(router.getGuestMacAddress());
        routerResponse.setGuestNetmask(router.getGuestNetmask());
        routerResponse.setTemplateId(router.getTemplateId());
        routerResponse.setCreated(router.getCreated());
        routerResponse.setState(router.getState());

        Account accountTemp = ApiDBUtils.findAccountById(router.getAccountId());
        if (accountTemp != null) {
            routerResponse.setAccountName(accountTemp.getAccountName());
            routerResponse.setDomainId(accountTemp.getDomainId());
            routerResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }
        
        routerResponse.setResponseName(getName());
        return routerResponse;
	}
}
