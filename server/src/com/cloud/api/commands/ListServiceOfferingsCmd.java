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
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class ListServiceOfferingsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listserviceofferingsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	String name = (String)params.get(BaseCmd.Properties.NAME.getName());
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
    	Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
    	String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        
        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        Criteria c = new Criteria("created", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));
        if (keyword != null) {
        	c.addCriteria(Criteria.KEYWORD, keyword);
        } else {
        	c.addCriteria(Criteria.ID, id);
            c.addCriteria(Criteria.NAME, name);
        }
        
        //If vmId is present in the list of parameters, verify it
        if (vmId != null) {
        	UserVmVO vmInstance = getManagementServer().findUserVMInstanceById(vmId.longValue());
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
            	throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
            }
        	if ((account != null) && !isAdmin(account.getType())) {
                if (account.getId().longValue() != vmInstance.getAccountId()) {
                    throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
                }
            }
        	if (keyword == null)
            	c.addCriteria(Criteria.INSTANCEID, vmId);
        }

        List<ServiceOfferingVO> offerings = getManagementServer().searchForServiceOfferings(c);
        if (offerings == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find service offerings");
        }
        List<Pair<String, Object>> offeringTags = new ArrayList<Pair<String, Object>>();
        Object[] soTag = new Object[offerings.size()];
        int i = 0;
        for (ServiceOfferingVO offering : offerings) 
        {
            List<Pair<String, Object>> offeringData = new ArrayList<Pair<String, Object>>();

            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), offering.getId().toString()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), offering.getName()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), offering.getDisplayText()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), Integer.valueOf(offering.getCpu()).toString()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), Integer.valueOf(offering.getSpeed()).toString()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY.getName(), Integer.valueOf(offering.getRamSize()).toString()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(offering.getCreated())));
            String storageType = offering.getUseLocalStorage() ? "local" : "shared";
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.STORAGE_TYPE.getName(), storageType));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.OFFER_HA.getName(), offering.getOfferHA()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.USE_VIRTUAL_NETWORK.getName(), (offering.getGuestIpType().equals(GuestIpType.Virtualized))));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), (offering.getTags())));      

            soTag[i++] = offeringData;
        }
        Pair<String, Object> offeringTag = new Pair<String, Object>("serviceoffering", soTag);
        offeringTags.add(offeringTag);
        return offeringTags;
    }
}
