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

import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.StoragePoolVO;

@SuppressWarnings("rawtypes")
@Implementation(method="createPool", manager=Manager.StorageManager)
public class CreateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStoragePoolCmd.class.getName());

    private static final String s_name = "createstoragepoolresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="clusterid", type=CommandType.LONG)
    private Long clusterId;

    @Parameter(name="details", type=CommandType.MAP)
    private Map details;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String storagePoolName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    @Parameter(name="url", type=CommandType.STRING, required=true)
    private String url;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public Map getDetails() {
        return details;
    }

    public String getStoragePoolName() {
        return storagePoolName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getTags() {
        return tags;
    }

    public String getUrl() {
        return url;
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
    public String getResponse() {
        StoragePoolVO pool = (StoragePoolVO)getResponseObject();

        StoragePoolResponse response = new StoragePoolResponse();
        response.setClusterId(pool.getClusterId());
        // TODO: Implement
        //response.setClusterName(pool.getClusterName());
        //response.setDiskSizeAllocated(pool.getDiskSizeAllocated());
        //response.setDiskSizeTotal(pool.getDiskSizeTotal());
        //response.setPodName(pool.getPodName());
        //response.setTags(pool.getTags());
        response.setCreated(pool.getCreated());
        response.setId(pool.getId());
        response.setIpAddress(pool.getHostAddress());
        response.setName(pool.getName());
        response.setPath(pool.getPath());
        response.setPodId(pool.getPodId());
        response.setType(pool.getPoolType().toString());

        return SerializerHelper.toSerializedString(response);
    }
}
