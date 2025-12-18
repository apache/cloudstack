/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.vm;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VirtualMachineResponse;

import com.cloud.vm.VirtualMachine;

@APICommand(name = "listAffectedVmsForStorageScopeChange",
        description = "List user and system VMs that need to be stopped and destroyed respectively for changing the scope of the storage pool from Zone to Cluster.",
        responseObject = VirtualMachineResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.1",
        authorized = {RoleType.Admin})
public class ListAffectedVmsForStorageScopeChangeCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.CLUSTER_ID,
            type = CommandType.UUID,
            entityType = ClusterResponse.class,
            required = true,
            description = "the Id of the cluster the scope of the storage pool is being changed to")
    private Long clusterIdForScopeChange;

    @Parameter(name = ApiConstants.STORAGE_ID,
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            required = true,
            description = "the Id of the storage pool on which change scope operation is being done")
    private Long storageId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterIdForScopeChange() {
        return clusterIdForScopeChange;
    }

    public Long getStorageId() {
        return storageId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<VirtualMachineResponse> response = _queryService.listAffectedVmsForStorageScopeChange(this);
        response.setResponseName(getCommandName());
        response.setObjectName(VirtualMachine.class.getSimpleName().toLowerCase());
        setResponseObject(response);
    }
}
