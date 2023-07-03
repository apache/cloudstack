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

import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RemoveVirtualMachinesFromKubernetesClusterResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "removeVirtualMachinesFromKubernetesCluster",
        description = "Remove VMs from an ExternalManaged kubernetes cluster. Not applicable for CloudManaged kubernetes clusters.",
        responseObject = RemoveVirtualMachinesFromKubernetesClusterResponse.class,
        since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RemoveVirtualMachinesFromKubernetesClusterCmd extends BaseListCmd {
    public static final Logger LOGGER = Logger.getLogger(RemoveVirtualMachinesFromKubernetesClusterCmd.class.getName());

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID,
            entityType = KubernetesClusterResponse.class,
            required = true,
            description = "the ID of the Kubernetes cluster")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_IDS, type = CommandType.LIST,
            collectionType=CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "the IDs of the VMs to remove from the cluster")
    private List<Long> vmIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getVmIds() {
        return vmIds;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            List<RemoveVirtualMachinesFromKubernetesClusterResponse> responseList = kubernetesClusterService.removeVmsFromCluster(this);
            if (responseList.size() < 1) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Provided VMs are not part of the CKS cluster");
            }
            ListResponse<RemoveVirtualMachinesFromKubernetesClusterResponse> listResponse = new ListResponse<>();
            listResponse.setResponseName(getCommandName());
            listResponse.setResponses(responseList);
            setResponseObject(listResponse);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
