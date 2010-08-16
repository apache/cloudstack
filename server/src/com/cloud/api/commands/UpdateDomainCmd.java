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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="updateDomain", manager=Manager.ManagementServer)
public class UpdateDomainCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateDomainCmd.class.getName());
    private static final String s_name = "updatedomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String domainName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getDomainName() {
        return domainName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long domainId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        String newName = (String)params.get(BaseCmd.Properties.NAME.getName());
//        Boolean editDomainResult = false;
//
//        //check if domain exists in the system
//    	DomainVO domain = getManagementServer().findDomainIdById(domainId);
//    	if (domain == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find domain " + domainId);
//    	} else if (domain.getParent() == null) {
//            //check if domain is ROOT domain - and deny to edit it
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "ROOT domain can not be edited");
//    	}
//
//    	// check permissions
//    	if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domain.getId())) {
//            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update domain " + domainId + ", permission denied");
//    	}
//
//    	if (newName == null) {
//    		newName = domain.getName();
//    	}
//
//        try {     
//        	getManagementServer().updateDomain(domainId, newName);
//        	domain = getManagementServer().findDomainIdById(domainId);
//        	if (domain.getName().equals(newName)) {
//        		editDomainResult = true;
//        	}
//        } catch (Exception ex) {
//            s_logger.error("Exception editing domain", ex);
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update domain " + domainId + ":  internal error.");
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        if (editDomainResult == true) {
//        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), new Boolean(true)));
//        } else {
//        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update domain " + domainId);
//        }
//        return returnValues;
//    }
}
