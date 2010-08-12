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
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class LockAccountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(LockAccountCmd.class.getName());

    private static final String s_name = "lockaccountresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account adminAccount = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());

        if ((adminAccount != null) && !getManagementServer().isChildDomain(adminAccount.getDomainId(), domainId)) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to lock account " + accountName + " in domain " + domainId + ", permission denied.");
        }

        Account account = getManagementServer().findActiveAccount(accountName, domainId);
        if (account == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find active account with name " + accountName + " in domain " + domainId);
        }

        // don't allow modify system account
        if (account.getId().longValue() == Account.ACCOUNT_ID_SYSTEM) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "can not lock system account");
        }

        boolean success = getManagementServer().lockAccount(account.getId().longValue());
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.valueOf(success).toString()));
        return returnValues;
    }
}
