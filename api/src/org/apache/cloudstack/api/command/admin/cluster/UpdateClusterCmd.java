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
package org.apache.cloudstack.api.command.admin.cluster;

import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.org.Cluster;
import com.cloud.user.Account;

@APICommand(name = "updateCluster", description = "Updates an existing cluster", responseObject = ClusterResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateClusterCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateClusterCmd.class.getName());

    private static final String s_name = "updateclusterresponse";

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true, description = "the ID of the Cluster")
    private Long id;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING, description = "the cluster name")
    private String clusterName;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "hypervisor type of the cluster")
    private String hypervisor;

    @Parameter(name = ApiConstants.CLUSTER_TYPE, type = CommandType.STRING, description = "hypervisor type of the cluster")
    private String clusterType;

    @Parameter(name = ApiConstants.ALLOCATION_STATE, type = CommandType.STRING, description = "Allocation state of this cluster for allocation of new resources")
    private String allocationState;

    @Parameter(name = ApiConstants.MANAGED_STATE, type = CommandType.STRING, description = "whether this cluster is managed by cloudstack")
    private String managedState;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_TRAFFIC_LABELS, type = CommandType.MAP, description = "Map of physical network traffic id (of guest and public traffic in the zone) and vmware network label (vSwitch name) in the cluster.")
    private Map physicalNetworkTrafficLabels;

    public String getClusterName() {
        return clusterName;
    }

    public Long getId() {
        return id;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String type) {
        this.clusterType = type;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    public String getManagedstate() {
        return managedState;
    }

    public void setManagedstate(String managedstate) {
        this.managedState = managedstate;
    }

    public Map<String, String> getPhysicalNetworkTrafficLabels() {
        return physicalNetworkTrafficLabels;
    }

    @Override
    public void execute() {
        Cluster cluster = _resourceService.getCluster(getId());
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + getId());
        }
        Cluster result = _resourceService.updateCluster(cluster, getClusterType(), getHypervisor(), getAllocationState(), getManagedstate(), getPhysicalNetworkTrafficLabels());
        if (result != null) {
            ClusterResponse clusterResponse = _responseGenerator.createClusterResponse(cluster, false, true);
            clusterResponse.setResponseName(getCommandName());
            this.setResponseObject(clusterResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update cluster");
        }
    }
}
