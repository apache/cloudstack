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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterNodeGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "scaleKubernetesClusterNodeGroup",
        description = "Scales a node group of a running CloudManaged Kubernetes cluster for its size or service offering",
        responseObject = KubernetesClusterNodeGroupResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ScaleKubernetesClusterNodeGroupCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = KubernetesClusterNodeGroupResponse.class, required = true,
            description = "The ID of the node group")
    private Long id;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG,
            description = "New number of nodes in the node group")
    private Long size;

    @Parameter(name = ApiConstants.NODE_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = UserVmResponse.class,
            description = "The IDs of the node group nodes to be removed. Can not be passed along with size or serviceofferingid")
    private List<Long> nodeIds;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class,
            description = "The ID of the new service offering for the nodes of the node group")
    private Long serviceOfferingId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getSize() {
        return size;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODE_GROUP_SCALE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Scaling Kubernetes cluster node group ID: %s", getResourceUuid(ApiConstants.ID));
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
            KubernetesClusterNodeGroup nodeGroup = kubernetesClusterService.scaleKubernetesClusterNodeGroup(this);
            KubernetesClusterNodeGroupResponse response = kubernetesClusterService.createKubernetesClusterNodeGroupResponse(nodeGroup.getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
