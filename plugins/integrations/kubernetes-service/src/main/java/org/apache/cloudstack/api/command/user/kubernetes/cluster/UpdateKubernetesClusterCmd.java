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

import java.util.HashMap;
import java.util.List;
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
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VmDetailConstants;

@APICommand(name = "updateKubernetesCluster",
        description = "Updates the service offering, root disk size and affinity groups of the node types of a CloudManaged Kubernetes cluster. " +
                "The new settings are used for the nodes created from then on. With rollout=true, the existing nodes that differ from them are " +
                "drained, stopped, changed and started again, one at a time, or maxunavailable worker nodes at a time. A rollout that fails " +
                "is resumed by calling this API again with rollout=true",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        since = "24.0.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateKubernetesClusterCmd extends BaseAsyncCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;
    @Inject
    protected KubernetesServiceHelper kubernetesServiceHelper;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true,
            entityType = KubernetesClusterResponse.class,
            description = "The ID of the Kubernetes cluster")
    private Long id;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.NODE_TYPE_OFFERING_MAP, type = CommandType.MAP,
            description = "Node type to service offering ID mapping, for the node types control, worker and etcd. " +
                    "Example: nodeofferings[0].node=worker&nodeofferings[0].offering=<offering ID>")
    private Map<String, Map<String, String>> serviceOfferingNodeTypeMap;

    @Parameter(name = ApiConstants.NODE_TYPE_ROOT_DISK_SIZE_MAP, type = CommandType.MAP,
            description = "Node type to root disk size in GB mapping, for the node types control, worker and etcd. " +
                    "Example: noderootdisksizes[0].node=worker&noderootdisksizes[0].size=50. A rollout only grows root disks")
    private Map<String, Map<String, String>> rootDiskSizeNodeTypeMap;

    @ACL(accessType = SecurityChecker.AccessType.UseEntry)
    @Parameter(name = ApiConstants.NODE_TYPE_AFFINITY_GROUP_MAP, type = CommandType.MAP,
            description = "Node type to affinity group IDs mapping, for the node types control, worker and etcd. " +
                    "The groups replace those of the node type. " +
                    "Example: nodeaffinitygroups[0].node=worker&nodeaffinitygroups[0].affinitygroup=<group ID>,<group ID>")
    private Map<String, Map<String, String>> affinityGroupNodeTypeMap;

    @Parameter(name = ApiConstants.ROLLOUT, type = CommandType.BOOLEAN,
            description = "Whether to restart the existing nodes that differ from the settings of their node type with them. " +
                    "The cluster must be Running or Alert. If false, the default, only the settings are stored")
    private Boolean rollout;

    @Parameter(name = ApiConstants.CANCEL_ROLLOUT, type = CommandType.BOOLEAN,
            description = "Asks the rollout in progress to stop once it is done with the nodes it is working on. Takes no other parameter")
    private Boolean cancelRollout;

    @Parameter(name = ApiConstants.MAX_UNAVAILABLE, type = CommandType.INTEGER,
            description = "With rollout=true, the number of worker nodes restarted at a time, 1 by default; " +
                    "above 1, it must be less than the number of worker nodes. " +
                    "A cluster with a single worker node has no worker left running while it restarts. " +
                    "Control and etcd nodes are always restarted one at a time, and only if the others keep the cluster running")
    private Integer maxUnavailable;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Map<String, Long> getServiceOfferingNodeTypeMap() {
        return kubernetesServiceHelper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);
    }

    /** The root disk sizes in GB, by node type */
    public Map<String, Long> getRootDiskSizeNodeTypeMap() {
        Map<String, Long> mapping = new HashMap<>();
        if (MapUtils.isEmpty(rootDiskSizeNodeTypeMap)) {
            return mapping;
        }
        for (Map<String, String> entry : rootDiskSizeNodeTypeMap.values()) {
            String nodeType = entry.get(VmDetailConstants.CKS_NODE_TYPE);
            String size = entry.get(ApiConstants.SIZE);
            if (StringUtils.isAnyEmpty(nodeType, size)) {
                throw new InvalidParameterValueException(String.format("Incomplete node type to root disk size mapping: '%s' -> '%s'", nodeType, size));
            }
            if (!kubernetesServiceHelper.isValidNodeType(nodeType)) {
                throw new InvalidParameterValueException(String.format("The provided value '%s' for node type is invalid", nodeType));
            }
            long sizeGb = NumberUtils.toLong(size, -1);
            if (sizeGb < 0) {
                throw new InvalidParameterValueException(String.format("The root disk size '%s' for node type %s is not a number of GB", size, nodeType));
            }
            mapping.put(nodeType.toUpperCase(), sizeGb);
        }
        return mapping;
    }

    public Map<String, List<Long>> getAffinityGroupNodeTypeMap() {
        return kubernetesServiceHelper.getAffinityGroupNodeTypeMap(affinityGroupNodeTypeMap);
    }

    public boolean isRollout() {
        return Boolean.TRUE.equals(rollout);
    }

    public boolean isCancelRollout() {
        return Boolean.TRUE.equals(cancelRollout);
    }

    public Integer getMaxUnavailable() {
        return maxUnavailable;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        KubernetesCluster cluster = _entityMgr.findById(KubernetesCluster.class, getId());
        return String.format("Updating Kubernetes cluster ID: %s", cluster != null ? cluster.getUuid() : getId());
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
        return getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            kubernetesClusterService.updateKubernetesCluster(this);
            final KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
