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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="assignToLoadBalancer", manager=Manager.NetworkManager)
public class AssignToLoadBalancerRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssignToLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "assigntoloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=false)
    private Long virtualMachineId;

    @Parameter(name="virtualmachineids", type=CommandType.LIST, collectionType=CommandType.LONG, required=false)
    private List<Long> virtualMachineIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getLoadBalancerId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public List<Long> getVirtualMachineIds() {
        return virtualMachineIds;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }
    
    /*
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        if ((instanceId == null) && (instanceIds == null)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No virtual machine id (or list if ids) specified.");
        }

        if (account == null) {
            account = getManagementServer().findActiveAccount(accountName, domainId);
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        LoadBalancerVO loadBalancer = getManagementServer().findLoadBalancerById(loadBalancerId.longValue());
        if (loadBalancer == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule, with id " + loadBalancerId);
        } else if (account != null) {
            if (!isAdmin(account.getType()) && (loadBalancer.getAccountId() != account.getId().longValue())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() +
                        " (id:" + loadBalancer.getId() + ")");
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid load balancer rule id (" + loadBalancer.getId() + ") given, unable to assign instances to load balancer rule.");
            }
        }

        long jobId = getManagementServer().assignToLoadBalancerAsync(paramMap);
    }
        */

    @Override
    public String getResponse() {
        // there's no specific response for this command
        return null;
    }
}
