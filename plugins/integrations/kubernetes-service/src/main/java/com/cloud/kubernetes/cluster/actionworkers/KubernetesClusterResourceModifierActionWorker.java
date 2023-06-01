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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
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
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
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
import com.cloud.user.SSHKeyPairVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;
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
    protected FirewallService firewallService;
    @Inject
    protected NetworkACLService networkACLService;
    @Inject
    protected  NetworkACLItemDao networkACLItemDao;
    @Inject
    protected LoadBalancingRulesService lbService;
    @Inject
    protected RulesService rulesService;
    @Inject
    protected PortForwardingRulesDao portForwardingRulesDao;
    @Inject
    protected ResourceManager resourceManager;
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

    protected String kubernetesClusterNodeNamePrefix;

    protected KubernetesClusterResourceModifierActionWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    protected void init() {
        super.init();
        kubernetesClusterNodeNamePrefix = getKubernetesClusterNodeNamePrefix();
    }

    private String getKubernetesNodeConfig(final String joinIp, final boolean ejectIso) throws IOException {
        String k8sNodeConfig = readResourceFile("/conf/k8s-node.yml");
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String joinIpKey = "{{ k8s_control_node.join_ip }}";
        final String clusterTokenKey = "{{ k8s_control_node.cluster.token }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (StringUtils.isNotEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n      - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sNodeConfig = k8sNodeConfig.replace(sshPubKey, pubKey);
        k8sNodeConfig = k8sNodeConfig.replace(joinIpKey, joinIp);
        k8sNodeConfig = k8sNodeConfig.replace(clusterTokenKey, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sNodeConfig = k8sNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));

        k8sNodeConfig = updateKubeConfigWithRegistryDetails(k8sNodeConfig);

        return k8sNodeConfig;
    }

    protected String updateKubeConfigWithRegistryDetails(String k8sConfig) {
        /* genarate /etc/containerd/config.toml file on the nodes only if Kubernetes cluster is created to
         * use docker private registry */
        String registryUsername = null;
        String registryPassword = null;
        String registryUrl = null;

        List<KubernetesClusterDetailsVO> details = kubernetesClusterDetailsDao.listDetails(kubernetesCluster.getId());
        for (KubernetesClusterDetailsVO detail : details) {
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_USER_NAME)) {
                registryUsername = detail.getValue();
            }
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_PASSWORD)) {
                registryPassword = detail.getValue();
            }
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_URL)) {
                registryUrl = detail.getValue();
            }
        }

        if (StringUtils.isNoneEmpty(registryUsername, registryPassword, registryUrl)) {
            // Update runcmd in the cloud-init configuration to run a script that updates the containerd config with provided registry details
            String runCmd = "- bash -x /opt/bin/setup-containerd";

            String registryEp = registryUrl.split("://")[1];
            k8sConfig = k8sConfig.replace("- containerd config default > /etc/containerd/config.toml", runCmd);
            final String registryUrlKey = "{{registry.url}}";
            final String registryUrlEpKey = "{{registry.url.endpoint}}";
            final String registryAuthKey = "{{registry.token}}";
            final String registryUname = "{{registry.username}}";
            final String registryPsswd = "{{registry.password}}";

            final String usernamePasswordKey = registryUsername + ":" + registryPassword;
            String base64Auth = Base64.encodeBase64String(usernamePasswordKey.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            k8sConfig = k8sConfig.replace(registryUrlKey,   registryUrl);
            k8sConfig = k8sConfig.replace(registryUrlEpKey, registryEp);
            k8sConfig = k8sConfig.replace(registryUname, registryUsername);
            k8sConfig = k8sConfig.replace(registryPsswd, registryPassword);
            k8sConfig = k8sConfig.replace(registryAuthKey, base64Auth);
        }
        return k8sConfig;
    }
    protected DeployDestination plan(final long nodesCount, final DataCenter zone, final ServiceOffering offering) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        List<HostVO> hosts = resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, zone.getId());
        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
            hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found = false;
        for (int i = 1; i <= nodesCount; i++) {
            suitable_host_found = false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO h = hp.first();
                if (!h.getHypervisorType().equals(clusterTemplate.getHypervisorType())) {
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
                if (capacityManager.checkIfHostHasCapacity(h.getId(), cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Found host : %s for with enough capacity, CPU=%d RAM=%s", h.getName(), cpu_requested * reserved, toHumanReadableSize(ram_requested * reserved)));
                    }
                    hostEntry.setValue(new Pair<HostVO, Integer>(h, reserved));
                    suitable_host_found = true;
                    break;
                }
            }
            if (!suitable_host_found) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Suitable hosts not found in datacenter : %s for node %d, with offering : %s and hypervisor: %s",
                        zone.getName(), i, offering.getName(), clusterTemplate.getHypervisorType().toString()));
                }
                break;
            }
        }
        if (suitable_host_found) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Suitable hosts found in datacenter : %s, creating deployment destination", zone.getName()));
            }
            return new DeployDestination(zone, null, null, null);
        }
        String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%d memory=%s) with offering : %s and hypervisor: %s",
                cpu_requested * nodesCount, toHumanReadableSize(ram_requested * nodesCount), offering.getName(), clusterTemplate.getHypervisorType().toString());

        logger.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, zone.getId());
    }

    protected DeployDestination plan() throws InsufficientServerCapacityException {
        ServiceOffering offering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Checking deployment destination for Kubernetes cluster : %s in zone : %s", kubernetesCluster.getName(), zone.getName()));
        }
        return plan(kubernetesCluster.getTotalNodeCount(), zone, offering);
    }

    protected void resizeNodeVolume(final UserVm vm) throws ManagementServerException {
        try {
            if (vm.getHypervisorType() == Hypervisor.HypervisorType.VMware && templateDao.findById(vm.getTemplateId()).isDeployAsIs()) {
                List<VolumeVO> vmVols = volumeDao.findByInstance(vm.getId());
                for (VolumeVO volumeVO : vmVols) {
                    if (volumeVO.getVolumeType() == Volume.Type.ROOT) {
                        ResizeVolumeCmd resizeVolumeCmd = new ResizeVolumeCmd();
                        resizeVolumeCmd = ComponentContext.inject(resizeVolumeCmd);
                        Field f = resizeVolumeCmd.getClass().getDeclaredField("size");
                        Field f1 = resizeVolumeCmd.getClass().getDeclaredField("id");
                        f.setAccessible(true);
                        f1.setAccessible(true);
                        f1.set(resizeVolumeCmd, volumeVO.getId());
                        f.set(resizeVolumeCmd, kubernetesCluster.getNodeRootDiskSize());
                        volumeService.resizeVolume(resizeVolumeCmd);
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ManagementServerException(String.format("Failed to resize volume of  VM in the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected void startKubernetesVM(final UserVm vm) throws ManagementServerException {
        try {
            StartVMCmd startVm = new StartVMCmd();
            startVm = ComponentContext.inject(startVm);
            Field f = startVm.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(startVm, vm.getId());
            itMgr.advanceStart(vm.getUuid(), null, null);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Started VM : %s in the Kubernetes cluster : %s", vm.getDisplayName(), kubernetesCluster.getName()));
            }
        } catch (IllegalAccessException | NoSuchFieldException | OperationTimedoutException | ResourceUnavailableException | InsufficientCapacityException ex) {
            throw new ManagementServerException(String.format("Failed to start VM in the Kubernetes cluster : %s", kubernetesCluster.getName()), ex);
        }

        UserVm startVm = userVmDao.findById(vm.getId());
        if (!startVm.getState().equals(VirtualMachine.State.Running)) {
            throw new ManagementServerException(String.format("Failed to start VM in the Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
    }

    protected List<UserVm> provisionKubernetesClusterNodeVms(final long nodeCount, final int offset, final String publicIpAddress) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        List<UserVm> nodes = new ArrayList<>();
        for (int i = offset + 1; i <= nodeCount; i++) {
            UserVm vm = createKubernetesNode(publicIpAddress);
            addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId(), false);
            if (kubernetesCluster.getNodeRootDiskSize() > 0) {
                resizeNodeVolume(vm);
            }
            startKubernetesVM(vm);
            vm = userVmDao.findById(vm.getId());
            if (vm == null) {
                throw new ManagementServerException(String.format("Failed to provision worker VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
            }
            nodes.add(vm);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned node VM : %s in to the Kubernetes cluster : %s", vm.getDisplayName(), kubernetesCluster.getName()));
            }
        }
        return nodes;
    }

    protected List<UserVm> provisionKubernetesClusterNodeVms(final long nodeCount, final String publicIpAddress) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        return provisionKubernetesClusterNodeVms(nodeCount, 0, publicIpAddress);
    }

    protected UserVm createKubernetesNode(String joinIp) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm nodeVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
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
            k8sNodeConfig = getKubernetesNodeConfig(joinIp, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes node configuration file", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        List<String> keypairs = new ArrayList<String>();
        if (StringUtils.isNotBlank(kubernetesCluster.getKeyPair())) {
            keypairs.add(kubernetesCluster.getKeyPair());
        }
        if (zone.isSecurityGroupEnabled()) {
            List<Long> securityGroupIds = new ArrayList<>();
            securityGroupIds.add(kubernetesCluster.getSecurityGroupId());
            nodeVm = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, clusterTemplate, networkIds, securityGroupIds, owner,
                    hostName, hostName, null, null, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST,base64UserData, null, null, keypairs,
                    null, addrs, null, null, null, customParameterMap, null, null, null,
                    null, true, null, UserVmManager.CKS_NODE);
        } else {
            nodeVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, clusterTemplate, networkIds, owner,
                    hostName, hostName, null, null, null,
                    Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                    null, addrs, null, null, null, customParameterMap, null, null, null, null, true, UserVmManager.CKS_NODE, null);
        }
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Created node VM : %s, %s in the Kubernetes cluster : %s", hostName, nodeVm.getUuid(), kubernetesCluster.getName()));
        }
        return nodeVm;
    }

    protected void provisionFirewallRules(final IpAddress publicIp, final Account account, int startPort, int endPort) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException, NetworkRuleConflictException {
        List<String> sourceCidrList = new ArrayList<String>();
        sourceCidrList.add("0.0.0.0/0");

        CreateFirewallRuleCmd rule = new CreateFirewallRuleCmd();
        rule = ComponentContext.inject(rule);

        Field addressField = rule.getClass().getDeclaredField("ipAddressId");
        addressField.setAccessible(true);
        addressField.set(rule, publicIp.getId());

        Field protocolField = rule.getClass().getDeclaredField("protocol");
        protocolField.setAccessible(true);
        protocolField.set(rule, "TCP");

        Field startPortField = rule.getClass().getDeclaredField("publicStartPort");
        startPortField.setAccessible(true);
        startPortField.set(rule, startPort);

        Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
        endPortField.setAccessible(true);
        endPortField.set(rule, endPort);

        Field cidrField = rule.getClass().getDeclaredField("cidrlist");
        cidrField.setAccessible(true);
        cidrField.set(rule, sourceCidrList);

        firewallService.createIngressFirewallRule(rule);
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
        PortForwardingRuleVO pfRule = Transaction.execute((TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>) status -> {
            PortForwardingRuleVO newRule =
                    new PortForwardingRuleVO(null, publicIpId,
                            sourcePort, sourcePort,
                            vmIp,
                            destPort, destPort,
                            "tcp", networkId, accountId, domainId, vmId);
            newRule.setDisplay(true);
            newRule.setState(FirewallRule.State.Add);
            newRule = portForwardingRulesDao.persist(newRule);
            return newRule;
        });
        rulesService.applyPortForwardingRules(publicIp.getId(), account);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Provisioned SSH port forwarding rule: %s from port %d to %d on %s to the VM IP : %s in Kubernetes cluster : %s", pfRule.getUuid(), sourcePort, destPort, publicIp.getAddress().addr(), vmIp.toString(), kubernetesCluster.getName()));
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
                                                   List<Long> clusterVMIds) throws ResourceUnavailableException,
            NetworkRuleConflictException {
        if (!CollectionUtils.isEmpty(clusterVMIds)) {
            for (int i = 0; i < clusterVMIds.size(); ++i) {
                provisionPublicIpPortForwardingRule(publicIp, network, account, clusterVMIds.get(i), CLUSTER_NODES_DEFAULT_START_SSH_PORT + i, DEFAULT_SSH_PORT);
            }
        }
    }

    protected FirewallRule removeApiFirewallRule(final IpAddress publicIp) {
        FirewallRule rule = null;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO firewallRule : firewallRules) {
            if (firewallRule.getSourcePortStart() == CLUSTER_API_PORT &&
                    firewallRule.getSourcePortEnd() == CLUSTER_API_PORT) {
                rule = firewallRule;
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                break;
            }
        }
        return rule;
    }

    protected FirewallRule removeSshFirewallRule(final IpAddress publicIp) {
        FirewallRule rule = null;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO firewallRule : firewallRules) {
            if (firewallRule.getSourcePortStart() == CLUSTER_NODES_DEFAULT_START_SSH_PORT) {
                rule = firewallRule;
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                break;
            }
        }
        return rule;
    }

    protected void removePortForwardingRules(final IpAddress publicIp, final Network network, final Account account, final List<Long> removedVMIds) throws ResourceUnavailableException {
        if (!CollectionUtils.isEmpty(removedVMIds)) {
            for (Long vmId : removedVMIds) {
                List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByNetwork(network.getId());
                for (PortForwardingRuleVO pfRule : pfRules) {
                    if (pfRule.getVirtualMachineId() == vmId) {
                        portForwardingRulesDao.remove(pfRule.getId());
                        break;
                    }
                }
            }
            rulesService.applyPortForwardingRules(publicIp.getId(), account);
        }
    }

    protected void removePortForwardingRules(final IpAddress publicIp, final Network network, final Account account, int startPort, int endPort)
        throws ResourceUnavailableException {
        List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByNetwork(network.getId());
        for (PortForwardingRuleVO pfRule : pfRules) {
            if (startPort <= pfRule.getSourcePortStart() && pfRule.getSourcePortStart() <= endPort) {
                portForwardingRulesDao.remove(pfRule.getId());
            }
        }
        rulesService.applyPortForwardingRules(publicIp.getId(), account);
    }

    protected void removeLoadBalancingRule(final IpAddress publicIp, final Network network,
                                           final Account account) throws ResourceUnavailableException {
        List<LoadBalancerVO> rules = loadBalancerDao.listByIpAddress(publicIp.getId());
        for (LoadBalancerVO rule : rules) {
            if (rule.getNetworkId() == network.getId() &&
                    rule.getAccountId() == account.getId() &&
                    rule.getSourcePortStart() == CLUSTER_API_PORT &&
                    rule.getSourcePortEnd() == CLUSTER_API_PORT) {
                lbService.deleteLoadBalancerRule(rule.getId(), true);
                break;
            }
        }
    }

    protected void provisionVpcTierAllowPortACLRule(final Network network, int startPort, int endPorts) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException {
        List<NetworkACLItemVO> aclItems = networkACLItemDao.listByACL(network.getNetworkACLId());
        aclItems = aclItems.stream().filter(x -> !NetworkACLItem.State.Revoke.equals(x.getState())).collect(Collectors.toList());
        CreateNetworkACLCmd rule = new CreateNetworkACLCmd();
        rule = ComponentContext.inject(rule);
        Map<String, Object> fieldValues = Map.of(
                "protocol", "TCP",
                "publicStartPort", startPort,
                "publicEndPort", endPorts,
                "trafficType", NetworkACLItem.TrafficType.Ingress.toString(),
                "networkId", network.getId(),
                "aclId", network.getNetworkACLId(),
                "action", NetworkACLItem.Action.Allow.toString()
        );
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            Field field = rule.getClass().getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(rule, entry.getValue());
        }
        NetworkACLItem aclRule = networkACLService.createNetworkACLItem(rule);
        networkACLService.moveRuleToTheTopInACLList(aclRule);
        networkACLService.applyNetworkACL(aclRule.getAclId());
    }

    protected void removeVpcTierAllowPortACLRule(final Network network, int startPort, int endPort) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException {
        List<NetworkACLItemVO> aclItems = networkACLItemDao.listByACL(network.getNetworkACLId());
        aclItems = aclItems.stream().filter(x -> (x.getProtocol() != null &&
                        x.getProtocol().equals("TCP") &&
                        x.getSourcePortStart() != null &&
                        x.getSourcePortStart().equals(startPort) &&
                        x.getSourcePortEnd() != null &&
                        x.getSourcePortEnd().equals(endPort) &&
                        x.getAction().equals(NetworkACLItem.Action.Allow)))
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

    protected void createFirewallRules(IpAddress publicIp, List<Long> clusterVMIds, boolean apiRule) throws ManagementServerException {
        // Firewall rule for SSH access on each node VM
        try {
            int endPort = CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMIds.size() - 1;
            provisionFirewallRules(publicIp, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Kubernetes cluster : %s", CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort, publicIp.getAddress().addr(), kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        if (!apiRule) {
            return;
        }
        // Firewall rule for API access for control node VMs
        try {
            provisionFirewallRules(publicIp, owner, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned firewall rule to open up port %d on %s for Kubernetes cluster %s",
                        CLUSTER_API_PORT, publicIp.getAddress().addr(), kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
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
        createFirewallRules(publicIp, clusterVMIds, apiRule);

        // Port forwarding rule for SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds);
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
        try {
            provisionVpcTierAllowPortACLRule(network, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned ACL rule to open up port %d on %s for Kubernetes cluster %s",
                        CLUSTER_API_PORT, publicIpAddress, kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | InvalidParameterValueException | PermissionDeniedException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        try {
            provisionVpcTierAllowPortACLRule(network, DEFAULT_SSH_PORT, DEFAULT_SSH_PORT);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned ACL rule to open up port %d on %s for Kubernetes cluster %s",
                        DEFAULT_SSH_PORT, publicIpAddress, kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | InvalidParameterValueException | PermissionDeniedException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
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
                logger.info(String.format("Removed network ACL rule to open up port %d on %s for Kubernetes cluster %s",
                        CLUSTER_API_PORT, publicIpAddress, kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove network ACL rule for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        // ACL rule for SSH access for all node VMs
        try {
            removeVpcTierAllowPortACLRule(network, DEFAULT_SSH_PORT, DEFAULT_SSH_PORT);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Removed network ACL rule to open up port %d on %s for Kubernetes cluster %s",
                        DEFAULT_SSH_PORT, publicIpAddress, kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove network ACL rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected void setupKubernetesClusterVpcTierRules(IpAddress publicIp, Network network, List<Long> clusterVMIds) throws ManagementServerException {
        // Create ACL rules
        createVpcTierAclRules(network);
        // Add port forwarding for API access
        try {
            provisionPublicIpPortForwardingRule(publicIp, network, owner, clusterVMIds.get(0), CLUSTER_API_PORT, CLUSTER_API_PORT);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate API port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        // Add port forwarding rule for SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    protected String getKubernetesClusterNodeNamePrefix() {
        String prefix = kubernetesCluster.getName();
        if (!NetUtils.verifyDomainNameLabel(prefix, true)) {
            prefix = prefix.replaceAll("[^a-zA-Z0-9-]", "");
            if (prefix.length() == 0) {
                prefix = kubernetesCluster.getUuid();
            }
            prefix = "k8s-" + prefix;
        }
        if (prefix.length() > 40) {
            prefix = prefix.substring(0, 40);
        }
        return prefix;
    }

    protected KubernetesClusterVO updateKubernetesClusterEntry(final Long cores, final Long memory,
        final Long size, final Long serviceOfferingId, final Boolean autoscaleEnabled, final Long minSize, final Long maxSize) {
        return Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
                @Override
                public KubernetesClusterVO doInTransaction(TransactionStatus status) {
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
                if (serviceOfferingId != null) {
                    updatedCluster.setServiceOfferingId(serviceOfferingId);
                }
                if (autoscaleEnabled != null) {
                    updatedCluster.setAutoscalingEnabled(autoscaleEnabled.booleanValue());
                }
                updatedCluster.setMinSize(minSize);
                updatedCluster.setMaxSize(maxSize);
                return kubernetesClusterDao.persist(updatedCluster);
            }
        });
    }

    private KubernetesClusterVO updateKubernetesClusterEntry(final Boolean autoscaleEnabled, final Long minSize, final Long maxSize) throws CloudRuntimeException {
        KubernetesClusterVO kubernetesClusterVO = updateKubernetesClusterEntry(null, null, null, null, autoscaleEnabled, minSize, maxSize);
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
}
