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
package org.apache.cloudstack.api.command.user.kubernetes.nodegroup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterNodeGroupResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteKubernetesClusterNodeGroup",
        description = "Deletes a node group of a running CloudManaged Kubernetes cluster, draining and destroying all its nodes",
        responseObject = SuccessResponse.class,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteKubernetesClusterNodeGroupCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = KubernetesClusterNodeGroupResponse.class, required = true,
            description = "The ID of the node group")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODE_GROUP_DELETE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Deleting Kubernetes cluster node group ID: %s", getResourceUuid(ApiConstants.ID));
    }

    @Override
    public long getEntityOwnerId() {
        KubernetesClusterNodeGroup nodeGroup = _entityMgr.findById(KubernetesClusterNodeGroup.class, getId());
        if (nodeGroup != null) {
            KubernetesCluster cluster = _entityMgr.findById(KubernetesCluster.class, nodeGroup.getClusterId());
            if (cluster != null) {
                return cluster.getAccountId();
            }
        }
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KubernetesCluster;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            if (!kubernetesClusterService.deleteKubernetesClusterNodeGroup(this)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to delete Kubernetes cluster node group ID: %d", getId()));
            }
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
