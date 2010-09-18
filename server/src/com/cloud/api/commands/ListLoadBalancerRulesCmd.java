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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.network.LoadBalancerVO;
import com.cloud.user.Account;

@Implementation(method="searchForLoadBalancers")
public class ListLoadBalancerRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger (ListLoadBalancerRulesCmd.class.getName());

    private static final String s_name = "listloadbalancerrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String loadBalancerRuleName;

    @Parameter(name="publicip", type=CommandType.STRING)
    private String publicIp;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public String getPublicIp() {
        return publicIp;
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
    public String getResponse() {
        List<LoadBalancerVO> loadBalancers = (List<LoadBalancerVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<LoadBalancerResponse> lbResponses = new ArrayList<LoadBalancerResponse>();
        for (LoadBalancerVO loadBalancer : loadBalancers) {
            LoadBalancerResponse lbResponse = new LoadBalancerResponse();
            lbResponse.setId(loadBalancer.getId());
            lbResponse.setName(loadBalancer.getName());
            lbResponse.setDescription(loadBalancer.getDescription());
            lbResponse.setPublicIp(loadBalancer.getIpAddress());
            lbResponse.setPublicPort(loadBalancer.getPublicPort());
            lbResponse.setPrivatePort(loadBalancer.getPrivatePort());
            lbResponse.setAlgorithm(loadBalancer.getAlgorithm());

            Account accountTemp = ApiDBUtils.findAccountById(loadBalancer.getAccountId());
            if (accountTemp != null) {
                lbResponse.setAccountName(accountTemp.getAccountName());
                lbResponse.setDomainId(accountTemp.getDomainId());
                lbResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
            }

            lbResponse.setResponseName("loadbalancerrule");
            lbResponses.add(lbResponse);
        }

        response.setResponses(lbResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
