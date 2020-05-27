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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.google.common.base.Strings;

public class KubernetesClusterStartWorker extends KubernetesClusterResourceModifierActionWorker {

    public KubernetesClusterStartWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    private Pair<String, Map<Long, Network.IpAddresses>> getKubernetesMasterIpAddresses(final DataCenter zone, final Network network, final Account account) throws InsufficientAddressCapacityException {
        String masterIp = null;
        Map<Long, Network.IpAddresses> requestedIps = null;
        if (Network.GuestType.Shared.equals(network.getGuestType())) {
            List<Long> vlanIds = new ArrayList<>();
            List<VlanVO> vlans = vlanDao.listVlansByNetworkId(network.getId());
            for (VlanVO vlan : vlans) {
                vlanIds.add(vlan.getId());
            }
            PublicIp ip = ipAddressManager.getAvailablePublicIpAddressFromVlans(zone.getId(), null, account, Vlan.VlanType.DirectAttached, vlanIds,network.getId(), null, false);
            if (ip != null) {
                masterIp = ip.getAddress().toString();
            }
            requestedIps = new HashMap<>();
            Ip ipAddress = ip.getAddress();
            boolean isIp6 = ipAddress.isIp6();
            requestedIps.put(network.getId(), new Network.IpAddresses(ipAddress.isIp4() ? ip.getAddress().addr() : null, null));
        } else {
            masterIp = ipAddressManager.acquireGuestIpAddress(networkDao.findById(kubernetesCluster.getNetworkId()), null);
        }
        return new Pair<>(masterIp, requestedIps);
    }

    private boolean isKubernetesVersionSupportsHA() {
        boolean haSupported = false;
        final KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version != null) {
            try {
                if (KubernetesVersionManagerImpl.compareSemanticVersions(version.getSemanticVersion(), KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT) >= 0) {
                    haSupported = true;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Unable to compare Kubernetes version for cluster version ID: %s with %s", version.getUuid(), KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }
        return haSupported;
    }

    private String getKubernetesMasterConfig(final String masterIp, final String serverIp,
                                             final String hostName, final boolean haSupported,
                                             final boolean ejectIso) throws IOException {
        String k8sMasterConfig = readResourceFile("/conf/k8s-master.yml");
        final String apiServerCert = "{{ k8s_master.apiserver.crt }}";
        final String apiServerKey = "{{ k8s_master.apiserver.key }}";
        final String caCert = "{{ k8s_master.ca.crt }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterToken = "{{ k8s_master.cluster.token }}";
        final String clusterInitArgsKey = "{{ k8s_master.cluster.initargs }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final List<String> addresses = new ArrayList<>();
        addresses.add(masterIp);
        if (!serverIp.equals(masterIp)) {
            addresses.add(serverIp);
        }
        final Certificate certificate = caManager.issueCertificate(null, Arrays.asList(hostName, "kubernetes",
                "kubernetes.default", "kubernetes.default.svc", "kubernetes.default.svc.cluster", "kubernetes.default.svc.cluster.local"),
                addresses, 3650, null);
        final String tlsClientCert = CertUtils.x509CertificateToPem(certificate.getClientCertificate());
        final String tlsPrivateKey = CertUtils.privateKeyToPem(certificate.getPrivateKey());
        final String tlsCaCert = CertUtils.x509CertificatesToPem(certificate.getCaCertificates());
        k8sMasterConfig = k8sMasterConfig.replace(apiServerCert, tlsClientCert.replace("\n", "\n      "));
        k8sMasterConfig = k8sMasterConfig.replace(apiServerKey, tlsPrivateKey.replace("\n", "\n      "));
        k8sMasterConfig = k8sMasterConfig.replace(caCert, tlsCaCert.replace("\n", "\n      "));
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (!Strings.isNullOrEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sMasterConfig = k8sMasterConfig.replace(sshPubKey, pubKey);
        k8sMasterConfig = k8sMasterConfig.replace(clusterToken, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        String initArgs = "";
        if (haSupported) {
            initArgs = String.format("--control-plane-endpoint %s:%d --upload-certs --certificate-key %s ",
                    serverIp,
                    CLUSTER_API_PORT,
                    KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        }
        initArgs += String.format("--apiserver-cert-extra-sans=%s", serverIp);
        k8sMasterConfig = k8sMasterConfig.replace(clusterInitArgsKey, initArgs);
        k8sMasterConfig = k8sMasterConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        return k8sMasterConfig;
    }

    private UserVm createKubernetesMaster(final Network network, String serverIp) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm masterVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        VirtualMachineTemplate template = templateDao.findById(kubernetesCluster.getTemplateId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Pair<String, Map<Long, Network.IpAddresses>> ipAddresses = getKubernetesMasterIpAddresses(zone, network, owner);
        String masterIp = ipAddresses.first();
        Map<Long, Network.IpAddresses> requestedIps = ipAddresses.second();
        if (Network.GuestType.Shared.equals(network.getGuestType()) && Strings.isNullOrEmpty(serverIp)) {
            serverIp = masterIp;
        }
        Network.IpAddresses addrs = new Network.IpAddresses(masterIp, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = kubernetesClusterNodeNamePrefix + "-master";
        if (kubernetesCluster.getMasterNodeCount() > 1) {
            hostName += "-1";
        }
        hostName = getKubernetesClusterNodeAvailableName(hostName);
        boolean haSupported = isKubernetesVersionSupportsHA();
        String k8sMasterConfig = null;
        try {
            k8sMasterConfig = getKubernetesMasterConfig(masterIp, serverIp, hostName, haSupported, Hypervisor.HypervisorType.VMware.equals(template.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes master configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sMasterConfig.getBytes(StringUtils.getPreferredCharset()));
        masterVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, hostName, null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                requestedIps, addrs, null, null, null, customParameterMap, null, null, null, null);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Created master VM ID: %s, %s in the Kubernetes cluster ID: %s", masterVm.getUuid(), hostName, kubernetesCluster.getUuid()));
        }
        return masterVm;
    }

    private String getKubernetesAdditionalMasterConfig(final String joinIp, final boolean ejectIso) throws IOException {
        String k8sMasterConfig = readResourceFile("/conf/k8s-master-add.yml");
        final String joinIpKey = "{{ k8s_master.join_ip }}";
        final String clusterTokenKey = "{{ k8s_master.cluster.token }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterHACertificateKey = "{{ k8s_master.cluster.ha.certificate.key }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (!Strings.isNullOrEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sMasterConfig = k8sMasterConfig.replace(sshPubKey, pubKey);
        k8sMasterConfig = k8sMasterConfig.replace(joinIpKey, joinIp);
        k8sMasterConfig = k8sMasterConfig.replace(clusterTokenKey, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sMasterConfig = k8sMasterConfig.replace(clusterHACertificateKey, KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        k8sMasterConfig = k8sMasterConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        return k8sMasterConfig;
    }

    private UserVm createKubernetesAdditionalMaster(final String joinIp, final int additionalMasterNodeInstance) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm additionalMasterVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        VirtualMachineTemplate template = templateDao.findById(kubernetesCluster.getTemplateId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = getKubernetesClusterNodeAvailableName(String.format("%s-master-%d", kubernetesClusterNodeNamePrefix, additionalMasterNodeInstance + 1));
        String k8sMasterConfig = null;
        try {
            k8sMasterConfig = getKubernetesAdditionalMasterConfig(joinIp, Hypervisor.HypervisorType.VMware.equals(template.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes master configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sMasterConfig.getBytes(StringUtils.getPreferredCharset()));
        additionalMasterVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, hostName, null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                null, addrs, null, null, null, customParameterMap, null, null, null, null);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Created master VM ID: %s, %s in the Kubernetes cluster ID: %s", additionalMasterVm.getUuid(), hostName, kubernetesCluster.getUuid()));
        }
        return additionalMasterVm;
    }

    private UserVm provisionKubernetesClusterMasterVm(final Network network, final String publicIpAddress) throws
            ManagementServerException, InsufficientCapacityException, ResourceUnavailableException {
        UserVm k8sMasterVM = null;
        k8sMasterVM = createKubernetesMaster(network, publicIpAddress);
        addKubernetesClusterVm(kubernetesCluster.getId(), k8sMasterVM.getId());
        startKubernetesVM(k8sMasterVM);
        k8sMasterVM = userVmDao.findById(k8sMasterVM.getId());
        if (k8sMasterVM == null) {
            throw new ManagementServerException(String.format("Failed to provision master VM for Kubernetes cluster ID: %s" , kubernetesCluster.getUuid()));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Provisioned the master VM ID: %s in to the Kubernetes cluster ID: %s", k8sMasterVM.getUuid(), kubernetesCluster.getUuid()));
        }
        return k8sMasterVM;
    }

    private List<UserVm> provisionKubernetesClusterAdditionalMasterVms(final String publicIpAddress) throws
            InsufficientCapacityException, ManagementServerException, ResourceUnavailableException {
        List<UserVm> additionalMasters = new ArrayList<>();
        if (kubernetesCluster.getMasterNodeCount() > 1) {
            for (int i = 1; i < kubernetesCluster.getMasterNodeCount(); i++) {
                UserVm vm = null;
                vm = createKubernetesAdditionalMaster(publicIpAddress, i);
                addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId());
                startKubernetesVM(vm);
                vm = userVmDao.findById(vm.getId());
                if (vm == null) {
                    throw new ManagementServerException(String.format("Failed to provision additional master VM for Kubernetes cluster ID: %s" , kubernetesCluster.getUuid()));
                }
                additionalMasters.add(vm);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Provisioned additional master VM ID: %s in to the Kubernetes cluster ID: %s", vm.getUuid(), kubernetesCluster.getUuid()));
                }
            }
        }
        return additionalMasters;
    }

    private Network startKubernetesClusterNetwork(final DeployDestination destination) throws ManagementServerException {
        final ReservationContext context = new ReservationContextImpl(null, null, null, owner);
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            String msg  = String.format("Network for Kubernetes cluster ID: %s not found", kubernetesCluster.getUuid());
            LOGGER.warn(msg);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        try {
            networkMgr.startNetwork(network.getId(), destination, context);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Network ID: %s is started for the  Kubernetes cluster ID: %s", network.getUuid(), kubernetesCluster.getUuid()));
            }
        } catch (ConcurrentOperationException | ResourceUnavailableException |InsufficientCapacityException e) {
            String msg = String.format("Failed to start Kubernetes cluster ID: %s as unable to start associated network ID: %s" , kubernetesCluster.getUuid(), network.getUuid());
            LOGGER.error(msg, e);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }
        return network;
    }

    private void provisionLoadBalancerRule(final IpAddress publicIp, final Network network,
                                           final Account account, final List<Long> clusterVMIds, final int port) throws NetworkRuleConflictException,
            InsufficientAddressCapacityException {
        LoadBalancer lb = lbService.createPublicLoadBalancerRule(null, "api-lb", "LB rule for API access",
                port, port, port, port,
                publicIp.getId(), NetUtils.TCP_PROTO, "roundrobin", network.getId(),
                account.getId(), false, NetUtils.TCP_PROTO, true);

        Map<Long, List<String>> vmIdIpMap = new HashMap<>();
        for (int i = 0; i < kubernetesCluster.getMasterNodeCount(); ++i) {
            List<String> ips = new ArrayList<>();
            Nic masterVmNic = networkModel.getNicInNetwork(clusterVMIds.get(i), kubernetesCluster.getNetworkId());
            ips.add(masterVmNic.getIPv4Address());
            vmIdIpMap.put(clusterVMIds.get(i), ips);
        }
        lbService.assignToLoadBalancer(lb.getId(), null, vmIdIpMap);
    }

    /**
     * Setup network rules for Kubernetes cluster
     * Open up firewall port CLUSTER_API_PORT, secure port on which Kubernetes
     * API server is running. Also create load balancing rule to forward public
     * IP traffic to master VMs' private IP.
     * Open up  firewall ports NODES_DEFAULT_START_SSH_PORT to NODES_DEFAULT_START_SSH_PORT+n
     * for SSH access. Also create port-forwarding rule to forward public IP traffic to all
     * @param network
     * @param clusterVMs
     * @throws ManagementServerException
     */
    private void setupKubernetesClusterNetworkRules(Network network, List<UserVm> clusterVMs) throws ManagementServerException {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Network ID: %s for Kubernetes cluster ID: %s is not an isolated network, therefore, no need for network rules", network.getUuid(), kubernetesCluster.getUuid()));
            }
            return;
        }
        List<Long> clusterVMIds = new ArrayList<>();
        for (UserVm vm : clusterVMs) {
            clusterVMIds.add(vm.getId());
        }
        IpAddress publicIp = getSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network ID: %s, Kubernetes cluster ID: %s", network.getUuid(), kubernetesCluster.getUuid()));
        }

        try {
            provisionFirewallRules(publicIp, owner, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Provisioned firewall rule to open up port %d on %s for Kubernetes cluster ID: %s",
                        CLUSTER_API_PORT, publicIp.getAddress().addr(), kubernetesCluster.getUuid()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }

        try {
            int endPort = CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMs.size() - 1;
            provisionFirewallRules(publicIp, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Kubernetes cluster ID: %s", CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort, publicIp.getAddress().addr(), kubernetesCluster.getUuid()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for SSH access for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }

        // Load balancer rule fo API access for master node VMs
        try {
            provisionLoadBalancerRule(publicIp, network, owner, clusterVMIds, CLUSTER_API_PORT);
        } catch (NetworkRuleConflictException | InsufficientAddressCapacityException e) {
            throw new ManagementServerException(String.format("Failed to provision load balancer rule for API access for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }

        // Port forwarding rule fo SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds, CLUSTER_NODES_DEFAULT_START_SSH_PORT);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }
    }

    private void startKubernetesClusterVMs() {
        List <UserVm> clusterVms = getKubernetesClusterVMs();
        for (final UserVm vm : clusterVms) {
            if (vm == null) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            try {
                startKubernetesVM(vm);
            } catch (ManagementServerException ex) {
                LOGGER.warn(String.format("Failed to start VM ID: %s in Kubernetes cluster ID: %s due to ", vm.getUuid(), kubernetesCluster.getUuid()) + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }
        for (final UserVm userVm : clusterVms) {
            UserVm vm = userVmDao.findById(userVm.getId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }
    }

    private boolean isKubernetesClusterKubeConfigAvailable(final long timeoutTime) {
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            KubernetesClusterDetailsVO kubeConfigDetail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "kubeConfigData");
            if (kubeConfigDetail != null && !Strings.isNullOrEmpty(kubeConfigDetail.getValue())) {
                return true;
            }
        }
        String kubeConfig = KubernetesClusterUtil.getKubernetesClusterConfig(kubernetesCluster, publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, sshKeyFile, timeoutTime);
        if (!Strings.isNullOrEmpty(kubeConfig)) {
            final String masterVMPrivateIpAddress = getMasterVmPrivateIp();
            if (!Strings.isNullOrEmpty(masterVMPrivateIpAddress)) {
                kubeConfig = kubeConfig.replace(String.format("server: https://%s:%d", masterVMPrivateIpAddress, CLUSTER_API_PORT),
                        String.format("server: https://%s:%d", publicIpAddress, CLUSTER_API_PORT));
            }
            kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), "kubeConfigData", Base64.encodeBase64String(kubeConfig.getBytes(StringUtils.getPreferredCharset())), false);
            return true;
        }
        return false;
    }

    private boolean isKubernetesClusterDashboardServiceRunning(final boolean onCreate, final Long timeoutTime) {
        if (!onCreate) {
            KubernetesClusterDetailsVO dashboardServiceRunningDetail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "dashboardServiceRunning");
            if (dashboardServiceRunningDetail != null && Boolean.parseBoolean(dashboardServiceRunningDetail.getValue())) {
                return true;
            }
        }
        if (KubernetesClusterUtil.isKubernetesClusterDashboardServiceRunning(kubernetesCluster, publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, sshKeyFile, timeoutTime, 15000)) {
            kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), "dashboardServiceRunning", String.valueOf(true), false);
            return true;
        }
        return false;
    }

    private void updateKubernetesClusterEntryEndpoint() {
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(kubernetesCluster.getId());
        kubernetesClusterVO.setEndpoint(String.format("https://%s:%d/", publicIpAddress, CLUSTER_API_PORT));
        kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesClusterVO);
    }

    public boolean startKubernetesClusterOnCreate() {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Starting Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
        }
        final long startTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterStartTimeout.value() * 1000;
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StartRequested);
        DeployDestination dest = null;
        try {
            dest = plan();
        } catch (InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the cluster failed due to insufficient capacity in the Kubernetes cluster: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        Network network = null;
        try {
            network = startKubernetesClusterNetwork(dest);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s as its network cannot be started", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        if (Strings.isNullOrEmpty(publicIpAddress) &&
                (Network.GuestType.Isolated.equals(network.getGuestType()) || kubernetesCluster.getMasterNodeCount() > 1)) { // Shared network, single-master cluster won't have an IP yet
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s as no public IP found for the cluster" , kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        List<UserVm> clusterVMs = new ArrayList<>();
        UserVm k8sMasterVM = null;
        try {
            k8sMasterVM = provisionKubernetesClusterMasterVm(network, publicIpAddress);
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the master VM failed in the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        clusterVMs.add(k8sMasterVM);
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            publicIpSshPort = getKubernetesClusterServerIpSshPort(k8sMasterVM);
            publicIpAddress = publicIpSshPort.first();
            if (Strings.isNullOrEmpty(publicIpAddress)) {
                logTransitStateAndThrow(Level.WARN, String.format("Failed to start Kubernetes cluster ID: %s as no public IP found for the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            }
        }
        try {
            List<UserVm> additionalMasterVMs = provisionKubernetesClusterAdditionalMasterVms(publicIpAddress);
            clusterVMs.addAll(additionalMasterVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning additional master VM failed in the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        try {
            List<UserVm> nodeVMs = provisionKubernetesClusterNodeVms(kubernetesCluster.getNodeCount(), publicIpAddress);
            clusterVMs.addAll(nodeVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning node VM failed in the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster ID: %s VMs successfully provisioned", kubernetesCluster.getUuid()));
        }
        try {
            setupKubernetesClusterNetworkRules(network, clusterVMs);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster ID: %s, unable to setup network rules", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        attachIsoKubernetesVMs(clusterVMs);
        if (!KubernetesClusterUtil.isKubernetesClusterMasterVmRunning(kubernetesCluster, publicIpAddress, publicIpSshPort.second(), startTimeoutTime)) {
            String msg = String.format("Failed to setup Kubernetes cluster ID: %s in usable state as unable to access master node VMs of the cluster", kubernetesCluster.getUuid());
            if (kubernetesCluster.getMasterNodeCount() > 1 && Network.GuestType.Shared.equals(network.getGuestType())) {
                msg = String.format("%s. Make sure external load-balancer has port forwarding rules for SSH access on ports %d-%d and API access on port %d",
                        msg,
                        CLUSTER_NODES_DEFAULT_START_SSH_PORT,
                        CLUSTER_NODES_DEFAULT_START_SSH_PORT + kubernetesCluster.getTotalNodeCount() - 1,
                        CLUSTER_API_PORT);
            }
            logTransitStateDetachIsoAndThrow(Level.ERROR, msg, kubernetesCluster, clusterVMs, KubernetesCluster.Event.CreateFailed, null);
        }
        boolean k8sApiServerSetup = KubernetesClusterUtil.isKubernetesClusterServerRunning(kubernetesCluster, publicIpAddress, CLUSTER_API_PORT, startTimeoutTime, 15000);
        if (!k8sApiServerSetup) {
            logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster ID: %s in usable state as unable to provision API endpoint for the cluster", kubernetesCluster.getUuid()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.CreateFailed, null);
        }
        sshPort = publicIpSshPort.second();
        updateKubernetesClusterEntryEndpoint();
        boolean readyNodesCountValid = KubernetesClusterUtil.validateKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpAddress, sshPort,
                CLUSTER_NODE_VM_USER, sshKeyFile, startTimeoutTime, 15000);
        detachIsoKubernetesVMs(clusterVMs);
        if (!readyNodesCountValid) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster ID: %s as it does not have desired number of nodes in ready state", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        if (!isKubernetesClusterKubeConfigAvailable(startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster ID: %s in usable state as unable to retrieve kube-config for the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterDashboardServiceRunning(true, startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster ID: %s in usable state as unable to get Dashboard service running for the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(),KubernetesCluster.Event.OperationFailed);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }

    public boolean startStoppedKubernetesCluster() throws CloudRuntimeException {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Starting Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
        }
        final long startTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterStartTimeout.value() * 1000;
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StartRequested);
        startKubernetesClusterVMs();
        try {
            InetAddress address = InetAddress.getByName(new URL(kubernetesCluster.getEndpoint()).getHost());
        } catch (MalformedURLException | UnknownHostException ex) {
            logTransitStateAndThrow(Level.ERROR, String.format("Kubernetes cluster ID: %s has invalid API endpoint. Can not verify if cluster is in ready state", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        Pair<String, Integer> sshIpPort =  getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = sshIpPort.first();
        sshPort = sshIpPort.second();
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s as no public IP found for the cluster" , kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!KubernetesClusterUtil.isKubernetesClusterServerRunning(kubernetesCluster, publicIpAddress, CLUSTER_API_PORT, startTimeoutTime, 15000)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s in usable state", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterKubeConfigAvailable(startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s in usable state as unable to retrieve kube-config for the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterDashboardServiceRunning(false, startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster ID: %s in usable state as unable to get Dashboard service running for the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster ID: %s successfully started", kubernetesCluster.getUuid()));
        }
        return true;
    }

    public boolean reconcileAlertCluster() {
        init();
        final long startTimeoutTime = System.currentTimeMillis() + 3 * 60 * 1000;
        List<KubernetesClusterVmMapVO> vmMapVOList = getKubernetesClusterVMMaps();
        if (CollectionUtils.isEmpty(vmMapVOList) || vmMapVOList.size() != kubernetesCluster.getTotalNodeCount()) {
            return false;
        }
        Pair<String, Integer> sshIpPort =  getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = sshIpPort.first();
        sshPort = sshIpPort.second();
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            return false;
        }
        long actualNodeCount = 0;
        try {
            actualNodeCount = KubernetesClusterUtil.getKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, sshKeyFile);
        } catch (Exception e) {
            return false;
        }
        if (kubernetesCluster.getTotalNodeCount() != actualNodeCount) {
            return false;
        }
        if (Strings.isNullOrEmpty(sshIpPort.first())) {
            return false;
        }
        if (!KubernetesClusterUtil.isKubernetesClusterServerRunning(kubernetesCluster, sshIpPort.first(),
                KubernetesClusterActionWorker.CLUSTER_API_PORT, startTimeoutTime, 0)) {
            return false;
        }
        updateKubernetesClusterEntryEndpoint();
        if (!isKubernetesClusterKubeConfigAvailable(startTimeoutTime)) {
            return false;
        }
        if (!isKubernetesClusterDashboardServiceRunning(false, startTimeoutTime)) {
            return false;
        }
        // mark the cluster to be running
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.RecoveryRequested);
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }
}
