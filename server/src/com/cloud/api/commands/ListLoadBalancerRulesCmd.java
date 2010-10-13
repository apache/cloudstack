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
import com.cloud.domain.DomainVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListLoadBalancerRulesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger (ListLoadBalancerRulesCmd.class.getName());

    private static final String s_name = "listloadbalancerrulesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    
    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_IP, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
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
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String publicIp = (String)params.get(BaseCmd.Properties.PUBLIC_IP.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long accountId = null;
        boolean isAdmin = false;
        Account ipAddressOwner = null;

        if (publicIp != null) {
            IPAddressVO ipAddressVO = getManagementServer().findIPAddressById(publicIp);
            if (ipAddressVO == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find IP address " + publicIp);
            } else {
                ipAddressOwner = getManagementServer().findAccountById(ipAddressVO.getAccountId());
            }
        }

        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list load balancer rules.");
                }
                if (accountName != null) {
                    Account userAccount = getManagementServer().findAccountByName(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (ipAddressOwner != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), ipAddressOwner.getDomainId())) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to list load balancer rules for IP address " + publicIp + ", permission denied.");
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }

        Criteria c = new Criteria("ipAddress", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.ACCOUNTID, accountId);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.INSTANCEID, vmId);
        c.addCriteria(Criteria.IPADDRESS, publicIp);
        c.addCriteria(Criteria.KEYWORD, keyword);
        if (isAdmin) {
            c.addCriteria(Criteria.DOMAINID, domainId);
        }

        // FIXME: this should be constrained by domain to search for all load balancers in a domain if an admin is searching
        List<LoadBalancerVO> loadBalancers = getManagementServer().searchForLoadBalancers(c);

        if (loadBalancers == null) {
            throw new ServerApiException(BaseCmd.NET_LIST_ERROR, "unable to find load balancing rules");
        }

        List<Pair<String, Object>> lbTags = new ArrayList<Pair<String, Object>>();
        Object[] lbTag = new Object[loadBalancers.size()];
        int i = 0;
        for (LoadBalancerVO loadBalancer : loadBalancers) {
            List<Pair<String, Object>> lbData = new ArrayList<Pair<String, Object>>();

            lbData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.valueOf(loadBalancer.getId()).toString()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), loadBalancer.getName()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), loadBalancer.getDescription()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_IP.getName(), loadBalancer.getIpAddress()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), loadBalancer.getPublicPort()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), loadBalancer.getPrivatePort()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.ALGORITHM.getName(), loadBalancer.getAlgorithm()));

            Account accountTemp = getManagementServer().findAccountById(loadBalancer.getAccountId());
            if (accountTemp != null) {
            	lbData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
            	lbData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
            	lbData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
            }

            lbTag[i++] = lbData;
        }
        Pair<String, Object> ruleTag = new Pair<String, Object>("loadbalancerrule", lbTag);
        lbTags.add(ruleTag);
        return lbTags;
    }
}
