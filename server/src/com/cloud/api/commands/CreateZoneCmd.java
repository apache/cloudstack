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
import com.cloud.dc.DataCenterVO;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class CreateZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateZoneCmd.class.getName());

    private static final String s_name = "createzoneresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DNS1, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DNS2, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.INTERNAL_DNS1, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.INTERNAL_DNS2, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VNET, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.GUEST_CIDR_ADDRESS, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
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
    	String zoneName = (String) params.get(BaseCmd.Properties.NAME.getName());
    	String dns1 = (String) params.get(BaseCmd.Properties.DNS1.getName());
    	String dns2 = (String) params.get(BaseCmd.Properties.DNS2.getName());
    	String dns3 = (String) params.get(BaseCmd.Properties.INTERNAL_DNS1.getName());
    	String dns4 = (String) params.get(BaseCmd.Properties.INTERNAL_DNS2.getName());
    	String vnet = (String) params.get(BaseCmd.Properties.VNET.getName());
    	String guestCidr = (String) params.get(BaseCmd.Properties.GUEST_CIDR_ADDRESS.getName());
    	Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());

    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
    	
    	DataCenterVO zone = null;
    	
        try {
             zone = getManagementServer().createZone(userId, zoneName, dns1, dns2, dns3, dns4, vnet, guestCidr);
        } catch (Exception ex) {
            s_logger.error("Exception creating zone", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        
        if (zone == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create zone " + zoneName + ":  internal error.");
        } else {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), zone.getId()));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), zoneName));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DNS1.getName(), dns1));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DNS2.getName(), dns2));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.INTERNAL_DNS1.getName(), dns3));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.INTERNAL_DNS2.getName(), dns4));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VNET.getName(), vnet));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.GUEST_CIDR_ADDRESS.getName(), guestCidr));
        }
        
        return returnValues;
    }
}
