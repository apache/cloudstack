// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.storage;

import java.net.UnknownHostException;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;


@SuppressWarnings("rawtypes")
@APICommand(name = "createStoragePool", description="Creates a storage pool.", responseObject=StoragePoolResponse.class)
public class CreateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStoragePoolCmd.class.getName());

    private static final String s_name = "createstoragepoolresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.UUID, entityType = ClusterResponse.class,
            description="the cluster ID for the storage pool")
    private Long clusterId;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.MAP, description="the details for the storage pool")
    private Map details;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name for the storage pool")
    private String storagePoolName;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.UUID, entityType = PodResponse.class,
            description="the Pod ID for the storage pool")
    private Long podId;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for the storage pool")
    private String tags;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL of the storage pool")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="the Zone ID for the storage pool")
    private Long zoneId;

    @Parameter(name=ApiConstants.PROVIDER, type=CommandType.STRING,
            required=false, description="the storage provider name")
    private String storageProviderName;

    @Parameter(name=ApiConstants.SCOPE, type=CommandType.STRING,
            required=false, description="the scope of the storage: cluster or zone")
    private String scope;

    @Parameter(name=ApiConstants.MANAGED, type=CommandType.BOOLEAN,
            required=false, description="whether the storage should be managed by CloudStack")
    private Boolean managed;

    @Parameter(name=ApiConstants.CAPACITY_IOPS, type=CommandType.LONG,
            required=false, description="IOPS CloudStack can provision from this storage pool")
    private Long capacityIops;

    @Parameter(name=ApiConstants.CAPACITY_BYTES, type=CommandType.LONG,
            required=false, description="bytes CloudStack can provision from this storage pool")
    private Long capacityBytes;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, required=false,
            description="hypervisor type of the hosts in zone that will be attached to this storage pool. KVM, VMware supported as of now.")
    private String hypervisor;

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

    public String getStorageProviderName() {
        return this.storageProviderName;
    }

    public String getScope() {
        return this.scope;
    }

    public Boolean isManaged() {
    	return managed;
    }

    public Long getCapacityIops() {
        return capacityIops;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public String getHypervisor() {
        return hypervisor;
    }

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
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add storage pool");
            }
        } catch (ResourceUnavailableException ex1) {
            s_logger.warn("Exception: ", ex1);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex1.getMessage());
        }catch (ResourceInUseException ex2) {
            s_logger.warn("Exception: ", ex2);
            throw new ServerApiException(ApiErrorCode.RESOURCE_IN_USE_ERROR, ex2.getMessage());
        } catch (UnknownHostException ex3) {
            s_logger.warn("Exception: ", ex3);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex3.getMessage());
        } catch (Exception ex4) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex4.getMessage());
        }
    }
}
