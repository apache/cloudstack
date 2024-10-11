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
package org.apache.cloudstack.api.command.admin.config;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HypervisorCapabilitiesResponse;

import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.user.Account;

@APICommand(name = "updateHypervisorCapabilities",
            description = "Updates a hypervisor capabilities.",
            responseObject = HypervisorCapabilitiesResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class UpdateHypervisorCapabilitiesCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HypervisorCapabilitiesResponse.class, description = "ID of the hypervisor capability")
    private Long id;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the hypervisor for which the hypervisor capabilities are to be updated", since = "4.19.1")
    private String hypervisor;

    @Parameter(name = ApiConstants.HYPERVISOR_VERSION, type = CommandType.STRING, description = "the hypervisor version for which the hypervisor capabilities are to be updated", since = "4.19.1")
    private String hypervisorVersion;

    @Parameter(name = ApiConstants.SECURITY_GROUP_EANBLED, type = CommandType.BOOLEAN, description = "set true to enable security group for this hypervisor.")
    private Boolean securityGroupEnabled;

    @Parameter(name = ApiConstants.MAX_GUESTS_LIMIT, type = CommandType.LONG, description = "the max number of Guest VMs per host for this hypervisor.")
    private Long maxGuestsLimit;

    @Parameter(name = ApiConstants.MAX_DATA_VOLUMES_LIMIT, type = CommandType.INTEGER, description = "the maximum number of Data Volumes that can be attached to a VM for this hypervisor.", since = "4.16.0")
    private Integer maxDataVolumesLimit;

    @Parameter(name = ApiConstants.STORAGE_MOTION_ENABLED, type = CommandType.BOOLEAN, description = "set true to enable storage motion support for this hypervisor", since = "4.16.0")
    private Boolean storageMotionSupported;

    @Parameter(name = ApiConstants.MAX_HOSTS_PER_CLUSTER, type = CommandType.INTEGER, description = "the maximum number of the hypervisor hosts per cluster ", since = "4.16.0")
    private Integer maxHostsPerClusterLimit;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_ENABELD, type = CommandType.BOOLEAN, description = "set true to enable VM snapshots for this hypervisor", since = "4.16.0")
    private Boolean vmSnapshotEnabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean getSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public Long getId() {
        return id;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public Long getMaxGuestsLimit() {
        return maxGuestsLimit;
    }

    public Integer getMaxDataVolumesLimit() {
        return maxDataVolumesLimit;
    }

    public Boolean getStorageMotionSupported() {
        return storageMotionSupported;
    }

    public Integer getMaxHostsPerClusterLimit() {
        return maxHostsPerClusterLimit;
    }

    public Boolean getVmSnapshotEnabled() {
        return vmSnapshotEnabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        HypervisorCapabilities result = _mgr.updateHypervisorCapabilities(this);
        if (result != null) {
            HypervisorCapabilitiesResponse response = _responseGenerator.createHypervisorCapabilitiesResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update hypervisor capabilities");
        }
    }
}
