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
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.server.Criteria;
import com.cloud.utils.Pair;

public class ListClustersCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listclustersresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
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
    	Long id = (Long) params.get(BaseCmd.Properties.ID.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());    	
    	Long podId = (Long) params.get(BaseCmd.Properties.POD_ID.getName());
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
        Integer page = (Integer) params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer) params.get(BaseCmd.Properties.PAGESIZE.getName());
        
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
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.DATACENTERID, zoneId);

        List<ClusterVO> clusters = getManagementServer().searchForClusters(c);
        
        if (clusters == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find clusters");
        }
        
        List<Pair<String, Object>> clusterTags = new ArrayList<Pair<String, Object>>();
        Object[] cTag = new Object[clusters.size()];
        int i = 0;
        for (ClusterVO cluster : clusters) {
            List<Pair<String, Object>> clusterData = new ArrayList<Pair<String, Object>>();
            
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), cluster.getId()));
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), cluster.getName()));
            
            HostPodVO pod = getManagementServer().findHostPodById(cluster.getPodId());
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), pod.getId()));
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), pod.getName()));
            
            DataCenterVO zone = getManagementServer().findDataCenterById(cluster.getDataCenterId());
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zone.getId()));
            clusterData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));            
            
            cTag[i++] = clusterData;
        }
        
        Pair<String, Object> clusterTag = new Pair<String, Object>("cluster", cTag);
        clusterTags.add(clusterTag);
        
        return clusterTags;
    }
}
