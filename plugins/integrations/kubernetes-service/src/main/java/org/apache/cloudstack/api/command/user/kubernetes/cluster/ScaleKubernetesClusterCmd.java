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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "scaleKubernetesCluster",
        description = "Scales a created, running or stopped Kubernetes cluster",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ScaleKubernetesClusterCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(ScaleKubernetesClusterCmd.class.getName());

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true,
        entityType = KubernetesClusterResponse.class,
        description = "the ID of the Kubernetes cluster")
    private Long id;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class,
        description = "the ID of the service offering for the virtual machines in the cluster.")
    private Long serviceOfferingId;

    @Parameter(name=ApiConstants.SIZE, type = CommandType.LONG,
        description = "number of Kubernetes cluster nodes")
    private Long clusterSize;

    @Parameter(name = ApiConstants.NODE_IDS,
        type = CommandType.LIST,
        collectionType = CommandType.UUID,
        entityType = UserVmResponse.class,
        description = "the IDs of the nodes to be removed")
    private List<Long> nodeIds;

    @Parameter(name=ApiConstants.AUTOSCALING_ENABLED, type = CommandType.BOOLEAN,
        description = "Whether autoscaling is enabled for the cluster")
    private Boolean isAutoscalingEnabled;

    @Parameter(name=ApiConstants.MIN_SIZE, type = CommandType.LONG,
        description = "Minimum number of worker nodes in the cluster")
    private Long minSize;

    @Parameter(name=ApiConstants.MAX_SIZE, type = CommandType.LONG,
        description = "Maximum number of worker nodes in the cluster")
    private Long maxSize;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getClusterSize() {
        return clusterSize;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public Boolean isAutoscalingEnabled() {
        return isAutoscalingEnabled;
    }

    public Long getMinSize() {
        return minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_SCALE;
    }

    @Override
    public String getEventDescription() {
        String description = "Scaling Kubernetes cluster";
        KubernetesCluster cluster = _entityMgr.findById(KubernetesCluster.class, getId());
        if (cluster != null) {
            description += String.format(" ID: %s", cluster.getUuid());
        } else {
            description += String.format(" ID: %d", getId());
        }
        return description;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        try {
            if (!kubernetesClusterService.scaleKubernetesCluster(this)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to scale Kubernetes cluster ID: %d", getId()));
            }
            final KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
