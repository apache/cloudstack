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
import java.util.Map;

import org.apache.cloudstack.api.ApiCommandResourceType;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.StoragePoolResponse;

import com.cloud.storage.StoragePool;
import com.cloud.user.Account;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;

@SuppressWarnings("rawtypes")
@APICommand(name = "updateStoragePool", description = "Updates a storage pool.", responseObject = StoragePoolResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateStoragePoolCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, required = true, description = "the Id of the storage pool")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, entityType = StoragePoolResponse.class, description = "Change the name of the storage pool", since = "4.15")
    private String name;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "comma-separated list of tags for the storage pool")
    private List<String> tags;

    @Parameter(name = ApiConstants.CAPACITY_IOPS, type = CommandType.LONG, required = false, description = "IOPS CloudStack can provision from this storage pool")
    private Long capacityIops;

    @Parameter(name = ApiConstants.CAPACITY_BYTES, type = CommandType.LONG, required = false, description = "bytes CloudStack can provision from this storage pool")
    private Long capacityBytes;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = false, description = "false to disable the pool for allocation of new volumes, true to" +
            " enable it back.")
    private Boolean enabled;

    @Parameter(name = ApiConstants.DETAILS,
                            type = CommandType.MAP,
                            required = false,
                            description = "the details for the storage pool",
                            since = "4.19.0")
    private Map details;

    @Parameter(name = ApiConstants.URL,
                            type = CommandType.STRING,
                            required = false,
                            description = "the URL of the storage pool",
                            since = "4.19.0")
    private String url;

    @Parameter(name = ApiConstants.IS_TAG_A_RULE, type = CommandType.BOOLEAN, description = ApiConstants.PARAMETER_DESCRIPTION_IS_TAG_A_RULE)
    private Boolean isTagARule;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getTags() {
        return tags;
    }

    public Long getCapacityIops() {
        return capacityIops;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean isTagARule() {
        return isTagARule;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.StoragePool;
    }

    public Map<String,String> getDetails() {
        return details;
    }

    public void setDetails(Map<String,String> details) {
        this.details = details;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void execute() {
        StoragePool result = null;
        if (ObjectUtils.anyNotNull(name, capacityIops, capacityBytes, url, isTagARule, tags) ||
                MapUtils.isNotEmpty(details)) {
            result = _storageService.updateStoragePool(this);
        }

        if (enabled != null) {
            result = enabled ? _storageService.enablePrimaryStoragePool(id)
                    : _storageService.disablePrimaryStoragePool(id);
        }

        if (result != null) {
            StoragePoolResponse response = _responseGenerator.createStoragePoolResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update storage pool");
        }
    }
}
