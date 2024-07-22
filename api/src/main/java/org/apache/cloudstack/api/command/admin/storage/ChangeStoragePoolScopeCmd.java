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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.StoragePool;

@APICommand(name = "changeStoragePoolScope", description = "Changes the scope of a storage pool when the pool is in Disabled state." +
        "This feature is officially tested and supported for Hypervisors: KVM and VMware, Protocols: NFS and Ceph, and Storage Provider: DefaultPrimary. " +
        "There might be extra steps involved to make this work for other hypervisors and storage options.",
        responseObject = SuccessResponse.class, since= "4.19.1", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ChangeStoragePoolScopeCmd extends BaseAsyncCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, required = true, description = "the Id of the storage pool")
    private Long id;

    @Parameter(name = ApiConstants.SCOPE, type = CommandType.STRING, required = true, description = "the scope of the storage: cluster or zone")
    private String scope;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "the Id of the cluster to use if scope is being set to Cluster")
    private Long clusterId;

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.StoragePool;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    public String getEventType() {
        return EventTypes.EVENT_CHANGE_STORAGE_POOL_SCOPE;
    }

    @Override
    public String getEventDescription() {
        String description = "Change storage pool scope. Storage pool Id: ";
        StoragePool pool = _entityMgr.findById(StoragePool.class, getId());
        if (pool != null) {
            description += pool.getUuid();
        } else {
            description += getId();
        }
        description += " to " + getScope();
        return description;
    }

    @Override
    public void execute() {
        _storageService.changeStoragePoolScope(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    public Long getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
