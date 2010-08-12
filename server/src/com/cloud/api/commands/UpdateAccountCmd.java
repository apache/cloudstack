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

public class UpdateAccountCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateAccountCmd.class.getName());
    private static final String s_name = "updateaccountresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NEW_NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        String newAccountName = (String)params.get(BaseCmd.Properties.NEW_NAME.getName());
        Account adminAccount = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());;
        Boolean updateAccountResult = false;
        Account account = null;

        // check if account exists in the system
        account = getManagementServer().findAccountByName(accountName, domainId);
    	if (account == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find account " + accountName + " in domain " + domainId);
    	}

    	if ((adminAccount != null) && !getManagementServer().isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid account " + accountName + " in domain " + domainId + " given, unable to update account.");
    	}

    	// don't allow modify system account
    	if (account.getId().longValue() == Account.ACCOUNT_ID_SYSTEM) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "can not modify system account");
    	}

        try {
        	getManagementServer().updateAccount(account.getId(), newAccountName);
        	account = getManagementServer().findAccountById(account.getId());
        	if (account.getAccountName().equals(newAccountName)) {
        		updateAccountResult = true;
        	}
        } catch (Exception ex) {
            s_logger.error("Exception updating account", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update account " + accountName + " in domain " + domainId + ":  internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        if (updateAccountResult == true) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), new Boolean(true)));
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update account " + accountName + " in domain " + domainId);
        }
        return returnValues;
    }
}
