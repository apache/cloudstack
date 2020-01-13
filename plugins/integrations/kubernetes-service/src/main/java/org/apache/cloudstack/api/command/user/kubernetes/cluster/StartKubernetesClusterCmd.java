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
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = StartKubernetesClusterCmd.APINAME, description = "Starts a stopped Kubernetes cluster",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class StartKubernetesClusterCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(StartKubernetesClusterCmd.class.getName());
    public static final String APINAME = "startKubernetesCluster";

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = KubernetesClusterResponse.class, required = true,
            description = "the ID of the Kubernetes cluster")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_START;
    }

    @Override
    public String getEventDescription() {
        KubernetesCluster cluster = _entityMgr.findById(KubernetesCluster.class, getId());
        return String.format("Starting Kubernetes cluster ID: %s", cluster.getUuid());
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public KubernetesCluster validateRequest() {
        if (getId() == null || getId() < 1L) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid Kubernetes cluster ID provided");
        }
        final KubernetesCluster kubernetesCluster = kubernetesClusterService.findById(getId());
        if (kubernetesCluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Given Kubernetes cluster was not found");
        }
        return kubernetesCluster;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        final KubernetesCluster kubernetesCluster = validateRequest();
        try {
            if (!kubernetesClusterService.startKubernetesCluster(kubernetesCluster.getId(), false)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to start Kubernetes cluster ID: %d", getId()));
            }
            final KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(kubernetesCluster.getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

}
