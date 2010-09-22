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
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class CreateIPForwardingRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIPForwardingRuleCmd.class.getName());

    private static final String s_name = "createportforwardingruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PRIVATE_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PROTOCOL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String ipAddress = (String)params.get(BaseCmd.Properties.IP_ADDRESS.getName());
        String publicPort = (String)params.get(BaseCmd.Properties.PUBLIC_PORT.getName());
        String privatePort = (String)params.get(BaseCmd.Properties.PRIVATE_PORT.getName());
        String protocol = (String)params.get(BaseCmd.Properties.PROTOCOL.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        IPAddressVO ipAddressVO = getManagementServer().findIPAddressById(ipAddress);
        if (ipAddressVO == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find IP address " + ipAddress);
        }

        UserVmVO userVM = getManagementServer().findUserVMInstanceById(vmId);
        if (userVM == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find virtual machine with id " + vmId);
        }

        if ((ipAddressVO.getAccountId() == null) || (ipAddressVO.getAccountId().longValue() != userVM.getAccountId())) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding rule, IP address " + ipAddress + " owner is not the same as owner of virtual machine " + userVM.toString()); 
        }

        if (ipAddressVO.getDataCenterId() != userVM.getDataCenterId()) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding rule, IP address " + ipAddress + " owner is not in the same availability zone as virtual machine " + userVM.toString());
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                Account vmOwner = getManagementServer().findAccountById(userVM.getAccountId());
                if (!getManagementServer().isChildDomain(account.getDomainId(), vmOwner.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding rule, IP address " + ipAddress + " to virtual machine " + vmId + ", permission denied.");
                }
            } else if (account.getId().longValue() != userVM.getAccountId()) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding rule, IP address " + ipAddress + " to virtual machine " + vmId + ", permission denied.");
            }
        }

        FirewallRuleVO firewallRule = null;
        
        try {
            firewallRule = getManagementServer().createPortForwardingRule(userId.longValue(), ipAddressVO, userVM, publicPort, privatePort, protocol);
        } catch (NetworkRuleConflictException ex) {
            throw new ServerApiException(BaseCmd.NET_CONFLICT_IPFW_RULE_ERROR, "Network rule conflict creating a forwarding rule on address:port " + ipAddress + ":" + publicPort + " to virtual machine " + userVM.toString());
        } catch (IllegalArgumentException argEx) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, argEx.getMessage());
        }

        if (firewallRule == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "The port forwarding rule from public port " + publicPort + " to private port " + privatePort + " for address " + ipAddress + " and virtual machine " + userVM.toString() + " already exists.");
        }

        List<Pair<String, Object>> groupsTags = new ArrayList<Pair<String, Object>>();
        Object[] forwardingTag = new Object[1];

        List<Pair<String, Object>> ruleData = new ArrayList<Pair<String, Object>>();

        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), firewallRule.getId().toString()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), firewallRule.getPublicPort()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), firewallRule.getPrivatePort()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.PROTOCOL.getName(), firewallRule.getProtocol()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_NAME.getName(), userVM.getName()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_DISPLAYNAME.getName(), userVM.getDisplayName()));
        ruleData.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName(), userVM.getId().toString()));

        forwardingTag[0] = ruleData;

        Pair<String, Object> eventTag = new Pair<String, Object>("portforwardingrule", forwardingTag);
        groupsTags.add(eventTag);
        return groupsTags;
    }
}
