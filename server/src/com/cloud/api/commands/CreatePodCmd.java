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
import com.cloud.dc.HostPodVO;
import com.cloud.test.PodZoneConfig;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class CreatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePodCmd.class.getName());

    private static final String s_name = "createpodresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.GATEWAY, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CIDR, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_IP, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.END_IP, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	String podName = (String) params.get(BaseCmd.Properties.NAME.getName());
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
    	String gateway = (String) params.get(BaseCmd.Properties.GATEWAY.getName());
    	String cidr = (String) params.get(BaseCmd.Properties.CIDR.getName());
    	String startIp = (String) params.get(BaseCmd.Properties.START_IP.getName());
    	String endIp = (String) params.get(BaseCmd.Properties.END_IP.getName());
    	Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
    	
    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
    	
    	//verify input parameters
    	DataCenterVO zone = getManagementServer().findDataCenterById(zoneId);
    	if (zone == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find zone by id " + zoneId);
    	}

    	if (endIp != null && startIp == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "If an end IP is specified, a start IP must be specified.");
    	}
    	
    	HostPodVO pod = null;
        try {
             pod = getManagementServer().createPod(userId, podName, zoneId, gateway, cidr, startIp, endIp);
        } catch (Exception ex) {
            s_logger.error("Exception creating pod", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        
        if (pod == null) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create pod; internal error.");
        } else {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), pod.getId()));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), podName));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zoneId));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), pod.getGateway()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CIDR.getName(), cidr));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.START_IP.getName(), startIp));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.END_IP.getName(), endIp != null ? endIp : ""));
        }
        
        return returnValues;
    }
}
