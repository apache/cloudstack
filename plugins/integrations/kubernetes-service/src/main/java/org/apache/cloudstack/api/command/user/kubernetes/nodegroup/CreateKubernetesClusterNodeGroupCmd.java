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
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createKubernetesClusterNodeGroup",
        description = "Creates a node group of worker nodes with their own service offering, template, labels and taints " +
                "in a running CloudManaged Kubernetes cluster",
        responseObject = KubernetesClusterNodeGroupResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateKubernetesClusterNodeGroupCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = KubernetesClusterResponse.class, required = true,
            description = "The ID of the Kubernetes cluster")
    private Long clusterId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Name of the node group, unique in the Kubernetes cluster. At most 20 lower case alphanumeric characters or '-'")
    private String name;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, required = true,
            description = "The ID of the service offering for the nodes of the node group")
    private Long serviceOfferingId;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class,
            description = "The ID of the CKS template for the nodes of the node group. Defaults to the template of the worker nodes of the cluster")
    private Long templateId;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG, required = true,
            description = "Number of nodes in the node group, can be 0")
    private Long size;

    @Parameter(name = ApiConstants.NODE_LABELS, type = CommandType.MAP,
            description = "Kubernetes labels for the nodes of the node group. " +
                    "Example: nodelabels[0].key=disktype&nodelabels[0].value=ssd")
    private Map<String, Map<String, String>> labels;

    @Parameter(name = ApiConstants.NODE_TAINTS, type = CommandType.MAP,
            description = "Kubernetes taints for the nodes of the node group, effect is one of NoSchedule, PreferNoSchedule, NoExecute. " +
                    "Example: nodetaints[0].key=gpu&nodetaints[0].value=true&nodetaints[0].effect=NoSchedule")
    private Map<String, Map<String, String>> taints;

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

    public Long getClusterId() {
        return clusterId;
    }

    public String getName() {
        return name;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getSize() {
        return size;
    }

    public Map<String, Map<String, String>> getLabels() {
        return labels;
    }

    public Map<String, Map<String, String>> getTaints() {
        return taints;
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
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODE_GROUP_CREATE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Creating node group %s for Kubernetes cluster ID: %s", getName(), getResourceUuid(ApiConstants.CLUSTER_ID));
    }

    @Override
    public long getEntityOwnerId() {
        KubernetesCluster cluster = _entityMgr.findById(KubernetesCluster.class, getClusterId());
        return cluster != null ? cluster.getAccountId() : CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KubernetesCluster;
    }

    @Override
    public Long getApiResourceId() {
        return getClusterId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            KubernetesClusterNodeGroup nodeGroup = kubernetesClusterService.createKubernetesClusterNodeGroup(this);
            KubernetesClusterNodeGroupResponse response = kubernetesClusterService.createKubernetesClusterNodeGroupResponse(nodeGroup.getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
