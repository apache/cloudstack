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
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "updateKubernetesClusterAffinityGroups",
        description = "Updates the affinity group mappings for a stopped Kubernetes cluster",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateKubernetesClusterAffinityGroupCmd extends BaseCmd {

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
    @Parameter(name = ApiConstants.NODE_TYPE_AFFINITY_GROUP_MAP, type = CommandType.MAP,
            description = "Node Type to Affinity Group ID mapping. VMs of each node type will be added to the specified affinity group",
            since = "4.23.0")
    private Map<String, Map<String, String>> affinityGroupNodeTypeMap;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Map<String, List<Long>> getAffinityGroupNodeTypeMap() {
        return kubernetesServiceHelper.getAffinityGroupNodeTypeMap(affinityGroupNodeTypeMap);
    }

    @Override
    public long getEntityOwnerId() {
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
            if (!kubernetesClusterService.updateKubernetesClusterAffinityGroups(this)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        String.format("Failed to update affinity groups for Kubernetes cluster ID: %d", getId()));
            }
            final KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException exception) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, exception.getMessage());
        }
    }
}
