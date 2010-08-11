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
import com.cloud.utils.Pair;

public class CreateDomainCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDomainCmd.class.getName());

    private static final String s_name = "createdomainresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PARENT_DOMAIN_ID, Boolean.FALSE));
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
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long parentDomainId = (Long)params.get(BaseCmd.Properties.PARENT_DOMAIN_ID.getName());

        // If account is null, consider System as an owner for this action
        if (account == null) {
            account = getManagementServer().findAccountById(Long.valueOf(1L));
        }

        if (parentDomainId == null){
        	parentDomainId = DomainVO.ROOT_DOMAIN;
        } else {
        	DomainVO parentDomain = null;
            parentDomain = getManagementServer().findDomainIdById(parentDomainId);
        	if (parentDomain == null) {
        		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find parent domain " + parentDomainId);
        	}
        }

        if (!getManagementServer().isChildDomain(account.getDomainId(), parentDomainId)) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Invalid parent domain " + parentDomainId + ", unable to create domain " + name);
        }

        DomainVO domain = null;
        try {
            domain = getManagementServer().createDomain(name, account.getId(), parentDomainId);
        } catch (IllegalArgumentException illArgEx) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Failed to create domain " + name + " due to invalid name given.");
            }
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create domain " + name + ", invalid name given.  The character '/' is not valid for domain names.");
        } catch (Exception ex) {
            s_logger.error("Exception creating domain", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create domain " + name + ":  internal error.");
        }

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        if (domain == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create domain " + name + ":  a domain with that name already exists.");
        } else {
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), domain.getId()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), domain.getName()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.LEVEL.getName(), domain.getLevel().toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PARENT_DOMAIN_ID.getName(), domain.getParent().toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PARENT_DOMAIN_NAME.getName(), 
        			getManagementServer().findDomainIdById(domain.getParent()).getName()));
            embeddedObject.add(new Pair<String, Object>("domain", new Object[] { returnValues } ));
        }
        return embeddedObject;
    }
}
