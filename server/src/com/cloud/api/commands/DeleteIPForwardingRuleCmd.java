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
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class DeleteIPForwardingRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIPForwardingRuleCmd.class.getName());

    private static final String s_name = "deleteportforwardingruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
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
        Long ruleId = (Long)params.get(BaseCmd.Properties.ID.getName());

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        FirewallRuleVO fwRule = getManagementServer().findForwardingRuleById(ruleId);
        if (fwRule == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find port forwarding rule " + ruleId);
        }

        IPAddressVO ipAddress = getManagementServer().findIPAddressById(fwRule.getPublicIpAddress());
        if (ipAddress == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find IP address for port forwarding rule " + ruleId);
        }

        Account ruleOwner = getManagementServer().findAccountById(ipAddress.getAccountId());
        if (ruleOwner == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find owning account for port forwarding rule " + ruleId);
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!getManagementServer().isChildDomain(account.getDomainId(), ruleOwner.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete port forwarding rule " + ruleId + ", permission denied.");
                }
            } else if (account.getId().longValue() != ruleOwner.getId().longValue()) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete port forwarding rule " + ruleId + ", permission denied.");
            }
        }

        try {
            getManagementServer().deleteRule(ruleId.longValue(), userId.longValue(), ruleOwner.getId().longValue());
        } catch (InvalidParameterValueException ex1) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete port forwarding rule " + ruleId + ", internal error.");
        } catch (PermissionDeniedException ex2) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete port forwarding rule " + ruleId + ", permission denied.");
        } catch (InternalErrorException ex3) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to delete port forwarding rule " + ruleId + ", internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
        return returnValues;
    }
}
