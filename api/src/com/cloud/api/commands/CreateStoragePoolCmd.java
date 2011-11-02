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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;

@SuppressWarnings("rawtypes")
@Implementation(description="Creates a storage pool.", responseObject=StoragePoolResponse.class)
public class CreateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStoragePoolCmd.class.getName());

    private static final String s_name = "createstoragepoolresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="cluster")
    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, description="the cluster ID for the storage pool")
    private Long clusterId;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.MAP, description="the details for the storage pool")
    private Map details;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name for the storage pool")
    private String storagePoolName;

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID for the storage pool")
    private Long podId;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for the storage pool")
    private String tags;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL of the storage pool")
    private String url;

    @IdentityMapper(entityTableName="data_center")
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
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        try {
            StoragePool result = _storageService.createPool(this);
            if (result != null) {
                StoragePoolResponse response = _responseGenerator.createStoragePoolResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add storage pool");
            }
        } catch (ResourceUnavailableException ex1) {
            s_logger.warn("Exception: ", ex1);
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex1.getMessage());
        }catch (ResourceInUseException ex2) {
            s_logger.warn("Exception: ", ex2);
            throw new ServerApiException(BaseCmd.RESOURCE_IN_USE_ERROR, ex2.getMessage());
        } catch (UnknownHostException ex3) {
            s_logger.warn("Exception: ", ex3);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex3.getMessage());
        }
    }
}
