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
import org.apache.cloudstack.api.response.KubernetesClusterNodeResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "updateKubernetesClusterNode",
        description = "Updates the Kubernetes labels and taints of a node of a node group, in addition to the labels and taints of " +
                "its node group. A label or taint of the node overrides the label of the node group with the same key, or the " +
                "taint of the node group with the same key and effect",
        responseObject = KubernetesClusterNodeResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateKubernetesClusterNodeCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true,
            description = "The ID of the instance of the node")
    private Long id;

    @Parameter(name = ApiConstants.NODE_LABELS, type = CommandType.MAP,
            description = "New Kubernetes labels of the node, replacing its existing ones. " +
                    "Example: nodelabels[0].key=disktype&nodelabels[0].value=ssd")
    private Map<String, Map<String, String>> labels;

    @Parameter(name = ApiConstants.NODE_TAINTS, type = CommandType.MAP,
            description = "New Kubernetes taints of the node, replacing its existing ones. " +
                    "Example: nodetaints[0].key=gpu&nodetaints[0].value=true&nodetaints[0].effect=NoSchedule")
    private Map<String, Map<String, String>> taints;

    @Parameter(name = ApiConstants.CLEANUP_NODE_LABELS, type = CommandType.BOOLEAN,
            description = "Removes all the Kubernetes labels of the node, keeping the labels of its node group. Can not be passed along with nodelabels")
    private Boolean cleanupLabels;

    @Parameter(name = ApiConstants.CLEANUP_NODE_TAINTS, type = CommandType.BOOLEAN,
            description = "Removes all the Kubernetes taints of the node, keeping the taints of its node group. Can not be passed along with nodetaints")
    private Boolean cleanupTaints;

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

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Updating Kubernetes cluster node, instance ID: %s", getResourceUuid(ApiConstants.ID));
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, getId());
        return vm != null ? vm.getAccountId() : CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            KubernetesClusterNodeResponse response = kubernetesClusterService.updateKubernetesClusterNode(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
