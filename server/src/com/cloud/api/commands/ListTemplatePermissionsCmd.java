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
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListTemplatePermissionsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatePermissionsCmd.class.getName());

    private static final String s_name = "listtemplatepermissionsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
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
        Long templateId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String acctName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long accountId = null;

        if ((account == null) || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list template permissions.");
                }
                if (acctName != null) {
                    Account userAccount = getManagementServer().findAccountByName(acctName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + acctName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        VMTemplateVO template = getManagementServer().findTemplateById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find template with id " + templateId);
        }

        if (accountId != null && !template.isPublicTemplate()) {
        	if (account.getType() == Account.ACCOUNT_TYPE_NORMAL && template.getAccountId() != accountId) {
        		throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to list permissions for template with id " + templateId);
        	} else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
        		DomainVO accountDomain = getManagementServer().findDomainIdById(account.getDomainId());
        		Account templateAccount = getManagementServer().findAccountById(template.getAccountId());
        		DomainVO templateDomain = getManagementServer().findDomainIdById(templateAccount.getDomainId());        			
            	if (!templateDomain.getPath().contains(accountDomain.getPath())) {
            		throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to list permissions for template with id " + templateId);
            	}
        	}                                    
        }

        if (templateId == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to list permissions fo template with id " + templateId);
        }

        List<String> accountNames = getManagementServer().listTemplatePermissions(templateId);

        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        Long templateOwnerDomain = null;
        if (isAdmin) {
            Account templateOwner = getManagementServer().findAccountById(template.getAccountId());
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), template.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(template.isPublicTemplate()).toString()));
        if (isAdmin && (templateOwnerDomain != null)) {
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), templateOwnerDomain.toString()));
        }
        if ((accountNames != null) && !accountNames.isEmpty()) {
            for (String accountName : accountNames) {
                returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountName));
            }
        }
        embeddedObject.add(new Pair<String, Object>("templatepermission", new Object[] { returnValues } ));
        return embeddedObject;
    }
}
