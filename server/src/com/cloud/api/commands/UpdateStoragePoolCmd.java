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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.ClusterVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.utils.Pair;

public class UpdateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateStoragePoolCmd.class.getName());

    private static final String s_name = "updatestoragepoolresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TAGS, Boolean.FALSE));
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
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("UpdateStoragePoolCmd Params @ " + params.toString());
        }
        
        Long poolId = (Long) params.get(BaseCmd.Properties.ID.getName());
        String tags = (String) params.get(BaseCmd.Properties.TAGS.getName());
        
        StoragePoolVO storagePool = null;
		try {
			storagePool = getManagementServer().updateStoragePool(poolId, tags);
		} catch (IllegalArgumentException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, e.getMessage());
		} 
 
        s_logger.debug("Successfully updated storagePool " + storagePool.toString() );
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(storagePool.getId())));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), storagePool.getDataCenterId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(storagePool.getDataCenterId()).getName()));
        if (storagePool.getPodId() != null) {
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), storagePool.getPodId()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().getPodBy(storagePool.getPodId()).getName()));
        }
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), storagePool.getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), storagePool.getHostAddress()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PATH.getName(), storagePool.getPath()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(storagePool.getCreated())));
        
        if (storagePool.getPoolType() != null) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), storagePool.getPoolType().toString()));
        }
        
        if (storagePool.getClusterId() != null) {
        	ClusterVO cluster = getManagementServer().findClusterById(storagePool.getClusterId());
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_ID.getName(), cluster.getId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_NAME.getName(), cluster.getName()));
        }
        
        StorageStats stats = getManagementServer().getStoragePoolStatistics(storagePool.getId());
        long capacity = storagePool.getCapacityBytes();
        long available = storagePool.getAvailableBytes() ;
        long used = capacity - available;

        if (stats != null) {
       	 used = stats.getByteUsed();
       	 available = capacity - used;
        }
        s_logger.debug("Successfully recieved the storagePool statistics. TotalDiskSize - " +capacity+ " AllocatedDiskSize - " +used );
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_TOTAL.getName(), Long.valueOf(storagePool.getCapacityBytes()).toString()));        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(used).toString()));        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), getManagementServer().getStoragePoolTags(storagePool.getId())));  

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        embeddedObject.add(new Pair<String, Object>("storagepool", new Object[] { returnValues } ));
        return embeddedObject;
    }
}
