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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.capacity.CapacityVO;
import com.cloud.server.Criteria;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolVO;

@Implementation(method="listCapacities", description="Lists capacity.")
public class ListCapacityCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListCapacityCmd.class.getName());
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");

    private static final String s_name = "listcapacityresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="lists capacity by the Host ID")
    private Long hostId;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="lists capacity by the Pod ID")
    private Long podId;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="lists capacity by type")
    private String type;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists capacity by the Zone ID")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getPodId() {
        return podId;
    }

    public String getType() {
        return type;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public Long getPageSizeVal() {
        Long pageSizeVal = 1000000L;
        Integer pageSize = getPageSize();
        if (pageSize != null) {
            pageSizeVal = pageSize.longValue();
        }
        return pageSizeVal;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<CapacityResponse> getResponse() {
        List<CapacityVO> capacities = (List<CapacityVO>)getResponseObject();

        ListResponse<CapacityResponse> response = new ListResponse<CapacityResponse>();
        List<CapacityResponse> capacityResponses = new ArrayList<CapacityResponse>();
        List<CapacityVO> summedCapacities = sumCapacities(capacities);
        for (CapacityVO summedCapacity : summedCapacities) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityTotal(summedCapacity.getTotalCapacity());
            capacityResponse.setCapacityType(summedCapacity.getCapacityType());
            capacityResponse.setCapacityUsed(summedCapacity.getUsedCapacity());
            if (summedCapacity.getPodId() != null) {
                capacityResponse.setPodId(summedCapacity.getPodId());
                if (summedCapacity.getPodId() > 0) {
                    capacityResponse.setPodName(ApiDBUtils.findPodById(summedCapacity.getPodId()).getName());
                } else {
                	capacityResponse.setPodName("All");
                }
            }
            capacityResponse.setZoneId(summedCapacity.getDataCenterId());
            capacityResponse.setZoneName(ApiDBUtils.findZoneById(summedCapacity.getDataCenterId()).getName());
            if (summedCapacity.getTotalCapacity() != 0) {
            	float computed = ((float)summedCapacity.getUsedCapacity() / (float)summedCapacity.getTotalCapacity() * 100f);
                capacityResponse.setPercentUsed(s_percentFormat.format((float)summedCapacity.getUsedCapacity() / (float)summedCapacity.getTotalCapacity() * 100f));
            } else {
                capacityResponse.setPercentUsed(s_percentFormat.format(0L));
            }

            capacityResponse.setResponseName("capacity");
            capacityResponses.add(capacityResponse);
        }

        response.setResponses(capacityResponses);
        response.setResponseName(getName());
        return response;
    }

    private List<CapacityVO> sumCapacities(List<CapacityVO> hostCapacities) {	        
        Map<String, Long> totalCapacityMap = new HashMap<String, Long>();
        Map<String, Long> usedCapacityMap = new HashMap<String, Long>();
        
        Set<Long> poolIdsToIgnore = new HashSet<Long>();
        Criteria c = new Criteria();
        // TODO:  implement
        List<? extends StoragePoolVO> allStoragePools = ApiDBUtils.searchForStoragePools(c);
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
