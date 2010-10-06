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
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.DomainRouterVO;


@Implementation(method="stopRouter", manager=Manager.NetworkManager)
public class StopRouterCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(StopRouterCmd.class.getName());
    private static final String s_name = "stoprouterresponse";

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

    @Override
    public long getAccountId() {
        UserVm vm = ApiDBUtils.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ROUTER_STOP;
    }

    @Override
    public String getEventDescription() {
        return  "stopping router: " + getId();
    }

    @Override @SuppressWarnings("unchecked")
    public DomainRouterResponse getResponse() {
        DomainRouterVO router = (DomainRouterVO)getResponseObject();

        DomainRouterResponse response = new DomainRouterResponse();
        response.setId(router.getId());
        response.setZoneId(router.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(router.getDataCenterId()).getName());
        response.setDns1(router.getDns1());
        response.setDns2(router.getDns2());
        response.setNetworkDomain(router.getDomain());
        response.setGateway(router.getGateway());
        response.setName(router.getName());
        response.setPodId(router.getPodId());
        response.setPrivateIp(router.getPrivateIpAddress());
        response.setPrivateMacAddress(router.getPrivateMacAddress());
        response.setPrivateNetmask(router.getPrivateNetmask());
        response.setPublicIp(router.getPublicIpAddress());
        response.setPublicMacAddress(router.getPublicMacAddress());
        response.setPublicNetmask(router.getPrivateNetmask());
        response.setGuestIpAddress(router.getGuestIpAddress());
        response.setGuestMacAddress(router.getGuestMacAddress());
        response.setTemplateId(router.getTemplateId());
        response.setCreated(router.getCreated());
        response.setGuestNetmask(router.getGuestNetmask());

        if (router.getHostId() != null) {
            response.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
            response.setHostId(router.getHostId());
        }

        Account acct = ApiDBUtils.findAccountById(router.getAccountId());
        if (acct != null) {
            response.setAccountName(acct.getAccountName());
            response.setDomainId(acct.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        }

        if (router.getState() != null) {
            response.setState(router.getState());
        }

        response.setResponseName(getName());
        return response;
    }
}
