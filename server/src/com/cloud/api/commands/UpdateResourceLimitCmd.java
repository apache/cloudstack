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
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.domain.DomainVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class UpdateResourceLimitCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateResourceLimitCmd.class.getName());

    private static final String s_name = "updateresourcelimitresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.RESOURCE_TYPE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.MAX, Boolean.FALSE));
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
        Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String accountName = (String) params.get(BaseCmd.Properties.ACCOUNT.getName());
        Integer type = (Integer) params.get(BaseCmd.Properties.RESOURCE_TYPE.getName());
        Long max = (Long) params.get(BaseCmd.Properties.MAX.getName());
        Long accountId = null;

        if (max == null) {
        	max = new Long(-1);
        } else if (max < -1) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }
        
        // Map resource type
        ResourceType resourceType;
        try {
        	resourceType = ResourceType.values()[type];
        } catch (ArrayIndexOutOfBoundsException e) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid resource type.");
        }               
        
        if (accountName==null && domainId != null && !domainId.equals(DomainVO.ROOT_DOMAIN)) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Resource limits must be made for an account or the ROOT domain.");
        }
        
        if (account != null) {
            if (domainId != null) {
                if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update resource limit for " + ((accountName == null) ? "" : "account " + accountName + " in ") + "domain " + domainId + ", permission denied");
                }
            } else if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                domainId = DomainVO.ROOT_DOMAIN; // for root admin, default to root domain if domain is not specified
            }                 
            
            if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            	// If there is an existing ROOT domain limit, make sure its max isn't being exceeded
            	Criteria c = new Criteria();
             	c.addCriteria(Criteria.DOMAINID, DomainVO.ROOT_DOMAIN);
             	c.addCriteria(Criteria.TYPE, resourceType);
            	List<ResourceLimitVO> currentRootDomainLimits = getManagementServer().searchForLimits(c);
            	ResourceLimitVO currentRootDomainLimit = (currentRootDomainLimits.size() == 0) ? null : currentRootDomainLimits.get(0);
            	if (currentRootDomainLimit != null) {
            		long currentRootDomainMax = currentRootDomainLimits.get(0).getMax();
            		if ((max == -1 && currentRootDomainMax != -1) || max > currentRootDomainMax) {
            			throw new ServerApiException(BaseCmd.PARAM_ERROR, "The current ROOT domain limit for resource type " + resourceType + " is " + currentRootDomainMax + " and cannot be exceeded.");
            		}
            	}
            }
        } else if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN; // for system commands, default to root domain if domain is not specified
        }

        if (domainId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update resource limit, unable to determine domain in which to update limit.");
        } else if (accountName != null) {
            Account userAccount = getManagementServer().findActiveAccount(accountName, domainId);
            if (userAccount == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find account by name " + accountName + " in domain with id " + domainId);
            }
            accountId = userAccount.getId();
            domainId = userAccount.getDomainId();
        }               

        ResourceLimitVO limit = null;
        try {
        	if (accountId != null) domainId = null;
            limit = getManagementServer().updateResourceLimit(domainId, accountId, resourceType, max);
        } catch (InvalidParameterValueException paramException) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, paramException.getMessage());
        } catch (Exception ex) {
            s_logger.error("Exception updating resource limit", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update limit due to exception: " + ex.getMessage());
        }
        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        
        if (limit == null)
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update resource limit. Please contact Cloud Support.");
        else {
        	
        	if (limit.getDomainId() != null) {
        		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), limit.getDomainId()));
            	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(limit.getDomainId()).getName()));
        	}
        	
        	if (limit.getAccountId() != null) {
        		Account accountTemp = getManagementServer().findAccountById(limit.getAccountId());
                if (accountTemp != null) {
                	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
                	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
                	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
                }
        	}
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.RESOURCE_TYPE.getName(), limit.getType().ordinal()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.MAX.getName(), limit.getMax()));
        	embeddedObject.add(new Pair<String, Object>("resourcelimit", new Object[] { returnValues } ));
        }
        return embeddedObject;
    }
}
