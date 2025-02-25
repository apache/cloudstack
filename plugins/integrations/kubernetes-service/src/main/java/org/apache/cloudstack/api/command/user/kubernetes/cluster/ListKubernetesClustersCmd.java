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

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listKubernetesClusters",
        description = "Lists Kubernetes clusters",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListKubernetesClustersCmd extends BaseListProjectAndAccountResourcesCmd {

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = KubernetesClusterResponse.class,
            description = "the ID of the Kubernetes cluster")
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "state of the Kubernetes cluster")
    private String state;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the Kubernetes cluster" +
            " (a substring match is made against the parameter value, data for all matching Kubernetes clusters will be returned)")
    private String name;

    @Parameter(name = ApiConstants.CLUSTER_TYPE, type = CommandType.STRING, since = "4.19.0",
            description = "type of the cluster: CloudManaged, ExternalManaged")
    private String clusterType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public String getClusterType() {
        return clusterType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            ListResponse<KubernetesClusterResponse> response = kubernetesClusterService.listKubernetesClusters(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
