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
import com.cloud.dc.HostPodVO;
import com.cloud.server.Criteria;
import com.cloud.test.PodZoneConfig;
import com.cloud.utils.Pair;

public class ListPodsByCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListPodsByCmd.class.getName());

    private static final String s_name = "listpodsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
    	String name = (String)params.get(BaseCmd.Properties.NAME.getName());
    	Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
    	String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
    	
    	Criteria c = new Criteria();
    	
    	if (keyword != null) {
    		c.addCriteria(Criteria.KEYWORD, keyword);
     	} else {
     		c.addCriteria(Criteria.ID, id);
            c.addCriteria(Criteria.NAME, name);
            c.addCriteria(Criteria.DATACENTERID, zoneId);
     	}
        
    	List<HostPodVO> pods = getManagementServer().searchForPods(c);
        
        if (pods == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find pods for specified search criteria.");
        }
        
        List<Pair<String, Object>> podTags = new ArrayList<Pair<String, Object>>();
        Object[] podDataTags = new Object[pods.size()];
        
        int i = 0;
        for (HostPodVO pod : pods) {
        	String[] ipRange = new String[2];
        	if (pod.getDescription() != null && pod.getDescription().length() > 0) {
        		ipRange = pod.getDescription().split("-");
        	} else {
        		ipRange[0] = pod.getDescription();
        	}
            List<Pair<String, Object>> podData = new ArrayList<Pair<String, Object>>();
            podData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), pod.getId()));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), pod.getName()));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), pod.getDataCenterId()));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), PodZoneConfig.getZoneName(pod.getDataCenterId())));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.CIDR.getName(), pod.getCidrAddress() +"/" + pod.getCidrSize()));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.START_IP.getName(), ipRange[0]));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.END_IP.getName(), (ipRange.length > 1 && ipRange[1] != null) ? ipRange[1] : ""));
            podData.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), pod.getGateway()));
            podDataTags[i++] = podData;
        }
        Pair<String, Object> podTag = new Pair<String, Object>("pod", podDataTags);
        podTags.add(podTag);
        return podTags;
    }
}
