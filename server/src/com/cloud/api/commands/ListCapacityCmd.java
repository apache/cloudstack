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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.capacity.CapacityVO;
import com.cloud.server.Criteria;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;

public class ListCapacityCmd extends BaseCmd{

    public static final Logger s_logger = Logger.getLogger(ListCapacityCmd.class.getName());
    private static final DecimalFormat s_percentFormat = new DecimalFormat("####.##");

    private static final String s_name = "listcapacityresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.HOST_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
        Long hostId = (Long)params.get(BaseCmd.Properties.HOST_ID.getName());
        String type = (String)params.get(BaseCmd.Properties.TYPE.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 1000000;
        if (pageSize != null) {
            pageSizeNum = pageSize.intValue();
        }
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }

        Criteria c = new Criteria ("capacityType", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.HOSTID, hostId);
        c.addCriteria(Criteria.TYPE, type);

        List<CapacityVO> capacities = getManagementServer().listCapacities(c);

        if (capacities == null ) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to get capacity statistic");
        }

        List<CapacityVO> summedCapacities = sumCapacities(capacities);
        List<Pair<String, Object>> capacitiesTags = new ArrayList<Pair<String, Object>>();
        Object[] cTag = new Object[summedCapacities.size()];
        int i=0;

        for (CapacityVO capacity : summedCapacities) {
            List<Pair<String, Object>> capacityData = new ArrayList<Pair<String, Object>>();
            capacityData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), capacity.getCapacityType()));
            capacityData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), capacity.getDataCenterId()));
            capacityData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(capacity.getDataCenterId()).getName()));
            if (capacity.getPodId() != null) {
                capacityData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), capacity.getPodId()));
                capacityData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), (capacity.getPodId() > 0) ? getManagementServer().findHostPodById(capacity.getPodId()).getName() : "All"));
            }
            capacityData.add(new Pair<String, Object>(BaseCmd.Properties.CAPACITY_USED.getName(), Long.valueOf(capacity.getUsedCapacity()).toString()));
            capacityData.add(new Pair<String, Object>(BaseCmd.Properties.CAPACITY_TOTAL.getName(), Long.valueOf(capacity.getTotalCapacity()).toString()));
            try {  
                if (capacity.getTotalCapacity() != 0) {
                    float percent = (float)capacity.getUsedCapacity()/(float)capacity.getTotalCapacity()*100;
                    capacityData.add(new Pair<String, Object>(BaseCmd.Properties.PERCENT_USED.getName(), s_percentFormat.format(percent)));
                }
                else {
                    capacityData.add(new Pair<String, Object>(BaseCmd.Properties.PERCENT_USED.getName(), s_percentFormat.format(0)));
                }
            } catch (Exception  ex) {
                throw new ServerApiException (BaseCmd.INTERNAL_ERROR, "unable to get capacity statistic");
            }
            cTag[i++] = capacityData;
        }

        Pair<String, Object> capacityTag = new Pair<String, Object>("capacity", cTag);
        capacitiesTags.add(capacityTag);
        return capacitiesTags;
    }

    public List<CapacityVO> sumCapacities(List<CapacityVO> hostCapacities) {	        
        Map<String, Long> totalCapacityMap = new HashMap<String, Long>();
        Map<String, Long> usedCapacityMap = new HashMap<String, Long>();
        
        Set<Long> poolIdsToIgnore = new HashSet<Long>();
        Criteria c = new Criteria();
        List<? extends StoragePoolVO> allStoragePools = getManagementServer().searchForStoragePools(c);
        for (StoragePoolVO pool : allStoragePools) {
        	StoragePoolType poolType = pool.getPoolType();
        	if (!(poolType.equals(StoragePoolType.NetworkFilesystem) || poolType.equals(StoragePoolType.IscsiLUN))) {
        		poolIdsToIgnore.add(pool.getId());
        	}
        }
        
        // collect all the capacity types, sum allocated/used and sum total...get one capacity number for each
        for (CapacityVO capacity : hostCapacities) {
        	if (poolIdsToIgnore.contains(capacity.getHostOrPoolId())) {
        		continue;
        	}
        	
            String key = capacity.getCapacityType() + "_" + capacity.getDataCenterId();
            String keyForPodTotal = key + "_-1";
            
            boolean sumPodCapacity = false;
            if (capacity.getPodId() != null) {
                key += "_" + capacity.getPodId();
                sumPodCapacity = true;
            }

            Long totalCapacity = totalCapacityMap.get(key);
            Long usedCapacity = usedCapacityMap.get(key);

            if (totalCapacity == null) {
                totalCapacity = new Long(capacity.getTotalCapacity());
            } else {
                totalCapacity = new Long(capacity.getTotalCapacity() + totalCapacity.longValue());
            }

            if (usedCapacity == null) {
                usedCapacity = new Long(capacity.getUsedCapacity());
            } else {
                usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity.longValue());
            }

            totalCapacityMap.put(key, totalCapacity);
            usedCapacityMap.put(key, usedCapacity);
            
            if (sumPodCapacity) {
            	totalCapacity = totalCapacityMap.get(keyForPodTotal);
                usedCapacity = usedCapacityMap.get(keyForPodTotal);

                if (totalCapacity == null) {
                    totalCapacity = new Long(capacity.getTotalCapacity());
                } else {
                    totalCapacity = new Long(capacity.getTotalCapacity() + totalCapacity.longValue());
                }

                if (usedCapacity == null) {
                    usedCapacity = new Long(capacity.getUsedCapacity());
                } else {
                    usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity.longValue());
                }

                totalCapacityMap.put(keyForPodTotal, totalCapacity);
                usedCapacityMap.put(keyForPodTotal, usedCapacity);
            }
        }

        List<CapacityVO> summedCapacities = new ArrayList<CapacityVO>();
        for (String key : totalCapacityMap.keySet()) {
            CapacityVO summedCapacity = new CapacityVO();

            StringTokenizer st = new StringTokenizer(key, "_");
            summedCapacity.setCapacityType(Short.parseShort(st.nextToken()));
            summedCapacity.setDataCenterId(Long.parseLong(st.nextToken()));
            if (st.hasMoreTokens()) {
                summedCapacity.setPodId(Long.parseLong(st.nextToken()));
            }

            summedCapacity.setTotalCapacity(totalCapacityMap.get(key));
            summedCapacity.setUsedCapacity(usedCapacityMap.get(key));

            summedCapacities.add(summedCapacity);
        }
        return summedCapacities;
    }	   
}
