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
import com.cloud.server.Criteria;
import com.cloud.storage.StoragePoolDetailVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.utils.Pair;

public class ListStoragePoolsCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(ListStoragePoolsCmd.class.getName());

    private static final String s_name = "liststoragepoolsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PATH, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @SuppressWarnings("unchecked")
	@Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
        String ipAddress = (String)params.get(BaseCmd.Properties.IP_ADDRESS.getName());
        String path = (String)params.get(BaseCmd.Properties.PATH.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
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
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.ADDRESS, ipAddress);
        c.addCriteria(Criteria.PATH, path);

        List<? extends StoragePoolVO> pools = getManagementServer().searchForStoragePools(c);

        if (pools == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find pools");
        }

        List<Pair<String, Object>> poolTags = new ArrayList<Pair<String, Object>>();
        Object[] sTag = new Object[pools.size()];
        int i = 0;
        for (StoragePoolVO pool : pools) {
            StoragePoolVO netfsPool = pool;
            List<Pair<String, Object>> poolData = new ArrayList<Pair<String, Object>>();
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(pool.getId())));
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), pool.getName()));

            if (pool.getPoolType() != null) {
                poolData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), pool.getPoolType().toString()));
            }
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), netfsPool.getHostAddress()));
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(pool.getDataCenterId()).toString()));
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(pool.getDataCenterId()).getName()));
            if (pool.getPodId() != null) {
                poolData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), Long.valueOf(pool.getPodId()).toString()));
                poolData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().getPodBy(pool.getPodId()).getName()));
            }

            poolData.add(new Pair<String, Object>(BaseCmd.Properties.PATH.getName(), netfsPool.getPath().toString()));

            StorageStats stats = getManagementServer().getStoragePoolStatistics(pool.getId());
            long capacity = pool.getCapacityBytes();
            long available = pool.getAvailableBytes() ;
            long used = capacity - available;

            if (stats != null) {
                used = stats.getByteUsed();
                available = capacity - used;
            }

            poolData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_TOTAL.getName(), Long.valueOf(pool.getCapacityBytes()).toString()));
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(used).toString()));

            if (pool.getCreated() != null) {
                poolData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(pool.getCreated())));
            }
         
            if (pool.getClusterId() != null) {
            	ClusterVO cluster = getManagementServer().findClusterById(pool.getClusterId());
            	poolData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_ID.getName(), cluster.getId()));
            	poolData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_NAME.getName(), cluster.getName()));
            }           
            
            poolData.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), getManagementServer().getStoragePoolTags(pool.getId())));

            sTag[i++] = poolData;
        }
        Pair<String, Object> poolTag = new Pair<String, Object>("storagepool", sTag);
        poolTags.add(poolTag);
        return poolTags;
    }
}
