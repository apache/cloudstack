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
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DisassociateIPAddrCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DisassociateIPAddrCmd.class.getName());

    private static final String s_name = "disassociateipaddressresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
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
        boolean result = false;

        // Verify input parameters
        Account accountByIp = getManagementServer().findAccountByIpAddress(ipAddress);
        if(accountByIp == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account owner for ip " + ipAddress);
        }

        Long accountId = accountByIp.getId();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId().longValue() != accountId.longValue()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " doesn't own ip address " + ipAddress);
                }
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), accountByIp.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to disassociate IP address " + ipAddress + ", permission denied.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        try {
            result = getManagementServer().disassociateIpAddress(userId.longValue(), accountId.longValue(), ipAddress);
        } catch (PermissionDeniedException ex) {
            throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex1) {
        	throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, ex1.getMessage());
        } catch (Exception ex2) {
        	throw new ServerApiException(BaseCmd.NET_IP_DIASSOC_ERROR, "unable to disassociate ip address");
        }
        
        if (result == false) {
            throw new ServerApiException(BaseCmd.NET_IP_DIASSOC_ERROR, "unable to disassociate ip address");
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.valueOf(result).toString()));
        return returnValues;
    }
}
