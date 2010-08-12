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
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class CreateServiceOfferingCmd extends BaseCmd{
	public static final Logger s_logger = Logger.getLogger(CreateServiceOfferingCmd.class.getName());
	private static final String _name = "createserviceofferingresponse";
	private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
	
	static {
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_TEXT, Boolean.TRUE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CPU_NUMBER, Boolean.TRUE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CPU_SPEED, Boolean.TRUE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.MEMORY, Boolean.TRUE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STORAGE_TYPE, Boolean.FALSE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OFFER_HA, Boolean.FALSE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USE_VIRTUAL_NETWORK, Boolean.FALSE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
		s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TAGS, Boolean.FALSE));
	}
	
	@Override
    public String getName() {
		return _name;
	}
	
	@Override
    public List<Pair<Enum, Boolean>> getProperties (){
		return s_properties;
	}
	
	
	@Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
	    // FIXME: add domain-private service offerings
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
		String name = (String)params.get(BaseCmd.Properties.NAME.getName());
		String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
		Long cpuNumber = (Long)params.get(BaseCmd.Properties.CPU_NUMBER.getName());
		Long cpuSpeed = (Long)params.get(BaseCmd.Properties.CPU_SPEED.getName());
		Long memory = (Long)params.get(BaseCmd.Properties.MEMORY.getName());
		String storageType = (String) params.get(BaseCmd.Properties.STORAGE_TYPE.getName());
		Boolean offerHA = (Boolean) params.get(BaseCmd.Properties.OFFER_HA.getName());
		Boolean useVirtualNetwork = (Boolean) params.get(BaseCmd.Properties.USE_VIRTUAL_NETWORK.getName());
		Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
		String tags = (String)params.get(BaseCmd.Properties.TAGS.getName());

		if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
		
		if (name.length() == 0) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering: specify the name that has non-zero length");
		}

		if (displayText.length() == 0) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering: specify the display text that has non-zero length");
		}

		if ((cpuNumber.intValue() <= 0) || (cpuNumber.intValue() > 2147483647)) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering: specify the cpu number value between 1 and 2147483647");
		}

		if ((cpuSpeed.intValue() <= 0) || (cpuSpeed.intValue() > 2147483647)) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering: specify the cpu speed value between 1 and 2147483647");
		}

		if ((memory.intValue() <= 0) || (memory.intValue() > 2147483647)) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering: specify the memory value between 1 and 2147483647");
		}
		
		boolean localStorageRequired;
		if (storageType == null) {
			localStorageRequired = false;
		} else if (storageType.equals("local")) {
			localStorageRequired = true;
		} else if (storageType.equals("shared")) {
			localStorageRequired = false;
		} else {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Valid pool types are: 'local' and 'shared'");
		}

		if (offerHA == null) {
			offerHA = false;
		}
		
		if (useVirtualNetwork == null) {
			useVirtualNetwork = Boolean.TRUE;
		}
		
		ServiceOfferingVO offering = null;
		try {
			offering = getManagementServer().createServiceOffering(userId, name, cpuNumber.intValue(), memory.intValue(), cpuSpeed.intValue(), displayText, localStorageRequired, offerHA, useVirtualNetwork, tags);
		} catch (Exception ex) {
			s_logger.error("Exception creating service offering", ex);
	        throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create service offering " + name + ":  internal error.");
		}

		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        if (offering == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create service offering " + name);
        } else {
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), offering.getId()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), offering.getName()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), offering.getDisplayText()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), Integer.valueOf(offering.getCpu()).toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), Integer.valueOf(offering.getSpeed()).toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY.getName(), Integer.valueOf(offering.getRamSize()).toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(offering.getCreated())));
            storageType = offering.getUseLocalStorage() ? "local" : "shared";
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.STORAGE_TYPE.getName(), storageType));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.OFFER_HA.getName(), offering.getOfferHA()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.USE_VIRTUAL_NETWORK.getName(), (offering.getGuestIpType().equals(GuestIpType.Virtualized))));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), offering.getTags()));
        }
        return returnValues;
	}
}
