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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class ListIPForwardingRulesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListIPForwardingRulesCmd.class.getName());

    private static final String s_name = "listforwardingrulesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
         s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
         s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.TRUE));
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
        String ipAddress = (String)params.get(BaseCmd.Properties.IP_ADDRESS.getName());

        IPAddressVO ipAddressVO = getManagementServer().findIPAddressById(ipAddress);
        if (ipAddressVO == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find IP address " + ipAddress);
        }

        Account addrOwner = getManagementServer().findAccountById(ipAddressVO.getAccountId());

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if ((account != null) && isAdmin(account.getType())) {
            if (ipAddressVO.getAccountId() != null) {
                if ((addrOwner != null) && !getManagementServer().isChildDomain(addrOwner.getDomainId(), account.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list forwarding rules for address " + ipAddress + ", permission denied for account " + account.getId());
                }
            } else {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to list forwarding rules for address " + ipAddress + ", address not in use.");
            }
        } else {
            if (account != null) {
                if ((ipAddressVO.getAccountId() == null) || (account.getId().longValue() != ipAddressVO.getAccountId().longValue())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list forwarding rules for address " + ipAddress + ", permission denied for account " + account.getId());
                }
                addrOwner = account;
            }
        }

        List<FirewallRuleVO> firewallRules = getManagementServer().listIPForwarding(ipAddress, true);

        if (firewallRules == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching forwarding rules for address " + ipAddress);
        }

        Criteria lbCriteria = new Criteria();
        lbCriteria.addCriteria(Criteria.IPADDRESS, ipAddress);
        List<LoadBalancerVO> loadBalancers = getManagementServer().searchForLoadBalancers(lbCriteria);
        if (loadBalancers == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching load balancer rules for address " + ipAddress);
        }

        Map<String, UserVmVO> userVmCache = new HashMap<String, UserVmVO>();

        List<Pair<String, Object>> groupsTags = new ArrayList<Pair<String, Object>>();
        Object[] forwardingTag = new Object[firewallRules.size()];
        int i = 0;
        for (FirewallRuleVO fwRule : firewallRules) {
            List<Pair<String, Object>> ruleData = new ArrayList<Pair<String, Object>>();

            ruleData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), fwRule.getId().toString()));
            ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), fwRule.getPublicPort()));
            ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), fwRule.getPrivatePort()));
            ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PROTOCOL.getName(), fwRule.getProtocol()));

            UserVmVO userVM = userVmCache.get(fwRule.getPrivateIpAddress());
            if (userVM == null) {
                Criteria c = new Criteria();
                c.addCriteria(Criteria.ACCOUNTID, new Object[] {addrOwner.getId()});
                c.addCriteria(Criteria.DATACENTERID, ipAddressVO.getDataCenterId());
                c.addCriteria(Criteria.IPADDRESS, fwRule.getPrivateIpAddress());
                List<UserVmVO> userVMs = getManagementServer().searchForUserVMs(c);

                if ((userVMs != null) && (userVMs.size() > 0)) {
                    userVM = userVMs.get(0);
                    userVmCache.put(fwRule.getPrivateIpAddress(), userVM);
                }
            }

            if (userVM != null) {
                ruleData.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName(), userVM.getId()));
                ruleData.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_NAME.getName(), userVM.getName()));
            }

            forwardingTag[i++] = ruleData;
        }
        Pair<String, Object> forwardingTags = new Pair<String, Object>("fowardingrule", forwardingTag);
        groupsTags.add(forwardingTags);

        Object[] lbTag = new Object[loadBalancers.size()];
        i = 0;
        for (LoadBalancerVO loadBalancer : loadBalancers) {
            List<Pair<String, Object>> lbData = new ArrayList<Pair<String, Object>>();

            lbData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), loadBalancer.getId().toString()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), loadBalancer.getPublicPort()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), loadBalancer.getPrivatePort()));
            lbData.add(new Pair<String, Object>(BaseCmd.Properties.ALGORITHM.getName(), loadBalancer.getAlgorithm()));

            lbTag[i++] = lbData;
        }
        Pair<String, Object> lbTags = new Pair<String, Object>("loadbalancer", lbTag);
        groupsTags.add(lbTags);
        return groupsTags;
    }
}
