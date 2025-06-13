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

import java.util.List;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import com.cloud.user.Account;

@APICommand(name = "configureStorageAccess", description = "Configure the storage access groups on zone/pod/cluster/host and storage, accordingly connections to the storage pools", responseObject = SuccessResponse.class, since = "4.21.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigureStorageAccessCmd extends BaseAsyncCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "UUID of the zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "UUID of the pod")
    private Long podId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "UUID of the cluster")
    private Long clusterId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "UUID of the host")
    private Long hostId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, description = "UUID of the Storage Pool")
    private Long storageId;

    @Parameter(name = ApiConstants.STORAGE_ACCESS_GROUPS, type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "comma separated list of storage access groups for connecting the storage pools and the hosts",
            since = "4.21.0")
    private List<String> storageAccessGroups;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getStorageId() {
        return storageId;
    }

    public List<String> getStorageAccessGroups() {
        return storageAccessGroups;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.StoragePool;
    }

    @Override
    public void execute() {
        try {
            boolean result = _storageService.configureStorageAccess(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure storage access");
            }
        } catch (Exception e) {
            logger.debug("Failed to configure storage access ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure storage access, " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CONFIGURE_STORAGE_ACCESS;
    }

    @Override
    public String getEventDescription() {
        return "configuring storage access groups";
    }
}
