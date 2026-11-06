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

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterNodeGroupResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "updateKubernetesClusterNodeGroup",
        description = "Updates the Kubernetes labels and taints or the autoscaling settings of a node group. The new labels and taints " +
                "replace the existing ones and are applied to the existing nodes of the node group as well as to the nodes added later",
        responseObject = KubernetesClusterNodeGroupResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateKubernetesClusterNodeGroupCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = KubernetesClusterNodeGroupResponse.class, required = true,
            description = "The ID of the node group")
    private Long id;

    @Parameter(name = ApiConstants.NODE_LABELS, type = CommandType.MAP,
            description = "New Kubernetes labels for the nodes of the node group, replacing the existing ones. " +
                    "Example: nodelabels[0].key=disktype&nodelabels[0].value=ssd")
    private Map<String, Map<String, String>> labels;

    @Parameter(name = ApiConstants.NODE_TAINTS, type = CommandType.MAP,
            description = "New Kubernetes taints for the nodes of the node group, replacing the existing ones. " +
                    "Example: nodetaints[0].key=gpu&nodetaints[0].value=true&nodetaints[0].effect=NoSchedule")
    private Map<String, Map<String, String>> taints;

    @Parameter(name = ApiConstants.CLEANUP_NODE_LABELS, type = CommandType.BOOLEAN,
            description = "Removes all the Kubernetes labels of the node group, can not be passed along with nodelabels")
    private Boolean cleanupLabels;

    @Parameter(name = ApiConstants.CLEANUP_NODE_TAINTS, type = CommandType.BOOLEAN,
            description = "Removes all the Kubernetes taints of the node group, can not be passed along with nodetaints")
    private Boolean cleanupTaints;

    @Parameter(name = ApiConstants.AUTOSCALING_ENABLED, type = CommandType.BOOLEAN,
            description = "Whether the size of the node group is managed by the cluster autoscaler, within its minsize and maxsize")
    private Boolean autoscalingEnabled;

    @Parameter(name = ApiConstants.MIN_SIZE, type = CommandType.LONG,
            description = "Minimum size of the node group when autoscaling is enabled")
    private Long minSize;

    @Parameter(name = ApiConstants.MAX_SIZE, type = CommandType.LONG,
            description = "Maximum size of the node group when autoscaling is enabled")
    private Long maxSize;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Map<String, Map<String, String>> getLabels() {
        return labels;
    }

    public Map<String, Map<String, String>> getTaints() {
        return taints;
    }

    public boolean isCleanupLabels() {
        return Boolean.TRUE.equals(cleanupLabels);
    }

    public boolean isCleanupTaints() {
        return Boolean.TRUE.equals(cleanupTaints);
    }

    public Boolean isAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public Long getMinSize() {
        return minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODE_GROUP_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Updating Kubernetes cluster node group ID: %s", getResourceUuid(ApiConstants.ID));
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
            KubernetesClusterNodeGroup nodeGroup = kubernetesClusterService.updateKubernetesClusterNodeGroup(this);
            KubernetesClusterNodeGroupResponse response = kubernetesClusterService.createKubernetesClusterNodeGroupResponse(nodeGroup.getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
