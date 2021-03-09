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

    private KubernetesSupportedVersion kubernetesClusterVersion;

    public KubernetesClusterStartWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    public KubernetesSupportedVersion getKubernetesClusterVersion() {
        if (kubernetesClusterVersion == null) {
            kubernetesClusterVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        }
        return kubernetesClusterVersion;
    }

    private Pair<String, Map<Long, Network.IpAddresses>> getKubernetesControlNodeIpAddresses(final DataCenter zone, final Network network, final Account account) throws InsufficientAddressCapacityException {
        String controlNodeIp = null;
        Map<Long, Network.IpAddresses> requestedIps = null;
        if (Network.GuestType.Shared.equals(network.getGuestType())) {
            List<Long> vlanIds = new ArrayList<>();
            List<VlanVO> vlans = vlanDao.listVlansByNetworkId(network.getId());
            for (VlanVO vlan : vlans) {
                vlanIds.add(vlan.getId());
            }
            PublicIp ip = ipAddressManager.getAvailablePublicIpAddressFromVlans(zone.getId(), null, account, Vlan.VlanType.DirectAttached, vlanIds,network.getId(), null, false);
            if (ip != null) {
                controlNodeIp = ip.getAddress().toString();
            }
            requestedIps = new HashMap<>();
            Ip ipAddress = ip.getAddress();
            boolean isIp6 = ipAddress.isIp6();
            requestedIps.put(network.getId(), new Network.IpAddresses(ipAddress.isIp4() ? ip.getAddress().addr() : null, null));
        } else {
            controlNodeIp = ipAddressManager.acquireGuestIpAddress(networkDao.findById(kubernetesCluster.getNetworkId()), null);
        }
        return new Pair<>(controlNodeIp, requestedIps);
    }

    private boolean isKubernetesVersionSupportsHA() {
        boolean haSupported = false;
        KubernetesSupportedVersion version = getKubernetesClusterVersion();
        if (version != null) {
            try {
                if (KubernetesVersionManagerImpl.compareSemanticVersions(version.getSemanticVersion(), KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT) >= 0) {
                    haSupported = true;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Unable to compare Kubernetes version for cluster version : %s with %s", version.getName(), KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }
        return haSupported;
    }

    private String getKubernetesControlNodeConfig(final String controlNodeIp, final String serverIp,
                                                  final String hostName, final boolean haSupported,
                                                  final boolean ejectIso) throws IOException {
        String k8sControlNodeConfig = readResourceFile("/conf/k8s-control-node.yml");
        final String apiServerCert = "{{ k8s_control_node.apiserver.crt }}";
        final String apiServerKey = "{{ k8s_control_node.apiserver.key }}";
        final String caCert = "{{ k8s_control_node.ca.crt }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterToken = "{{ k8s_control_node.cluster.token }}";
        final String clusterInitArgsKey = "{{ k8s_control_node.cluster.initargs }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final List<String> addresses = new ArrayList<>();
        addresses.add(controlNodeIp);
        if (!serverIp.equals(controlNodeIp)) {
            addresses.add(serverIp);
        }
        final Certificate certificate = caManager.issueCertificate(null, Arrays.asList(hostName, "kubernetes",
                "kubernetes.default", "kubernetes.default.svc", "kubernetes.default.svc.cluster", "kubernetes.default.svc.cluster.local"),
                addresses, 3650, null);
        final String tlsClientCert = CertUtils.x509CertificateToPem(certificate.getClientCertificate());
        final String tlsPrivateKey = CertUtils.privateKeyToPem(certificate.getPrivateKey());
        final String tlsCaCert = CertUtils.x509CertificatesToPem(certificate.getCaCertificates());
        k8sControlNodeConfig = k8sControlNodeConfig.replace(apiServerCert, tlsClientCert.replace("\n", "\n      "));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(apiServerKey, tlsPrivateKey.replace("\n", "\n      "));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(caCert, tlsCaCert.replace("\n", "\n      "));
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (!Strings.isNullOrEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sControlNodeConfig = k8sControlNodeConfig.replace(sshPubKey, pubKey);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterToken, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        String initArgs = "";
        if (haSupported) {
            initArgs = String.format("--control-plane-endpoint %s:%d --upload-certs --certificate-key %s ",
                    serverIp,
                    CLUSTER_API_PORT,
                    KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        }
        initArgs += String.format("--apiserver-cert-extra-sans=%s", serverIp);
        initArgs += String.format(" --kubernetes-version=%s", getKubernetesClusterVersion().getSemanticVersion());
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterInitArgsKey, initArgs);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        return k8sControlNodeConfig;
    }

    private UserVm createKubernetesControlNode(final Network network, String serverIp) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm controlVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Pair<String, Map<Long, Network.IpAddresses>> ipAddresses = getKubernetesControlNodeIpAddresses(zone, network, owner);
        String controlNodeIp = ipAddresses.first();
        Map<Long, Network.IpAddresses> requestedIps = ipAddresses.second();
        if (Network.GuestType.Shared.equals(network.getGuestType()) && Strings.isNullOrEmpty(serverIp)) {
            serverIp = controlNodeIp;
        }
        Network.IpAddresses addrs = new Network.IpAddresses(controlNodeIp, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = kubernetesClusterNodeNamePrefix + "-control";
        if (kubernetesCluster.getControlNodeCount() > 1) {
            hostName += "-1";
        }
        hostName = getKubernetesClusterNodeAvailableName(hostName);
        boolean haSupported = isKubernetesVersionSupportsHA();
        String k8sControlNodeConfig = null;
        try {
            k8sControlNodeConfig = getKubernetesControlNodeConfig(controlNodeIp, serverIp, hostName, haSupported, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes control node configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(StringUtils.getPreferredCharset()));
        controlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, clusterTemplate, networkIds, owner,
                hostName, hostName, null, null, null,
                Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                requestedIps, addrs, null, null, null, customParameterMap, null, null, null, null, true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Created control VM ID: %s, %s in the Kubernetes cluster : %s", controlVm.getUuid(), hostName, kubernetesCluster.getName()));
        }
        return controlVm;
    }

    private String getKubernetesAdditionalControlNodeConfig(final String joinIp, final boolean ejectIso) throws IOException {
        String k8sControlNodeConfig = readResourceFile("/conf/k8s-control-node-add.yml");
        final String joinIpKey = "{{ k8s_control_node.join_ip }}";
        final String clusterTokenKey = "{{ k8s_control_node.cluster.token }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterHACertificateKey = "{{ k8s_control_node.cluster.ha.certificate.key }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (!Strings.isNullOrEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sControlNodeConfig = k8sControlNodeConfig.replace(sshPubKey, pubKey);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(joinIpKey, joinIp);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterTokenKey, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterHACertificateKey, KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        return k8sControlNodeConfig;
    }

    private UserVm createKubernetesAdditionalControlNode(final String joinIp, final int additionalControlNodeInstance) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm additionalControlVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = getKubernetesClusterNodeAvailableName(String.format("%s-control-%d", kubernetesClusterNodeNamePrefix, additionalControlNodeInstance + 1));
        String k8sControlNodeConfig = null;
        try {
            k8sControlNodeConfig = getKubernetesAdditionalControlNodeConfig(joinIp, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes control configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(StringUtils.getPreferredCharset()));
        additionalControlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, clusterTemplate, networkIds, owner,
                hostName, hostName, null, null, null,
                Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                null, addrs, null, null, null, customParameterMap, null, null, null, null, true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Created control VM ID : %s, %s in the Kubernetes cluster : %s", additionalControlVm.getUuid(), hostName, kubernetesCluster.getName()));
        }
        return additionalControlVm;
    }

    private UserVm provisionKubernetesClusterControlVm(final Network network, final String publicIpAddress) throws
            ManagementServerException, InsufficientCapacityException, ResourceUnavailableException {
        UserVm k8sControlVM = null;
        k8sControlVM = createKubernetesControlNode(network, publicIpAddress);
        addKubernetesClusterVm(kubernetesCluster.getId(), k8sControlVM.getId());
        if (kubernetesCluster.getNodeRootDiskSize() > 0) {
            resizeNodeVolume(k8sControlVM);
        }
        startKubernetesVM(k8sControlVM);
        k8sControlVM = userVmDao.findById(k8sControlVM.getId());
        if (k8sControlVM == null) {
            throw new ManagementServerException(String.format("Failed to provision control VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Provisioned the control VM : %s in to the Kubernetes cluster : %s", k8sControlVM.getDisplayName(), kubernetesCluster.getName()));
        }
        return k8sControlVM;
    }

    private List<UserVm> provisionKubernetesClusterAdditionalControlVms(final String publicIpAddress) throws
            InsufficientCapacityException, ManagementServerException, ResourceUnavailableException {
        List<UserVm> additionalControlVms = new ArrayList<>();
        if (kubernetesCluster.getControlNodeCount() > 1) {
            for (int i = 1; i < kubernetesCluster.getControlNodeCount(); i++) {
                UserVm vm = null;
                vm = createKubernetesAdditionalControlNode(publicIpAddress, i);
                addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId());
                if (kubernetesCluster.getNodeRootDiskSize() > 0) {
                    resizeNodeVolume(vm);
                }
                startKubernetesVM(vm);
                vm = userVmDao.findById(vm.getId());
                if (vm == null) {
                    throw new ManagementServerException(String.format("Failed to provision additional control VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
                }
                additionalControlVms.add(vm);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Provisioned additional control VM : %s in to the Kubernetes cluster : %s", vm.getDisplayName(), kubernetesCluster.getName()));
                }
            }
        }
        return additionalControlVms;
    }

    private Network startKubernetesClusterNetwork(final DeployDestination destination) throws ManagementServerException {
        final ReservationContext context = new ReservationContextImpl(null, null, null, owner);
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            String msg  = String.format("Network for Kubernetes cluster : %s not found", kubernetesCluster.getName());
            LOGGER.warn(msg);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        try {
            networkMgr.startNetwork(network.getId(), destination, context);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Network : %s is started for the  Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
            }
        } catch (ConcurrentOperationException | ResourceUnavailableException |InsufficientCapacityException e) {
            String msg = String.format("Failed to start Kubernetes cluster : %s as unable to start associated network : %s" , kubernetesCluster.getName(), network.getName());
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
        for (int i = 0; i < kubernetesCluster.getControlNodeCount(); ++i) {
            List<String> ips = new ArrayList<>();
            Nic controlVmNic = networkModel.getNicInNetwork(clusterVMIds.get(i), kubernetesCluster.getNetworkId());
            ips.add(controlVmNic.getIPv4Address());
            vmIdIpMap.put(clusterVMIds.get(i), ips);
        }
        lbService.assignToLoadBalancer(lb.getId(), null, vmIdIpMap);
    }

    /**
     * Setup network rules for Kubernetes cluster
     * Open up firewall port CLUSTER_API_PORT, secure port on which Kubernetes
     * API server is running. Also create load balancing rule to forward public
     * IP traffic to control VMs' private IP.
     * Open up  firewall ports NODES_DEFAULT_START_SSH_PORT to NODES_DEFAULT_START_SSH_PORT+n
     * for SSH access. Also create port-forwarding rule to forward public IP traffic to all
     * @param network
     * @param clusterVMs
     * @throws ManagementServerException
     */
    private void setupKubernetesClusterNetworkRules(Network network, List<UserVm> clusterVMs) throws ManagementServerException {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Network : %s for Kubernetes cluster : %s is not an isolated network, therefore, no need for network rules", network.getName(), kubernetesCluster.getName()));
            }
            return;
        }
        List<Long> clusterVMIds = new ArrayList<>();
        for (UserVm vm : clusterVMs) {
            clusterVMIds.add(vm.getId());
        }
        IpAddress publicIp = getSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network : %s, Kubernetes cluster : %s",
                network.getName(), kubernetesCluster.getName()));
        }

        try {
            provisionFirewallRules(publicIp, owner, CLUSTER_API_PORT, CLUSTER_API_PORT);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Provisioned firewall rule to open up port %d on %s for Kubernetes cluster ID: %s",
                        CLUSTER_API_PORT, publicIp.getAddress().addr(), kubernetesCluster.getUuid()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }

        try {
            int endPort = CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMs.size() - 1;
            provisionFirewallRules(publicIp, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Kubernetes cluster : %s", CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort, publicIp.getAddress().addr(), kubernetesCluster.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }

        // Load balancer rule fo API access for control node VMs
        try {
            provisionLoadBalancerRule(publicIp, network, owner, clusterVMIds, CLUSTER_API_PORT);
        } catch (NetworkRuleConflictException | InsufficientAddressCapacityException e) {
            throw new ManagementServerException(String.format("Failed to provision load balancer rule for API access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }

        // Port forwarding rule fo SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds, CLUSTER_NODES_DEFAULT_START_SSH_PORT);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    private void startKubernetesClusterVMs() {
        List <UserVm> clusterVms = getKubernetesClusterVMs();
        for (final UserVm vm : clusterVms) {
            if (vm == null) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            try {
                startKubernetesVM(vm);
            } catch (ManagementServerException ex) {
                LOGGER.warn(String.format("Failed to start VM : %s in Kubernetes cluster : %s due to ", vm.getDisplayName(), kubernetesCluster.getName()) + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }
        for (final UserVm userVm : clusterVms) {
            UserVm vm = userVmDao.findById(userVm.getId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
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
            final String controlVMPrivateIpAddress = getControlVmPrivateIp();
            if (!Strings.isNullOrEmpty(controlVMPrivateIpAddress)) {
                kubeConfig = kubeConfig.replace(String.format("server: https://%s:%d", controlVMPrivateIpAddress, CLUSTER_API_PORT),
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
            LOGGER.info(String.format("Starting Kubernetes cluster : %s", kubernetesCluster.getName()));
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
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as its network cannot be started", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        if (Strings.isNullOrEmpty(publicIpAddress) &&
                (Network.GuestType.Isolated.equals(network.getGuestType()) || kubernetesCluster.getControlNodeCount() > 1)) { // Shared network, single-control node cluster won't have an IP yet
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as no public IP found for the cluster" , kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        List<UserVm> clusterVMs = new ArrayList<>();
        UserVm k8sControlVM = null;
        try {
            k8sControlVM = provisionKubernetesClusterControlVm(network, publicIpAddress);
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the control VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        clusterVMs.add(k8sControlVM);
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            publicIpSshPort = getKubernetesClusterServerIpSshPort(k8sControlVM);
            publicIpAddress = publicIpSshPort.first();
            if (Strings.isNullOrEmpty(publicIpAddress)) {
                logTransitStateAndThrow(Level.WARN, String.format("Failed to start Kubernetes cluster : %s as no public IP found for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            }
        }
        try {
            List<UserVm> additionalControlVMs = provisionKubernetesClusterAdditionalControlVms(publicIpAddress);
            clusterVMs.addAll(additionalControlVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning additional control VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        try {
            List<UserVm> nodeVMs = provisionKubernetesClusterNodeVms(kubernetesCluster.getNodeCount(), publicIpAddress);
            clusterVMs.addAll(nodeVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning node VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster : %s VMs successfully provisioned", kubernetesCluster.getName()));
        }
        try {
            setupKubernetesClusterNetworkRules(network, clusterVMs);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s, unable to setup network rules", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        attachIsoKubernetesVMs(clusterVMs);
        if (!KubernetesClusterUtil.isKubernetesClusterControlVmRunning(kubernetesCluster, publicIpAddress, publicIpSshPort.second(), startTimeoutTime)) {
            String msg = String.format("Failed to setup Kubernetes cluster : %s in usable state as unable to access control node VMs of the cluster", kubernetesCluster.getName());
            if (kubernetesCluster.getControlNodeCount() > 1 && Network.GuestType.Shared.equals(network.getGuestType())) {
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
            logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s in usable state as unable to provision API endpoint for the cluster", kubernetesCluster.getName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.CreateFailed, null);
        }
        sshPort = publicIpSshPort.second();
        updateKubernetesClusterEntryEndpoint();
        boolean readyNodesCountValid = KubernetesClusterUtil.validateKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpAddress, sshPort,
                CLUSTER_NODE_VM_USER, sshKeyFile, startTimeoutTime, 15000);
        detachIsoKubernetesVMs(clusterVMs);
        if (!readyNodesCountValid) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s as it does not have desired number of nodes in ready state", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        if (!isKubernetesClusterKubeConfigAvailable(startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s in usable state as unable to retrieve kube-config for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterDashboardServiceRunning(true, startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s in usable state as unable to get Dashboard service running for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(),KubernetesCluster.Event.OperationFailed);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }

    public boolean startStoppedKubernetesCluster() throws CloudRuntimeException {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Starting Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        final long startTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterStartTimeout.value() * 1000;
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StartRequested);
        startKubernetesClusterVMs();
        try {
            InetAddress address = InetAddress.getByName(new URL(kubernetesCluster.getEndpoint()).getHost());
        } catch (MalformedURLException | UnknownHostException ex) {
            logTransitStateAndThrow(Level.ERROR, String.format("Kubernetes cluster : %s has invalid API endpoint. Can not verify if cluster is in ready state", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        Pair<String, Integer> sshIpPort =  getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = sshIpPort.first();
        sshPort = sshIpPort.second();
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as no public IP found for the cluster" , kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!KubernetesClusterUtil.isKubernetesClusterServerRunning(kubernetesCluster, publicIpAddress, CLUSTER_API_PORT, startTimeoutTime, 15000)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s in usable state", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterKubeConfigAvailable(startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s in usable state as unable to retrieve kube-config for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        if (!isKubernetesClusterDashboardServiceRunning(false, startTimeoutTime)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s in usable state as unable to get Dashboard service running for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster : %s successfully started", kubernetesCluster.getName()));
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
