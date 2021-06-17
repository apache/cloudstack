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
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = CreateKubernetesClusterCmd.APINAME,
        description = "Creates a Kubernetes cluster",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateKubernetesClusterCmd extends BaseAsyncCreateCmd {
    public static final Logger LOGGER = Logger.getLogger(CreateKubernetesClusterCmd.class.getName());
    public static final String APINAME = "createKubernetesCluster";

    @Inject
    public KubernetesClusterService kubernetesClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name for the Kubernetes cluster")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = true, description = "description for the Kubernetes cluster")
    private String description;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
            description = "availability zone in which Kubernetes cluster to be launched")
    private Long zoneId;

    @Parameter(name = ApiConstants.KUBERNETES_VERSION_ID, type = CommandType.UUID, entityType = KubernetesSupportedVersionResponse.class, required = true,
            description = "Kubernetes version with which cluster to be launched")
    private Long kubernetesVersionId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class,
            required = true, description = "the ID of the service offering for the virtual machines in the cluster.")
    private Long serviceOfferingId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the" +
            " virtual machine. Must be used with domainId.")
    private String accountName;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "Deploy cluster for the project")
    private Long projectId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class,
            description = "Network in which Kubernetes cluster is to be launched")
    private Long networkId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.SSH_KEYPAIR, type = CommandType.STRING,
            description = "name of the ssh key pair used to login to the virtual machines")
    private String sshKeyPairName;

    @Parameter(name=ApiConstants.MASTER_NODES, type = CommandType.LONG,
            description = "number of Kubernetes cluster master nodes, default is 1. This option is deprecated, please use 'controlnodes' parameter.")
    @Deprecated
    private Long masterNodes;

    @Parameter(name=ApiConstants.CONTROL_NODES, type = CommandType.LONG,
            description = "number of Kubernetes cluster control nodes, default is 1")
    private Long controlNodes;

    @Parameter(name=ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, type = CommandType.STRING,
            description = "external load balancer IP address while using shared network with Kubernetes HA cluster")
    private String externalLoadBalancerIpAddress;

    @Parameter(name=ApiConstants.SIZE, type = CommandType.LONG,
            required = true, description = "number of Kubernetes cluster worker nodes")
    private Long clusterSize;

    @Parameter(name = ApiConstants.DOCKER_REGISTRY_USER_NAME, type = CommandType.STRING,
            description = "user name for the docker image private registry")
    private String dockerRegistryUserName;

    @Parameter(name = ApiConstants.DOCKER_REGISTRY_PASSWORD, type = CommandType.STRING,
            description = "password for the docker image private registry")
    private String dockerRegistryPassword;

    @Parameter(name = ApiConstants.DOCKER_REGISTRY_URL, type = CommandType.STRING,
            description = "URL for the docker image private registry")
    private String dockerRegistryUrl;

    @Parameter(name = ApiConstants.DOCKER_REGISTRY_EMAIL, type = CommandType.STRING,
            description = "email of the docker image private registry user")
    private String dockerRegistryEmail;

    @Parameter(name = ApiConstants.NODE_ROOT_DISK_SIZE, type = CommandType.LONG,
            description = "root disk size of root disk for each node")
    private Long nodeRootDiskSize;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName == null) {
            return CallContext.current().getCallingAccount().getAccountName();
        }
        return accountName;
    }

    public String getDisplayName() {
        return description;
    }

    public Long getDomainId() {
        if (domainId == null) {
            return CallContext.current().getCallingAccount().getDomainId();
        }
        return domainId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getKubernetesVersionId() {
        return kubernetesVersionId;
    }

    public Long getNetworkId() { return networkId;}

    public String getName() {
        return name;
    }

    public String getSSHKeyPairName() {
        return sshKeyPairName;
    }

    public Long getMasterNodes() {
        if (masterNodes == null) {
            return 1L;
        }
        return masterNodes;
    }

    public Long getControlNodes() {
        if (controlNodes == null) {
            return 1L;
        }
        return controlNodes;
    }

    public String getExternalLoadBalancerIpAddress() {
        return externalLoadBalancerIpAddress;
    }

    public Long getClusterSize() {
        return clusterSize;
    }

    public String getDockerRegistryUserName() {
        return dockerRegistryUserName;
    }

    public String getDockerRegistryPassword() {
        return dockerRegistryPassword;
    }

    public String getDockerRegistryUrl() {
        return dockerRegistryUrl;
    }

    public String getDockerRegistryEmail() {
        return dockerRegistryEmail;
    }

    public Long getNodeRootDiskSize() {
        return nodeRootDiskSize;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    public static String getResultObjectName() {
        return "kubernetescluster";
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CREATE;
    }

    @Override
    public String getCreateEventType() {
        return KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating Kubernetes cluster";
    }

    @Override
    public String getEventDescription() {
        return "Creating Kubernetes cluster. Cluster Id: " + getEntityId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public void execute() {
        try {
            if (!kubernetesClusterService.startKubernetesCluster(getEntityId(), true)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start Kubernetes cluster");
            }
            KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getEntityId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void create() throws CloudRuntimeException {
        try {
            KubernetesCluster cluster = kubernetesClusterService.createKubernetesCluster(this);
            if (cluster == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Kubernetes cluster");
            }
            setEntityId(cluster.getId());
            setEntityUuid(cluster.getUuid());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
