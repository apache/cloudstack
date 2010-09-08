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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.domain.DomainVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.DomainRouterVO;

@Implementation(method="searchForRouters")
public class ListRoutersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListRoutersCmd.class.getName());

    private static final String s_name = "listroutersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="hostid", type=CommandType.LONG)
    private Long hostId;

    @Parameter(name="name", type=CommandType.STRING)
    private String routerName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="state", type=CommandType.STRING)
    private String state;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getRouterName() {
        return routerName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<DomainRouterVO> routers = (List<DomainRouterVO>)getResponseObject();

        List<DomainRouterResponse> response = new ArrayList<DomainRouterResponse>();
        for (DomainRouterVO router : routers) {
            DomainRouterResponse routerResponse = new DomainRouterResponse();
            routerResponse.setId(router.getId());

            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("domain_router", router.getId());
            if (asyncJob != null) {
                routerResponse.setJobId(asyncJob.getId());
                routerResponse.setJobStatus(asyncJob.getStatus());
            } 

            routerResponse.setZoneId(router.getDataCenterId());
            routerResponse.setZoneName(getManagementServer().findDataCenterById(router.getDataCenterId()).getName());
            routerResponse.setDns1(router.getDns1());
            routerResponse.setDns2(router.getDns2());
            routerResponse.setNetworkDomain(router.getDomain());
            routerResponse.setGateway(router.getGateway());
            routerResponse.setName(router.getName());
            routerResponse.setPodId(router.getPodId());

            if (router.getHostId() != null) {
                routerResponse.setHostId(router.getHostId());
                routerResponse.setHostName(getManagementServer().getHostBy(router.getHostId()).getName());
            } 

            routerResponse.setPrivateIp(router.getPrivateIpAddress()());
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

            Account accountTemp = getManagementServer().findAccountById(router.getAccountId());
            if (accountTemp != null) {
                routerResponse.setAccountName(accountTemp.getAccountName());
                routerResponse.setDomainId(accountTemp.getDomainId());
                routerResponse.setDomain(getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName());
            }

            response.add(routerResponse);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
