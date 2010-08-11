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
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

public class CreateUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateUserCmd.class.getName());

    private static final String s_name = "createuserresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USERNAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PASSWORD, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.FIRSTNAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.LASTNAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.EMAIL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_TYPE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TIMEZONE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        String username = (String)params.get(BaseCmd.Properties.USERNAME.getName());
        String password = (String)params.get(BaseCmd.Properties.PASSWORD.getName());
        String firstname = (String)params.get(BaseCmd.Properties.FIRSTNAME.getName());
        String lastname = (String)params.get(BaseCmd.Properties.LASTNAME.getName());
        String email = (String)params.get(BaseCmd.Properties.EMAIL.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long accountType = (Long)params.get(BaseCmd.Properties.ACCOUNT_TYPE.getName());
        String timezone = (String)params.get(BaseCmd.Properties.TIMEZONE.getName());

        // Check the domainId
        if (domainId == null) {
        	domainId = DomainVO.ROOT_DOMAIN;
        }

        //Verify if the account exists 
        if (accountName != null) {
        	Account account = getManagementServer().findActiveAccount(accountName, domainId);
            if (account !=null) {
            	accountType = Long.valueOf((long)account.getType());
            }
        }
        else {
        	accountName = username;
        }

        User createdUser = null;
        try {
            createdUser = getManagementServer().createUserAPI(username, password, firstname, lastname, domainId, accountName, accountType.shortValue(), email, timezone);
        } catch (CloudRuntimeException ex) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("exception creating user: " + ex);
            }
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, ex.getMessage());
        }

        if (createdUser == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "failed to create user");
        } else {
        	Account userAccount = getManagementServer().findAccountById(Long.valueOf(createdUser.getAccountId()));
            if (userAccount != null) {
                domainId = userAccount.getDomainId();
                accountName = userAccount.getAccountName();
            }
        }
        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), createdUser.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.USERNAME.getName(), createdUser.getUsername()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.FIRSTNAME.getName(), createdUser.getFirstname()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.LASTNAME.getName(), createdUser.getLastname()));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.EMAIL.getName(), createdUser.getEmail())); 
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(createdUser.getCreated())));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), createdUser.getState()));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountName)); 
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT_TYPE.getName(), accountType)); 
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), domainId.toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(domainId).getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TIMEZONE.getName(),createdUser.getTimezone()));
        embeddedObject.add(new Pair<String, Object>("user", new Object[] { returnValues } ));
        return embeddedObject;
    }
}