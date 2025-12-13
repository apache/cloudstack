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

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.dc.ASNumberVO;
import com.cloud.dc.dao.ASNumberDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
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
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createKubernetesCluster",
        description = "Creates a Kubernetes cluster",
        responseObject = KubernetesClusterResponse.class,
        responseView = ResponseView.Restricted,
        entityType = {KubernetesCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateKubernetesClusterCmd extends BaseAsyncCreateCmd {
    private static final Long DEFAULT_NODE_ROOT_DISK_SIZE = 8L;

    @Inject
    public KubernetesClusterService kubernetesClusterService;
    @Inject
    protected KubernetesServiceHelper kubernetesClusterHelper;
    @Inject
    private ConfigurationDao configurationDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private ASNumberDao asNumberDao;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name for the Kubernetes cluster")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "description for the Kubernetes cluster")
    private String description;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
            description = "availability zone in which Kubernetes cluster to be launched")
    private Long zoneId;

    @Parameter(name = ApiConstants.KUBERNETES_VERSION_ID, type = CommandType.UUID, entityType = KubernetesSupportedVersionResponse.class,
            description = "Kubernetes version with which cluster to be launched")
    private Long kubernetesVersionId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class,
            description = "the ID of the service offering for the virtual machines in the cluster.")
    private Long serviceOfferingId;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.NODE_TYPE_OFFERING_MAP, type = CommandType.MAP,
            description = "(Optional) Node Type to Service Offering ID mapping. If provided, it overrides the serviceofferingid parameter",
            since = "4.21.0")
    private Map<String, Map<String, String>> serviceOfferingNodeTypeMap;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.NODE_TYPE_TEMPLATE_MAP, type = CommandType.MAP,
            description = "(Optional) Node Type to Template ID mapping. If provided, it overrides the default template: System VM template",
            since = "4.21.0")
    private Map<String, Map<String, String>> templateNodeTypeMap;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.ETCD_NODES, type = CommandType.LONG,
            description = "(Optional) Number of Kubernetes cluster etcd nodes, default is 0." +
                    "In case the number is greater than 0, etcd nodes are separate from master nodes and are provisioned accordingly",
            since = "4.21.0")
    private Long etcdNodes;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the" +
            " virtual machine. Must be used with domainId.")
    private String accountName;

    @ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used. " +
                    "Hosts dedicated to the specified domain will be used for deploying the cluster")
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
            description = "number of Kubernetes cluster worker nodes")
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

    @Parameter(name = ApiConstants.NODE_ROOT_DISK_SIZE, type = CommandType.LONG,
            description = "root disk size in GB for each node")
    private Long nodeRootDiskSize;

    @Parameter(name = ApiConstants.CLUSTER_TYPE, type = CommandType.STRING, description = "type of the cluster: CloudManaged, ExternalManaged. The default value is CloudManaged.", since="4.19.0")
    private String clusterType;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the hypervisor on which the CKS cluster is to be deployed. This is required if the zone in which the CKS cluster is being deployed has clusters with different hypervisor types.", since = "4.21.0")
    private String hypervisor;

    @Parameter(name = ApiConstants.CNI_CONFIG_ID, type = CommandType.UUID, entityType = UserDataResponse.class, description = "the ID of the Userdata", since = "4.21.0")
    private Long cniConfigId;

    @Parameter(name = ApiConstants.CNI_CONFIG_DETAILS, type = CommandType.MAP,
            description = "used to specify the parameters values for the variables in userdata. " +
                    "Example: cniconfigdetails[0].key=accesskey&cniconfigdetails[0].value=s389ddssaa&" +
                    "cniconfigdetails[1].key=secretkey&cniconfigdetails[1].value=8dshfsss",
            since = "4.21.0")
    private Map cniConfigDetails;

    @Parameter(name = ApiConstants.ENABLE_CSI, type = CommandType.BOOLEAN, description = "if true, setups up CloudStack CSI driver", since = "4.22.0")
    private Boolean enableCsi;

    @Parameter(name=ApiConstants.AS_NUMBER, type=CommandType.LONG, description="the AS Number of the network")
    private Long asNumber;

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
        return StringUtils.firstNonEmpty(description, name);
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

    public long getEtcdNodes() {
        return etcdNodes == null ? 0 : etcdNodes;
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

    public Long getNodeRootDiskSize() {
        if (nodeRootDiskSize != null) {
            if (nodeRootDiskSize < DEFAULT_NODE_ROOT_DISK_SIZE) {
                throw new InvalidParameterValueException("Provided node root disk size is lesser than default size of " + DEFAULT_NODE_ROOT_DISK_SIZE +"GB");
            }
            return nodeRootDiskSize;
        } else {
            return DEFAULT_NODE_ROOT_DISK_SIZE;
        }
    }

    public String getClusterType() {
        if (clusterType == null) {
            return KubernetesCluster.ClusterType.CloudManaged.toString();
        }
        return clusterType;
    }

    public Map<String, Long> getServiceOfferingNodeTypeMap() {
        return kubernetesClusterHelper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);
    }

    public Map<String, Long> getTemplateNodeTypeMap() {
        return kubernetesClusterHelper.getTemplateNodeTypeMap(templateNodeTypeMap);
    }

    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisor == null ? null : Hypervisor.HypervisorType.getType(hypervisor);
    }

    private Pair<NetworkOfferingVO,NetworkVO> getKubernetesNetworkOffering(Long networkId) {
        if (Objects.isNull(networkId)) {
            ConfigurationVO configurationVO = configurationDao.findByName(KubernetesClusterService.KubernetesClusterNetworkOffering.key());
            String offeringName = configurationVO.getValue();
            return new Pair<>(networkOfferingDao.findByUniqueName(offeringName), null);
        } else {
            NetworkVO networkVO = networkDao.findById(getNetworkId());
            if (networkVO == null) {
                throw new InvalidParameterException(String.format("Failed to find network with id: %s", getNetworkId()));
            }
            NetworkOfferingVO offeringVO = networkOfferingDao.findById(networkVO.getNetworkOfferingId());
            return new Pair<>(offeringVO, networkVO);
        }
    }

    public Long getAsNumber() {
        Pair<NetworkOfferingVO, NetworkVO> offeringAndNetwork = getKubernetesNetworkOffering(getNetworkId());
        NetworkOfferingVO offering = offeringAndNetwork.first();
        NetworkVO networkVO = offeringAndNetwork.second();

        if (offering == null) {
            throw new CloudRuntimeException("Failed to find kubernetes network offering");
        }
        ASNumberVO asNumberVO = null;
        if (Objects.isNull(getNetworkId()) && !offering.isForVpc()) {
            if (Boolean.TRUE.equals(NetworkOffering.RoutingMode.Dynamic.equals(offering.getRoutingMode()) && offering.isSpecifyAsNumber()) && asNumber == null) {
                throw new InvalidParameterException("AsNumber must be specified as network offering has specifyasnumber set");
            }
        } else if (Objects.nonNull(networkVO)) {
            if (offering.isForVpc()) {
                asNumberVO = asNumberDao.findByZoneAndVpcId(getZoneId(), networkVO.getVpcId());
            } else {
                asNumberVO = asNumberDao.findByZoneAndNetworkId(getZoneId(), getNetworkId());
            }
        }
        if (Objects.nonNull(asNumberVO)) {
            return asNumberVO.getAsNumber();
        }
        return asNumber;
    }

    public Map getCniConfigDetails() {
        return convertDetailsToMap(cniConfigDetails);
    }

    public Long getCniConfigId() {
        return cniConfigId;
    }

    public boolean getEnableCsi() {
        return Objects.nonNull(enableCsi) ? enableCsi : Boolean.FALSE;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

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
        return "Creating Kubernetes cluster";
    }

    @Override
    public String getEventDescription() {
        return "Creating Kubernetes cluster Id: " + getEntityId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KubernetesCluster;
    }

    @Override
    public void execute() {
        try {
            kubernetesClusterService.startKubernetesCluster(this);
            KubernetesClusterResponse response = kubernetesClusterService.createKubernetesClusterResponse(getEntityId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void create() throws CloudRuntimeException {
        KubernetesCluster cluster;
        KubernetesCluster.ClusterType type;
        try {
            type = KubernetesCluster.ClusterType.valueOf(getClusterType());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException("Unable to resolve cluster type " + getClusterType() + " to a supported value (CloudManaged, ExternalManaged)");
        }

        try {
            if (type == KubernetesCluster.ClusterType.CloudManaged) {
                cluster = kubernetesClusterService.createManagedKubernetesCluster(this);
            } else {
                cluster = kubernetesClusterService.createUnmanagedKubernetesCluster(this);
            }
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
