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

package com.cloud.kubernetes.cluster.actionworkers;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;
import static com.cloud.utils.NumbersUtil.toHumanReadableSize;
import static com.cloud.utils.db.Transaction.execute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.cpu.CPU;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.rules.FirewallManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.Ip;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.apache.logging.log4j.Level;

public class KubernetesClusterResourceModifierActionWorker extends KubernetesClusterActionWorker {

    @Inject
    protected CapacityManager capacityManager;
    @Inject
    protected ClusterDao clusterDao;
    @Inject
    protected ClusterDetailsDao clusterDetailsDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected FirewallRulesDao firewallRulesDao;
    @Inject
    protected NetworkACLService networkACLService;
    @Inject
    protected  NetworkACLItemDao networkACLItemDao;
    @Inject
    protected LoadBalancingRulesService lbService;
    @Inject
    protected RulesService rulesService;
    @Inject
    protected FirewallManager firewallManager;
    @Inject
    protected PortForwardingRulesDao portForwardingRulesDao;
    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected DedicatedResourceDao dedicatedResourceDao;
    @Inject
    protected LoadBalancerDao loadBalancerDao;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected UserVmManager userVmManager;
    @Inject
    protected LaunchPermissionDao launchPermissionDao;
    @Inject
    protected VolumeApiService volumeService;
    @Inject
    protected VolumeDao volumeDao;
    @Inject
    protected NetworkOfferingDao networkOfferingDao;

    protected String kubernetesClusterNodeNamePrefix;

    private static final int MAX_CLUSTER_PREFIX_LENGTH = 43;

    protected KubernetesClusterResourceModifierActionWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    protected void init() {
        super.init();
        kubernetesClusterNodeNamePrefix = getKubernetesClusterNodeNamePrefix();
    }

    protected DeployDestination plan(final long nodesCount, final DataCenter zone, final ServiceOffering offering,
                                     final Long domainId, final Long accountId, final Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        boolean useDedicatedHosts = false;
        List<HostVO> hosts = new ArrayList<>();
        Long group = getExplicitAffinityGroup(domainId, accountId);
        if (Objects.nonNull(group)) {
            List<DedicatedResourceVO> dedicatedHosts = new ArrayList<>();
            if (Objects.nonNull(accountId)) {
                dedicatedHosts = dedicatedResourceDao.listByAccountId(accountId);
            } else if (Objects.nonNull(domainId)) {
                dedicatedHosts = dedicatedResourceDao.listByDomainId(domainId);
            }
            for (DedicatedResourceVO dedicatedHost : dedicatedHosts) {
                hosts.add(hostDao.findById(dedicatedHost.getHostId()));
                useDedicatedHosts = true;
            }
        }
        if (hosts.isEmpty()) {
            hosts = resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, zone.getId());
        }
        if (hypervisorType != null) {
            hosts = hosts.stream().filter(x -> x.getHypervisorType() == hypervisorType).collect(Collectors.toList());
        }
        if (arch != null) {
            hosts = hosts.stream().filter(x -> x.getArch().equals(arch)).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(hosts)) {
            String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%d memory=%s) with offering: %s hypervisor: %s and arch: %s",
                    cpu_requested * nodesCount, toHumanReadableSize(ram_requested * nodesCount), offering.getName(), clusterTemplate.getHypervisorType().toString(), arch.getType());
            logAndThrow(Level.WARN, msg, new InsufficientServerCapacityException(msg, DataCenter.class, zone.getId()));
        }

        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
            hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found = false;
        HostVO suitableHost = null;
        for (int i = 1; i <= nodesCount; i++) {
            suitable_host_found = false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO h = hp.first();
                if (!h.getHypervisorType().equals(clusterTemplate.getHypervisorType()) || !h.getArch().equals(clusterTemplate.getArch())) {
                    continue;
                }
                hostDao.loadHostTags(h);
                if (StringUtils.isNotEmpty(offering.getHostTag()) && !(h.getHostTags() != null && h.getHostTags().contains(offering.getHostTag()))) {
                    continue;
                }
                int reserved = hp.second();
                reserved++;
                ClusterVO cluster = clusterDao.findById(h.getClusterId());
                ClusterDetailsVO cluster_detail_cpu = clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Checking host : %s for capacity already reserved %d", h.getName(), reserved));
                }
                if (capacityManager.checkIfHostHasCapacity(h, cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    logger.debug("Found host {} with enough capacity: CPU={} RAM={}", h.getName(), cpu_requested * reserved, toHumanReadableSize(ram_requested * reserved));
                    hostEntry.setValue(new Pair<HostVO, Integer>(h, reserved));
                    suitable_host_found = true;
                    suitableHost = h;
                    break;
                }
            }
            if (!suitable_host_found) {
                if (logger.isInfoEnabled()) {
                    logger.info("Suitable hosts not found in datacenter: {} for node {}, with offering: {} and hypervisor: {}",
                            zone, i, offering, clusterTemplate.getHypervisorType().toString());
                }
                break;
            }
        }
        if (suitable_host_found) {
            if (logger.isInfoEnabled()) {
                logger.info("Suitable hosts found in datacenter: {}, creating deployment destination", zone);
            }
            if (useDedicatedHosts) {
                return new DeployDestination(zone, null, null, suitableHost);
            }
            return new DeployDestination(zone, null, null, null);
        }
        String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%d memory=%s) with offering: %s hypervisor: %s and arch: %s",
                cpu_requested * nodesCount, toHumanReadableSize(ram_requested * nodesCount), offering.getName(), clusterTemplate.getHypervisorType().toString(), arch.getType());

        logger.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, zone.getId());
    }

    /**
     * Plan Kubernetes Cluster Deployment
     * @return a map of DeployDestination per node type
     */
    protected Map<String, DeployDestination> planKubernetesCluster(Long domainId, Long accountId, Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) throws InsufficientServerCapacityException {
        Map<String, DeployDestination> destinationMap = new HashMap<>();
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (logger.isDebugEnabled()) {
            logger.debug("Checking deployment destination for Kubernetes cluster: {} in zone: {}", kubernetesCluster, zone);
        }
        long controlNodeCount = kubernetesCluster.getControlNodeCount();
        long clusterSize = kubernetesCluster.getNodeCount();
        long etcdNodes = kubernetesCluster.getEtcdNodeCount();
        Map<String, Long> nodeTypeCount = Map.of(WORKER.name(), clusterSize,
                CONTROL.name(), controlNodeCount, ETCD.name(), etcdNodes);

        for (KubernetesClusterNodeType nodeType : CLUSTER_NODES_TYPES_LIST) {
            Long nodes = nodeTypeCount.getOrDefault(nodeType.name(), kubernetesCluster.getServiceOfferingId());
            if (nodes == null || nodes == 0) {
                continue;
            }
            ServiceOffering nodeOffering = getServiceOfferingForNodeTypeOnCluster(nodeType, kubernetesCluster);
            if (logger.isDebugEnabled()) {
                logger.debug("Checking deployment destination for {} nodes on Kubernetes cluster : {} in zone : {}", nodeType.name(), kubernetesCluster.getName(), zone.getName());
            }
            DeployDestination planForNodeType = plan(nodes, zone, nodeOffering, domainId, accountId, hypervisorType, arch);
            destinationMap.put(nodeType.name(), planForNodeType);
        }
        return destinationMap;
    }

    protected void resizeNodeVolume(final UserVm vm) throws ManagementServerException {
        try {
            if (vm.getHypervisorType() == Hypervisor.HypervisorType.VMware && templateDao.findById(vm.getTemplateId()).isDeployAsIs()) {
                List<VolumeVO> vmVols = volumeDao.findByInstance(vm.getId());
                for (VolumeVO volumeVO : vmVols) {
                    if (volumeVO.getVolumeType() == Volume.Type.ROOT) {
                        ResizeVolumeCmd resizeVolumeCmd = new ResizeVolumeCmd();
                        resizeVolumeCmd = ComponentContext.inject(resizeVolumeCmd);
                        resizeVolumeCmd.setSize(kubernetesCluster.getNodeRootDiskSize());
                        resizeVolumeCmd.setId(volumeVO.getId());

                        volumeService.resizeVolume(resizeVolumeCmd);
                    }
                }
            }
        } catch (ResourceAllocationException e) {
            throw new ManagementServerException(String.format("Failed to resize volume of  VM in the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected void startKubernetesVM(final UserVm vm, final Long domainId, final Long accountId, KubernetesClusterNodeType nodeType) throws ManagementServerException {
        CallContext vmContext = null;
        if (!ApiCommandResourceType.VirtualMachine.equals(CallContext.current().getEventResourceType())); {
            vmContext = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
            vmContext.setEventResourceId(vm.getId());
        }
        DeploymentPlan plan = null;
        if (Objects.nonNull(domainId) && !listDedicatedHostsInDomain(domainId).isEmpty()) {
            DeployDestination dest = null;
            try {
                Map<String, DeployDestination> destinationMap = planKubernetesCluster(domainId, accountId, vm.getHypervisorType(), clusterTemplate.getArch());
                dest = destinationMap.get(nodeType.name());
            } catch (InsufficientCapacityException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the cluster failed due to insufficient capacity in the Kubernetes cluster: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
            }
            if (dest != null) {
                plan = new DataCenterDeployment(
                        Objects.nonNull(dest.getDataCenter()) ? dest.getDataCenter().getId() : 0,
                        Objects.nonNull(dest.getPod()) ? dest.getPod().getId() : null,
                        Objects.nonNull(dest.getCluster()) ? dest.getCluster().getId() : null,
                        Objects.nonNull(dest.getHost()) ? dest.getHost().getId() : null,
                        null,
                        null);
            }
        }
        try {
            userVmManager.startVirtualMachine(vm, plan);
        } catch (OperationTimedoutException | ResourceUnavailableException | InsufficientCapacityException ex) {
            throw new ManagementServerException(String.format("Failed to start VM in the Kubernetes cluster : %s", kubernetesCluster.getName()), ex);
        } finally {
            if (vmContext != null) {
                CallContext.unregister();
            }
        }

        UserVm startVm = userVmDao.findById(vm.getId());
        if (!startVm.getState().equals(VirtualMachine.State.Running)) {
            throw new ManagementServerException(String.format("Failed to start VM in the Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
    }

    protected List<UserVm> provisionKubernetesClusterNodeVms(final long nodeCount, final int offset,
                                                             final String controlIpAddress, final Long domainId, final Long accountId) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        List<UserVm> nodes = new ArrayList<>();
        for (int i = offset + 1; i <= nodeCount; i++) {
            CallContext vmContext = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
            try {
                UserVm vm = createKubernetesNode(controlIpAddress, domainId, accountId);
                vmContext.setEventResourceId(vm.getId());
                addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId(), false, false, false, false);
                if (kubernetesCluster.getNodeRootDiskSize() > 0) {
                    resizeNodeVolume(vm);
                }
                startKubernetesVM(vm, domainId, accountId, WORKER);
                vm = userVmDao.findById(vm.getId());
                if (vm == null) {
                    throw new ManagementServerException(String.format("Failed to provision worker VM for Kubernetes cluster : %s", kubernetesCluster.getName()));
                }
                nodes.add(vm);
                logger.info("Provisioned node VM: {} in to the Kubernetes cluster: {}", vm, kubernetesCluster);
            } finally {
                CallContext.unregister();
            }
        }
        return nodes;
    }

    protected List<UserVm> provisionKubernetesClusterNodeVms(final long nodeCount, final String controlIpAddress, final Long domainId, final Long accountId) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        return provisionKubernetesClusterNodeVms(nodeCount, 0, controlIpAddress, domainId, accountId);
    }

    protected UserVm createKubernetesNode(String joinIp, Long domainId, Long accountId) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm nodeVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = getServiceOfferingForNodeTypeOnCluster(WORKER, kubernetesCluster);
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Account owner = accountDao.findById(kubernetesCluster.getAccountId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        if (Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType())) {
            customParameterMap.put(VmDetailConstants.ROOT_DISK_CONTROLLER, "scsi");
        }
        String suffix = Long.toHexString(System.currentTimeMillis());
        String hostName = String.format("%s-node-%s", kubernetesClusterNodeNamePrefix, suffix);
        String k8sNodeConfig = null;
        try {
            k8sNodeConfig = getKubernetesNodeConfig(joinIp, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()), false);
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes node configuration file", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        List<String> keypairs = new ArrayList<String>();
        if (StringUtils.isNotBlank(kubernetesCluster.getKeyPair())) {
            keypairs.add(kubernetesCluster.getKeyPair());
        }
        Long affinityGroupId = getExplicitAffinityGroup(domainId, accountId);
        if (kubernetesCluster.getSecurityGroupId() != null && networkModel.checkSecurityGroupSupportForNetwork(owner, zone, networkIds, List.of(kubernetesCluster.getSecurityGroupId()))) {
            List<Long> securityGroupIds = new ArrayList<>();
            securityGroupIds.add(kubernetesCluster.getSecurityGroupId());
            nodeVm = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, workerNodeTemplate, networkIds, securityGroupIds, owner,
                    hostName, hostName, null, null, null, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST,base64UserData, null, null, keypairs,
                    null, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null,
                    null, true, null, UserVmManager.CKS_NODE, null, null);
        } else {
            nodeVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, workerNodeTemplate, networkIds, owner,
                    hostName, hostName, null, null, null, null,
                    Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                    null, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null, null, true, UserVmManager.CKS_NODE, null, null, null);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created node VM : {}, {} in the Kubernetes cluster : {}", hostName, nodeVm, kubernetesCluster.getName());
        }
        return nodeVm;
    }

    protected void provisionFirewallRules(final IpAddress publicIp, final Account account, int startPort, int endPort) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException, NetworkRuleConflictException {
        List<String> sourceCidrList = new ArrayList<String>();
        sourceCidrList.add("0.0.0.0/0");

        CreateFirewallRuleCmd firewallRule = new CreateFirewallRuleCmd();
        firewallRule = ComponentContext.inject(firewallRule);

        firewallRule.setIpAddressId(publicIp.getId());

        firewallRule.setProtocol("TCP");

        firewallRule.setPublicStartPort(startPort);

        firewallRule.setPublicEndPort(endPort);

        firewallRule.setSourceCidrList(sourceCidrList);

        firewallService.createIngressFirewallRule(firewallRule);
        firewallService.applyIngressFwRules(publicIp.getId(), account);
    }

    protected void provisionPublicIpPortForwardingRule(IpAddress publicIp, Network network, Account account,
                                                       final long vmId, final int sourcePort, final int destPort) throws NetworkRuleConflictException, ResourceUnavailableException {
        final long publicIpId = publicIp.getId();
        final long networkId = network.getId();
        final long accountId = account.getId();
        final long domainId = account.getDomainId();
        Nic vmNic = networkModel.getNicInNetwork(vmId, networkId);
        final Ip vmIp = new Ip(vmNic.getIPv4Address());
        PortForwardingRuleVO pfRule = execute((TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>) status -> {
            PortForwardingRuleVO newRule =
                    new PortForwardingRuleVO(null, publicIpId,
                            sourcePort, sourcePort,
                            vmIp,
                            destPort, destPort,
                            "tcp", networkId, accountId, domainId, vmId, null);
            newRule.setDisplay(true);
            newRule.setState(FirewallRule.State.Add);
            newRule = portForwardingRulesDao.persist(newRule);
            return newRule;
        });
        rulesService.applyPortForwardingRules(publicIp.getId(), account);
        if (logger.isInfoEnabled()) {
            logger.info("Provisioned SSH port forwarding rule: {} from port {} to {} on {} to the VM IP: {} in Kubernetes cluster: {}", pfRule, sourcePort, destPort, publicIp.getAddress().addr(), vmIp, kubernetesCluster);
        }
    }

    /**
     * To provision SSH port forwarding rules for the given Kubernetes cluster
     * for its given virtual machines
     *
     * @param publicIp
     * @param network
     * @param account
     * @param clusterVMIds (when empty then method must be called while
     *                     down-scaling of the KubernetesCluster therefore no new rules
     *                     to be added)
     * @throws ResourceUnavailableException
     * @throws NetworkRuleConflictException
     */
    protected void provisionSshPortForwardingRules(IpAddress publicIp, Network network, Account account,
                                                   List<Long> clusterVMIds, Map<Long, Integer> vmIdPortMap) throws ResourceUnavailableException,
            NetworkRuleConflictException {
        if (!CollectionUtils.isEmpty(clusterVMIds)) {
            int defaultNodesCount = clusterVMIds.size() - vmIdPortMap.size();
            int sourcePort = CLUSTER_NODES_DEFAULT_START_SSH_PORT;
            for (int i = 0; i < defaultNodesCount; ++i) {
                sourcePort = CLUSTER_NODES_DEFAULT_START_SSH_PORT + i;
                provisionPublicIpPortForwardingRule(publicIp, network, account, clusterVMIds.get(i), sourcePort, DEFAULT_SSH_PORT);
            }
            for (int i = defaultNodesCount; i < clusterVMIds.size(); ++i) {
                sourcePort += 1;
                provisionPublicIpPortForwardingRule(publicIp, network, account, clusterVMIds.get(i), sourcePort, DEFAULT_SSH_PORT);
            }
        }
    }

    protected FirewallRule removeApiFirewallRule(final IpAddress publicIp) {
        FirewallRule rule = null;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO);
        for (FirewallRuleVO firewallRule : firewallRules) {
            Integer startPort = firewallRule.getSourcePortStart();
            Integer endPort = firewallRule.getSourcePortEnd();
            if (startPort != null && startPort == CLUSTER_API_PORT &&
                    endPort != null && endPort == CLUSTER_API_PORT) {
                rule = firewallRule;
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                logger.debug("The API firewall rule [%s] with the id [%s] was revoked",firewallRule.getName(),firewallRule.getId());
                break;
            }
        }
        return rule;
    }

    protected FirewallRule removeSshFirewallRule(final IpAddress publicIp, final long networkId) {
        FirewallRule rule = null;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO);
        for (FirewallRuleVO firewallRule : firewallRules) {
            PortForwardingRuleVO pfRule = portForwardingRulesDao.findByNetworkAndPorts(networkId, firewallRule.getSourcePortStart(), firewallRule.getSourcePortEnd());
            if (firewallRule.getSourcePortStart() == CLUSTER_NODES_DEFAULT_START_SSH_PORT || (Objects.nonNull(pfRule) && pfRule.getDestinationPortStart() == DEFAULT_SSH_PORT) ) {
                rule = firewallRule;
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                logger.debug("The SSH firewall rule {} with the id {} was revoked", firewallRule.getName(), firewallRule.getId());
                break;
            }
        }
        return rule;
    }

    protected void removePortForwardingRules(final IpAddress publicIp, final Network network, final Account account, final List<Long> removedVMIds) throws ResourceUnavailableException {
        if (!CollectionUtils.isEmpty(removedVMIds)) {
            List<PortForwardingRuleVO> pfRules = new ArrayList<>();
            List<PortForwardingRuleVO> revokedRules = new ArrayList<>();
            for (Long vmId : removedVMIds) {
                pfRules.addAll(portForwardingRulesDao.listByNetwork(network.getId()));
                for (PortForwardingRuleVO pfRule : pfRules) {
                    if (pfRule.getVirtualMachineId() == vmId) {
                        portForwardingRulesDao.remove(pfRule.getId());
                        logger.trace("Marking PF rule {} with Revoke state", pfRule);
                        pfRule.setState(FirewallRule.State.Revoke);
                        revokedRules.add(pfRule);
                        logger.debug("The Port forwarding rule {} with the id {} was removed.", pfRule.getName(), pfRule.getId());
                        break;
                    }
                }
            }
            firewallManager.applyRules(revokedRules, false, true);
        }
    }

    protected void removePortForwardingRules(final IpAddress publicIp, final Network network, final Account account, int startPort, int endPort)
        throws ResourceUnavailableException {
        List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByNetwork(network.getId());
        for (PortForwardingRuleVO pfRule : pfRules) {
            if (startPort <= pfRule.getSourcePortStart() && pfRule.getSourcePortStart() <= endPort) {
                portForwardingRulesDao.remove(pfRule.getId());
                logger.debug("The Port forwarding rule [{}] with the id [{}] was mark as revoked.", pfRule.getName(), pfRule.getId());
                pfRule.setState(FirewallRule.State.Revoke);
            }
        }
        firewallManager.applyRules(pfRules, false, true);
    }

    protected void removeLoadBalancingRule(final IpAddress publicIp, final Network network,
                                           final Account account) throws ResourceUnavailableException {
        List<LoadBalancerVO> loadBalancerRules = loadBalancerDao.listByIpAddress(publicIp.getId());
        loadBalancerRules.stream().filter(lbRules -> lbRules.getNetworkId() == network.getId() && lbRules.getAccountId() == account.getId() && lbRules.getSourcePortStart() == CLUSTER_API_PORT
        && lbRules.getSourcePortEnd() == CLUSTER_API_PORT).forEach(lbRule -> {
            lbService.deleteLoadBalancerRule(lbRule.getId(), true);
            logger.debug("The load balancing rule with the Id: {} was removed",lbRule.getId());
        });
    }

    protected void provisionVpcTierAllowPortACLRule(final Network network, int startPort, int endPorts) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException {
        List<NetworkACLItemVO> aclItems = networkACLItemDao.listByACL(network.getNetworkACLId());
        aclItems = aclItems.stream().filter(networkACLItem -> !NetworkACLItem.State.Revoke.equals(networkACLItem.getState())).collect(Collectors.toList());
        CreateNetworkACLCmd networkACLRule = new CreateNetworkACLCmd();
        networkACLRule = ComponentContext.inject(networkACLRule);

        networkACLRule.setProtocol("TCP");

        networkACLRule.setPublicStartPort(startPort);

        networkACLRule.setPublicEndPort(endPorts);

        networkACLRule.setTrafficType(NetworkACLItem.TrafficType.Ingress.toString());

        networkACLRule.setNetworkId(network.getId());

        networkACLRule.setAclId(network.getNetworkACLId());

        networkACLRule.setAction(NetworkACLItem.Action.Allow.toString());

        NetworkACLItem aclRule = networkACLService.createNetworkACLItem(networkACLRule);
        networkACLService.moveRuleToTheTopInACLList(aclRule);
        networkACLService.applyNetworkACL(aclRule.getAclId());
    }

    protected void removeVpcTierAllowPortACLRule(final Network network, int startPort, int endPort) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException {
        List<NetworkACLItemVO> aclItems = networkACLItemDao.listByACL(network.getNetworkACLId());
        aclItems = aclItems.stream().filter(networkACLItem -> (networkACLItem.getProtocol() != null &&
                        networkACLItem.getProtocol().equals("TCP") &&
                        networkACLItem.getSourcePortStart() != null &&
                        networkACLItem.getSourcePortStart().equals(startPort) &&
                        networkACLItem.getSourcePortEnd() != null &&
                        networkACLItem.getSourcePortEnd().equals(endPort) &&
                        networkACLItem.getAction().equals(NetworkACLItem.Action.Allow)))
                .collect(Collectors.toList());

        for (NetworkACLItemVO aclItem : aclItems) {
            networkACLService.revokeNetworkACLItem(aclItem.getId());
        }
    }

    protected void provisionLoadBalancerRule(final IpAddress publicIp, final Network network,
                                             final Account account, final List<Long> clusterVMIds, final int port) throws NetworkRuleConflictException,
            InsufficientAddressCapacityException {
        LoadBalancer lb = lbService.createPublicLoadBalancerRule(null, "api-lb", "LB rule for API access",
                port, port, port, port,
                publicIp.getId(), NetUtils.TCP_PROTO, "roundrobin", network.getId(),
                account.getId(), false, NetUtils.TCP_PROTO, true);

        Map<Long, List<String>> vmIdIpMap = new HashMap<>();
        for (int i = 0; i < kubernetesCluster.getControlNodeCount(); ++i) {
            List<String> ips = new ArrayList<>();
            Nic controlVmNic = networkModel.getNicInNetwork(clusterVMIds.get(i), kubernetesCluster.getNetworkId());
            ips.add(controlVmNic.getIPv4Address());
            vmIdIpMap.put(clusterVMIds.get(i), ips);
        }
        lbService.assignToLoadBalancer(lb.getId(), null, vmIdIpMap, false);
    }

    protected Map<Long, Integer> createFirewallRules(IpAddress publicIp, List<Long> clusterVMIds, boolean apiRule) throws ManagementServerException {
        // Firewall rule for SSH access on each node VM
        Map<Long, Integer> vmIdPortMap = addFirewallRulesForNodes(publicIp, clusterVMIds.size());
        if (!apiRule) {
            return vmIdPortMap;
        }
        // Firewall rule for API access for control node VMs
        CallContext.register(CallContext.current(), null);
        try {
            provisionFirewallRules(publicIp, owner, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (logger.isInfoEnabled()) {
                logger.info("Provisioned firewall rule to open up port {} on {} for Kubernetes cluster {}", CLUSTER_API_PORT, publicIp.getAddress().addr(), kubernetesCluster);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        } finally {
            CallContext.unregister();
        }
        return vmIdPortMap;
    }

    /**
     * Setup network rules for Kubernetes cluster
     * Open up firewall port CLUSTER_API_PORT, secure port on which Kubernetes
     * API server is running. Also create load balancing rule to forward public
     * IP traffic to control VMs' private IP.
     * Open up  firewall ports NODES_DEFAULT_START_SSH_PORT to NODES_DEFAULT_START_SSH_PORT+n
     * for SSH access. Also create port-forwarding rule to forward public IP traffic to all
     * @param network
     * @param clusterVMIds
     * @throws ManagementServerException
     */
    protected void setupKubernetesClusterIsolatedNetworkRules(IpAddress publicIp, Network network, List<Long> clusterVMIds, boolean apiRule) throws ManagementServerException {
        Map<Long, Integer> vmIdPortMap = createFirewallRules(publicIp, clusterVMIds, apiRule);

        // Port forwarding rule for SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds, vmIdPortMap);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }

        if (!apiRule) {
            return;
        }
        // Load balancer rule for API access for control node VMs
        try {
            provisionLoadBalancerRule(publicIp, network, owner, clusterVMIds, CLUSTER_API_PORT);
        } catch (NetworkRuleConflictException | InsufficientAddressCapacityException e) {
            throw new ManagementServerException(String.format("Failed to provision load balancer rule for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected void createVpcTierAclRules(Network network) throws ManagementServerException {
        if (network.getNetworkACLId() == NetworkACL.DEFAULT_ALLOW) {
            return;
        }
        // ACL rule for API access for control node VMs
        CallContext.register(CallContext.current(), null);
        try {
            provisionVpcTierAllowPortACLRule(network, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (logger.isInfoEnabled()) {
                logger.info("Provisioned ACL rule to open up port {} on {} for Kubernetes cluster {}", CLUSTER_API_PORT, publicIpAddress, kubernetesCluster);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | InvalidParameterValueException | PermissionDeniedException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        } finally {
            CallContext.unregister();
        }
        CallContext.register(CallContext.current(), null);
        try {
            provisionVpcTierAllowPortACLRule(network, DEFAULT_SSH_PORT, DEFAULT_SSH_PORT);
            if (logger.isInfoEnabled()) {
                logger.info("Provisioned ACL rule to open up port {} on {} for Kubernetes cluster {}", DEFAULT_SSH_PORT, publicIpAddress, kubernetesCluster);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | InvalidParameterValueException | PermissionDeniedException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        } finally {
            CallContext.unregister();
        }
    }

    protected void removeVpcTierAclRules(Network network) throws ManagementServerException {
        if (network.getNetworkACLId() == NetworkACL.DEFAULT_ALLOW) {
            return;
        }
        // ACL rule for API access for control node VMs
        try {
            removeVpcTierAllowPortACLRule(network, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (logger.isInfoEnabled()) {
                logger.info("Removed network ACL rule to open up port {} on {} for Kubernetes cluster {}", CLUSTER_API_PORT, publicIpAddress, kubernetesCluster);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove network ACL rule for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        // ACL rule for SSH access for all node VMs
        try {
            removeVpcTierAllowPortACLRule(network, DEFAULT_SSH_PORT, DEFAULT_SSH_PORT);
            if (logger.isInfoEnabled()) {
                logger.info("Removed network ACL rule to open up port {} on {} for Kubernetes cluster {}", DEFAULT_SSH_PORT, publicIpAddress, kubernetesCluster);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove network ACL rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected void setupKubernetesClusterVpcTierRules(IpAddress publicIp, Network network, List<Long> clusterVMIds) throws ManagementServerException {
        // Create ACL rules
        createVpcTierAclRules(network);

        NetworkOffering offering = networkOfferingDao.findById(network.getNetworkOfferingId());
        if (offering.isConserveMode()) {
            // Add load balancing for API access
            try {
                provisionLoadBalancerRule(publicIp, network, owner, clusterVMIds, CLUSTER_API_PORT);
            } catch (InsufficientAddressCapacityException e) {
                throw new ManagementServerException(String.format("Failed to activate API load balancing rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
        } else {
            // Add port forwarding for API access
            try {
                provisionPublicIpPortForwardingRule(publicIp, network, owner, clusterVMIds.get(0), CLUSTER_API_PORT, CLUSTER_API_PORT);
            } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
                throw new ManagementServerException(String.format("Failed to activate API port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
        }

        // Add port forwarding rule for SSH access on each node VM
        try {
            Map<Long, Integer> vmIdPortMap = getVmPortMap();
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds, vmIdPortMap);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    /**
     * Generates a valid name prefix for Kubernetes cluster nodes.
     *
     * <p>The prefix must comply with Kubernetes naming constraints:
     * <ul>
     *   <li>Maximum 63 characters total</li>
     *   <li>Only lowercase alphanumeric characters and hyphens</li>
     *   <li>Must start with a letter</li>
     *   <li>Must end with an alphanumeric character</li>
     * </ul>
     *
     * <p>The generated prefix is limited to 43 characters to accommodate the full node naming pattern:
     * <pre>{'prefix'}-{'control' | 'node'}-{'11-digit-hash'}</pre>
     *
     * @return A valid node name prefix, truncated if necessary
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names/">Kubernetes "Object Names and IDs" documentation</a>
     */
    protected String getKubernetesClusterNodeNamePrefix() {
        String prefix = kubernetesCluster.getName().toLowerCase();

        if (NetUtils.verifyDomainNameLabel(prefix, true)) {
            return StringUtils.truncate(prefix, MAX_CLUSTER_PREFIX_LENGTH);
        }

        prefix = prefix.replaceAll("[^a-z0-9-]", "");
        if (prefix.isEmpty()) {
            prefix = kubernetesCluster.getUuid();
        }
        return StringUtils.truncate("k8s-" + prefix, MAX_CLUSTER_PREFIX_LENGTH);
    }

    protected String getEtcdNodeNameForCluster() {
        String prefix = kubernetesCluster.getName();
        if (!NetUtils.verifyDomainNameLabel(prefix, true)) {
            prefix = prefix.replaceAll("[^a-zA-Z0-9-]", "");
            if (prefix.isEmpty()) {
                prefix = kubernetesCluster.getUuid();
            }
        }
        prefix = prefix + "-etcd" ;
        if (prefix.length() > 40) {
            prefix = prefix.substring(0, 40);
        }
        return prefix;
    }

    protected KubernetesClusterVO updateKubernetesClusterEntry(final Long cores, final Long memory, final Long size,
                                                               final Long serviceOfferingId, final Boolean autoscaleEnabled,
                                                               final Long minSize, final Long maxSize,
                                                               final KubernetesClusterNodeType nodeType,
                                                               final boolean updateNodeOffering,
                                                               final boolean updateClusterOffering) {
        return Transaction.execute((TransactionCallback<KubernetesClusterVO>) status -> {
            KubernetesClusterVO updatedCluster = kubernetesClusterDao.createForUpdate(kubernetesCluster.getId());

            if (cores != null) {
                updatedCluster.setCores(cores);
            }
            if (memory != null) {
                updatedCluster.setMemory(memory);
            }
            if (size != null) {
                updatedCluster.setNodeCount(size);
            }
            if (updateNodeOffering && serviceOfferingId != null && nodeType != null) {
                if (WORKER == nodeType) {
                    updatedCluster.setWorkerNodeServiceOfferingId(serviceOfferingId);
                } else if (CONTROL == nodeType) {
                    updatedCluster.setControlNodeServiceOfferingId(serviceOfferingId);
                } else if (ETCD == nodeType) {
                    updatedCluster.setEtcdNodeServiceOfferingId(serviceOfferingId);
                }
            }
            if (updateClusterOffering && serviceOfferingId != null) {
                updatedCluster.setServiceOfferingId(serviceOfferingId);
            }
            if (autoscaleEnabled != null) {
                updatedCluster.setAutoscalingEnabled(autoscaleEnabled.booleanValue());
            }
            updatedCluster.setMinSize(minSize);
            updatedCluster.setMaxSize(maxSize);
            kubernetesClusterDao.persist(updatedCluster);
            // Prevent null attributes set by the createForUpdate method
            return kubernetesClusterDao.findById(kubernetesCluster.getId());
        });
    }

    private KubernetesClusterVO updateKubernetesClusterEntry(final Boolean autoscaleEnabled, final Long minSize, final Long maxSize) throws CloudRuntimeException {
        KubernetesClusterVO kubernetesClusterVO = updateKubernetesClusterEntry(null, null, null, null, autoscaleEnabled, minSize, maxSize, null, false, false);
        if (kubernetesClusterVO == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to update Kubernetes cluster",
                    kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        return kubernetesClusterVO;
    }

    protected boolean autoscaleCluster(boolean enable, Long minSize, Long maxSize) {
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.AutoscaleRequested);
        }

        File pkFile = getManagementServerSshPublicKeyFile();
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();

        try {
            if (enable) {
                String command = String.format("sudo /opt/bin/autoscale-kube-cluster -i %s -e -M %d -m %d",
                    kubernetesCluster.getUuid(), maxSize, minSize);
                Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 60000);

                // Maybe the file isn't present. Try and copy it
                if (!result.first()) {
                    logMessage(Level.INFO, "Autoscaling files missing. Adding them now", null);
                    retrieveScriptFiles();
                    copyScripts(publicIpAddress, sshPort);

                    if (!createCloudStackSecret(keys)) {
                        logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup keys for Kubernetes cluster %s",
                            kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                    }

                    // If at first you don't succeed ...
                    result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                        pkFile, null, command, 10000, 10000, 60000);
                    if (!result.first()) {
                        throw new CloudRuntimeException(result.second());
                    }
                }
                updateKubernetesClusterEntry(true, minSize, maxSize);
            } else {
                Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                    pkFile, null, String.format("sudo /opt/bin/autoscale-kube-cluster -d"),
                        10000, 10000, 60000);
                if (!result.first()) {
                    throw new CloudRuntimeException(result.second());
                }
                updateKubernetesClusterEntry(false, null, null);
            }
            return true;
        } catch (Exception e) {
            String msg = String.format("Failed to autoscale Kubernetes cluster: %s : %s", kubernetesCluster.getName(), e.getMessage());
            logAndThrow(Level.ERROR, msg);
            return false;
        } finally {
            // Deploying the autoscaler might fail but it can be deployed manually too, so no need to go to an alert state
            updateLoginUserDetails(null);
        }
    }

    protected List<DedicatedResourceVO> listDedicatedHostsInDomain(Long domainId) {
        return dedicatedResourceDao.listByDomainId(domainId);
    }

    public boolean deletePVsWithReclaimPolicyDelete() {
        File pkFile = getManagementServerSshPublicKeyFile();
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        try {
            String command = String.format("sudo %s/%s", scriptPath, deletePvScriptFilename);
            logMessage(Level.INFO, "Starting PV deletion script for cluster: " + kubernetesCluster.getName(), null);
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 600000); // 10 minute timeout
            if (Boolean.FALSE.equals(result.first())) {
                logMessage(Level.INFO, "PV delete script missing. Adding it now", null);
                retrieveScriptFiles();
                if (deletePvScriptFile != null) {
                    copyScriptFile(publicIpAddress, sshPort, deletePvScriptFile, deletePvScriptFilename);
                    logMessage(Level.INFO, "Executing PV deletion script (this may take several minutes)...", null);
                    result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                            pkFile, null, command, 10000, 10000, 600000); // 10 minute timeout
                    if (Boolean.FALSE.equals(result.first())) {
                        logMessage(Level.ERROR, "PV deletion script failed: " + result.second(), null);
                        throw new CloudRuntimeException(result.second());
                    }
                    logMessage(Level.INFO, "PV deletion script completed successfully", null);
                } else {
                    logMessage(Level.WARN, "PV delete script file not found in resources, skipping PV deletion", null);
                    return false;
                }
            } else {
                logMessage(Level.INFO, "PV deletion script completed successfully", null);
            }

            if (result.second() != null && !result.second().trim().isEmpty()) {
                logMessage(Level.INFO, "PV deletion script output: " + result.second(), null);
            }

            return true;
        } catch (Exception e) {
            String msg = String.format("Failed to delete PVs with reclaimPolicy=Delete: %s : %s", kubernetesCluster.getName(), e.getMessage());
            logMessage(Level.WARN, msg, e);
            return false;
        }
    }
}
