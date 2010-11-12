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

import java.net.UnknownHostException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.storage.StoragePoolVO;

@SuppressWarnings("rawtypes")
@Implementation(description="Creates a storage pool.", responseObject=StoragePoolResponse.class)
public class CreateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStoragePoolCmd.class.getName());

    private static final String s_name = "createstoragepoolresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, description="the cluster ID for the storage pool")
    private Long clusterId;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.MAP, description="the details for the storage pool")
    private Map details;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name for the storage pool")
    private String storagePoolName;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID for the storage pool")
    private Long podId;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for the storage pool")
    private String tags;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL of the storage pool")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the Zone ID for the storage pool")
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
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        try {
            StoragePoolVO result = BaseCmd._storageMgr.createPool(this);
            if (result != null) {
                StoragePoolResponse response = ApiResponseHelper.createStoragePoolResponse(result);
                response.setResponseName(getName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add host");
            }
        } catch (ResourceAllocationException ex1) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex1.getMessage());
        }catch (ResourceInUseException ex2) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex2.getMessage());
        } catch (UnknownHostException ex3) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex3.getMessage());
        }
    }
}
