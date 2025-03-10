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
package org.apache.cloudstack.api.command.user.kubernetes.cluster;

import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.BooleanUtils;

import javax.inject.Inject;

import java.util.List;

@APICommand(name = "addNodesToKubernetesCluster",
        description = "Add nodes as workers to an existing CKS cluster. ",
        responseObject = KubernetesClusterResponse.class,
        since = "4.21.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class AddNodesToKubernetesClusterCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    @Parameter(name = ApiConstants.NODE_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType= UserVmResponse.class,
            description = "comma separated list of (external) node (physical or virtual machines) IDs that need to be" +
                    "added as worker nodes to an existing managed Kubernetes cluster (CKS)",
            required = true)
    private List<Long> nodeIds;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true,
            entityType = KubernetesClusterResponse.class,
            description = "the ID of the Kubernetes cluster", since = "4.21.0")
    private Long clusterId;

    @Parameter(name = ApiConstants.MOUNT_CKS_ISO_ON_VR, type = CommandType.BOOLEAN,
            description = "(optional) Vmware only, uses the CKS cluster network VR to mount the CKS ISO")
    private Boolean mountCksIsoOnVr;

    @Parameter(name = ApiConstants.MANUAL_UPGRADE, type = CommandType.BOOLEAN,
            description = "(optional) indicates if the node is marked for manual upgrade and excluded from the Kubernetes cluster upgrade operation")
    private Boolean manualUpgrade;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public boolean isMountCksIsoOnVr() {
        return BooleanUtils.isTrue(mountCksIsoOnVr);
    }

    public boolean isManualUpgrade() {
        return BooleanUtils.isTrue(manualUpgrade);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD;
    }

    @Override
    public String getEventDescription() {
        return String.format("Adding %s nodes to the Kubernetes cluster with ID: %s", nodeIds.size(), clusterId);
    }

    @Override
    public void execute() {
        try {
            kubernetesClusterService.addNodesToKubernetesCluster(this);
            final KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getClusterId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to add nodes to cluster ID %s due to: %s",
                    getClusterId(), e.getLocalizedMessage()), e);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KubernetesCluster;
    }

    @Override
    public Long getApiResourceId() {
        return getClusterId();
    }

}
