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
import com.cloud.api.ServerApiException;
import com.cloud.network.LoadBalancerVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class ListLoadBalancerRuleInstancesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger (ListLoadBalancerRuleInstancesCmd.class.getName());

    private static final String s_name = "listloadbalancerruleinstancesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    
    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.APPLIED, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long loadBalancerId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Boolean applied = (Boolean)params.get(BaseCmd.Properties.APPLIED.getName());

        if (applied == null) {
            applied = Boolean.TRUE;
        }

        LoadBalancerVO loadBalancer = getManagementServer().findLoadBalancerById(loadBalancerId.longValue());
        if (loadBalancer == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "no load balancer rule with id " + loadBalancerId + " exists.");
        }

        if (account != null) {
            long lbAcctId = loadBalancer.getAccountId();
            if (isAdmin(account.getType())) {
                Account userAccount = getManagementServer().findAccountById(Long.valueOf(lbAcctId));
                if (!getManagementServer().isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid load balancer rule id (" + loadBalancerId + ") given, unable to list instances.");
                }
            } else if (account.getId().longValue() != lbAcctId) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName());
            }
        }

        List<UserVmVO> instances = getManagementServer().listLoadBalancerInstances(loadBalancerId.longValue(), applied.booleanValue());

        if (instances == null) {
            throw new ServerApiException(BaseCmd.NET_LIST_ERROR, "unable to find instances for load balancer rule " + loadBalancerId);
        }

        List<Pair<String, Object>> instanceTags = new ArrayList<Pair<String, Object>>();
        Object[] instanceTag = new Object[instances.size()];
        int i = 0;
        for (UserVmVO instance : instances) {
            List<Pair<String, Object>> instanceData = new ArrayList<Pair<String, Object>>();

            instanceData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), instance.getId().toString()));
            instanceData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), instance.getName()));
            instanceData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_NAME.getName(), instance.getDisplayName()));
            instanceData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_IP.getName(), instance.getPrivateIpAddress()));
            
            Account accountTemp = getManagementServer().findAccountById(instance.getAccountId());
            if (accountTemp != null) {
            	instanceData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
            	instanceData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
            	instanceData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
            }
            instanceTag[i++] = instanceData;
        }
        Pair<String, Object> ruleTag = new Pair<String, Object>("loadbalancerruleinstance", instanceTag);
        instanceTags.add(ruleTag);
        return instanceTags;
    }
}


