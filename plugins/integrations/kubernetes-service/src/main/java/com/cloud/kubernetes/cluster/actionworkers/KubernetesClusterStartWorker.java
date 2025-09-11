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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.UserDataVO;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
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
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import org.apache.logging.log4j.Level;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

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
                logger.error(String.format("Unable to compare Kubernetes version for cluster version : %s with %s", version.getName(), KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }
        return haSupported;
    }

    private Pair<String, String> getKubernetesControlNodeConfig(final String controlNodeIp, final String serverIp,
                                                                final List<Network.IpAddresses> etcdIps, final String hostName, final boolean haSupported,
                                                                final boolean ejectIso, final boolean externalCni) throws IOException {
        String k8sControlNodeConfig = readK8sConfigFile("/conf/k8s-control-node.yml");
        final String apiServerCert = "{{ k8s_control_node.apiserver.crt }}";
        final String apiServerKey = "{{ k8s_control_node.apiserver.key }}";
        final String caCert = "{{ k8s_control_node.ca.crt }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterToken = "{{ k8s_control_node.cluster.token }}";
        final String clusterInitArgsKey = "{{ k8s_control_node.cluster.initargs }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final String installWaitTime = "{{ k8s.install.wait.time }}";
        final String installReattemptsCount = "{{ k8s.install.reattempts.count }}";
        final String externalEtcdNodes = "{{ etcd.unstacked_etcd }}";
        final String etcdEndpointList = "{{ etcd.etcd_endpoint_list }}";
        final String k8sServerIp = "{{ k8s_control.server_ip }}";
        final String k8sApiPort = "{{ k8s.api_server_port }}";
        final String certSans = "{{ k8s_control.server_ips }}";
        final String k8sCertificate = "{{ k8s_control.certificate_key }}";
        final String externalCniPlugin = "{{ k8s.external.cni.plugin }}";

        final List<String> addresses = new ArrayList<>();
        addresses.add(controlNodeIp);
        if (!serverIp.equals(controlNodeIp)) {
            addresses.add(serverIp);
        }

        boolean externalEtcd = !etcdIps.isEmpty();
        final Certificate certificate = caManager.issueCertificate(null, Arrays.asList(hostName, "kubernetes",
                        "kubernetes.default", "kubernetes.default.svc", "kubernetes.default.svc.cluster", "kubernetes.default.svc.cluster.local"),
                addresses, 3650, null);
        final String tlsClientCert = CertUtils.x509CertificateToPem(certificate.getClientCertificate());
        final String tlsPrivateKey = CertUtils.privateKeyToPem(certificate.getPrivateKey());
        final String tlsCaCert = CertUtils.x509CertificatesToPem(certificate.getCaCertificates());
        final Long waitTime = KubernetesClusterService.KubernetesControlNodeInstallAttemptWait.value();
        final Long reattempts = KubernetesClusterService.KubernetesControlNodeInstallReattempts.value();
        String endpointList = getEtcdEndpointList(etcdIps);

        k8sControlNodeConfig = k8sControlNodeConfig.replace(apiServerCert, tlsClientCert.replace("\n", "\n      "));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(apiServerKey, tlsPrivateKey.replace("\n", "\n      "));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(caCert, tlsCaCert.replace("\n", "\n      "));
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (StringUtils.isNotEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n      - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sControlNodeConfig = k8sControlNodeConfig.replace(installWaitTime, String.valueOf(waitTime));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(installReattemptsCount, String.valueOf(reattempts));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(sshPubKey, pubKey);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterToken, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(externalEtcdNodes, String.valueOf(externalEtcd));
        String initArgs = "";
        if (haSupported) {
            initArgs = String.format("--control-plane-endpoint %s:%d --upload-certs --certificate-key %s ",
                    controlNodeIp,
                    CLUSTER_API_PORT,
                    KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        }
        initArgs += String.format("--apiserver-cert-extra-sans=%s", controlNodeIp);
        initArgs += String.format(" --kubernetes-version=%s", getKubernetesClusterVersion().getSemanticVersion());
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterInitArgsKey, initArgs);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(etcdEndpointList, endpointList);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(k8sServerIp, controlNodeIp);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(k8sApiPort, String.valueOf(CLUSTER_API_PORT));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(certSans, String.format("- %s", serverIp));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(k8sCertificate, KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(externalCniPlugin, String.valueOf(externalCni));

        k8sControlNodeConfig = updateKubeConfigWithRegistryDetails(k8sControlNodeConfig);

        return new Pair<>(k8sControlNodeConfig, controlNodeIp);
    }

    private Pair<UserVm,String> createKubernetesControlNode(final Network network, String serverIp, List<Network.IpAddresses> etcdIps, Long domainId, Long accountId, Long asNumber) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm controlVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = getServiceOfferingForNodeTypeOnCluster(CONTROL, kubernetesCluster);
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Pair<String, Map<Long, Network.IpAddresses>> ipAddresses = getKubernetesControlNodeIpAddresses(zone, network, owner);
        String controlNodeIp = ipAddresses.first();
        Map<Long, Network.IpAddresses> requestedIps = ipAddresses.second();
        if (StringUtils.isEmpty(serverIp) && manager.isDirectAccess(network)) {
            serverIp = controlNodeIp;
        }
        Network.IpAddresses addrs = new Network.IpAddresses(controlNodeIp, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        if (Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType())) {
            customParameterMap.put(VmDetailConstants.ROOT_DISK_CONTROLLER, "scsi");
        }
        String suffix = Long.toHexString(System.currentTimeMillis());
        String hostName = String.format("%s-control-%s", kubernetesClusterNodeNamePrefix, suffix);
        boolean haSupported = isKubernetesVersionSupportsHA();
        Long userDataId = kubernetesCluster.getCniConfigId();
        Pair<String, String> k8sControlNodeConfigAndControlIp = new Pair<>(null, null);
        try {
            k8sControlNodeConfigAndControlIp = getKubernetesControlNodeConfig(controlNodeIp, serverIp, etcdIps, hostName, haSupported, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()), Objects.nonNull(userDataId));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes control node configuration file", e);
        }
        String k8sControlNodeConfig = k8sControlNodeConfigAndControlIp.first();
        String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        if (Objects.nonNull(userDataId)) {
            logger.info("concatenating userdata");
            UserDataVO cniConfigVo = userDataDao.findById(userDataId);
            String cniConfig = new String(Base64.decodeBase64(cniConfigVo.getUserData()));
            if (Objects.nonNull(asNumber)) {
                cniConfig = substituteASNumber(cniConfig, asNumber);
            }
            cniConfig = Base64.encodeBase64String(cniConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            base64UserData = userDataManager.concatenateUserData(base64UserData, cniConfig, null);
        }

        List<String> keypairs = new ArrayList<String>();
        if (StringUtils.isNotBlank(kubernetesCluster.getKeyPair())) {
            keypairs.add(kubernetesCluster.getKeyPair());
        }

        Long affinityGroupId = getExplicitAffinityGroup(domainId, accountId);
        String userDataDetails = kubernetesCluster.getCniConfigDetails();
        if (kubernetesCluster.getSecurityGroupId() != null &&
                networkModel.checkSecurityGroupSupportForNetwork(owner, zone, networkIds,
                        List.of(kubernetesCluster.getSecurityGroupId()))) {
            List<Long> securityGroupIds = new ArrayList<>();
            securityGroupIds.add(kubernetesCluster.getSecurityGroupId());
            controlVm = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, controlNodeTemplate, networkIds, securityGroupIds, owner,
            hostName, hostName, null, null, null, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST,base64UserData, userDataId, userDataDetails, keypairs,
                    requestedIps, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null,
                    null, true, null, UserVmManager.CKS_NODE, null, null);
        } else {
            controlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, controlNodeTemplate, networkIds, owner,
                    hostName, hostName, null, null, null, null,
                    Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, userDataId, userDataDetails, keypairs,
                    requestedIps, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null, null, true, UserVmManager.CKS_NODE, null, null, null);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created control VM: {}, {} in the Kubernetes cluster: {}", controlVm, hostName, kubernetesCluster);
        }
        return new Pair<>(controlVm, k8sControlNodeConfigAndControlIp.second());
    }

    private String substituteASNumber(String cniConfig, Long asNumber) {
        final String asNumberKey = "{{ AS_NUMBER }}";
        cniConfig = cniConfig.replace(asNumberKey, String.valueOf(asNumber));
        return cniConfig;

    }

    private String getKubernetesAdditionalControlNodeConfig(final String joinIp, final boolean ejectIso) throws IOException {
        String k8sControlNodeConfig = readK8sConfigFile("/conf/k8s-control-node-add.yml");
        final String joinIpKey = "{{ k8s_control_node.join_ip }}";
        final String clusterTokenKey = "{{ k8s_control_node.cluster.token }}";
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String clusterHACertificateKey = "{{ k8s_control_node.cluster.ha.certificate.key }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final String installWaitTime = "{{ k8s.install.wait.time }}";
        final String installReattemptsCount = "{{ k8s.install.reattempts.count }}";

        final Long waitTime = KubernetesClusterService.KubernetesControlNodeInstallAttemptWait.value();
        final Long reattempts = KubernetesClusterService.KubernetesControlNodeInstallReattempts.value();

        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (StringUtils.isNotEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n      - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sControlNodeConfig = k8sControlNodeConfig.replace(installWaitTime, String.valueOf(waitTime));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(installReattemptsCount, String.valueOf(reattempts));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(sshPubKey, pubKey);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(joinIpKey, joinIp);
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterTokenKey, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(clusterHACertificateKey, KubernetesClusterUtil.generateClusterHACertificateKey(kubernetesCluster));
        k8sControlNodeConfig = k8sControlNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        k8sControlNodeConfig = updateKubeConfigWithRegistryDetails(k8sControlNodeConfig);

        return k8sControlNodeConfig;
    }

    private String getInitialEtcdClusterDetails(List<String> ipAddresses, List<String> hostnames) {
        String initialCluster = "%s=http://%s:%s";
        StringBuilder clusterInfo = new StringBuilder();
        for (int i = 0; i < ipAddresses.size(); i++) {
            clusterInfo.append(String.format(initialCluster, hostnames.get(i), ipAddresses.get(i), KubernetesClusterActionWorker.ETCD_NODE_PEER_COMM_PORT));
            if (i < ipAddresses.size()-1) {
                clusterInfo.append(",");
            }
        }
        return clusterInfo.toString();
    }

    /**
     *
     * @param ipAddresses list of etcd node guest IPs
     * @return a formatted list of etcd endpoints adhering to YAML syntax
     */
    private String getEtcdEndpointList(List<Network.IpAddresses> ipAddresses) {
        StringBuilder endpoints = new StringBuilder();
        for (int i = 0; i < ipAddresses.size(); i++) {
            endpoints.append(String.format("- http://%s:%s", ipAddresses.get(i).getIp4Address(), KubernetesClusterActionWorker.ETCD_NODE_CLIENT_REQUEST_PORT));
            if (i < ipAddresses.size()-1) {
                endpoints.append("\n          ");
            }
        }
        return endpoints.toString();
    }


    private List<String> getEtcdNodeHostnames() {
        List<String> hostnames = new ArrayList<>();
        for (int etcdNodeIndex = 1; etcdNodeIndex <= kubernetesCluster.getEtcdNodeCount(); etcdNodeIndex++) {
            String suffix = Long.toHexString(System.currentTimeMillis());
            hostnames.add(String.format("%s-%s-%s", getEtcdNodeNameForCluster(), etcdNodeIndex, suffix));
        }
        return hostnames;
    }

    private String getEtcdNodeConfig(final List<String> ipAddresses, final List<String> hostnames, final int etcdNodeIndex,
                                     final boolean ejectIso) throws IOException {
        String k8sEtcdNodeConfig = readK8sConfigFile("/conf/etcd-node.yml");
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final String installWaitTime = "{{ k8s.install.wait.time }}";
        final String installReattemptsCount = "{{ k8s.install.reattempts.count }}";
        final String etcdNodeName = "{{ etcd.node_name }}";
        final String etcdNodeIp = "{{ etcd.node_ip }}";
        final String etcdInitialClusterNodes = "{{ etcd.initial_cluster_nodes }}";

        final Long waitTime = KubernetesClusterService.KubernetesControlNodeInstallAttemptWait.value();
        final Long reattempts = KubernetesClusterService.KubernetesControlNodeInstallReattempts.value();
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (StringUtils.isNotEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n      - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        String initialClusterDetails = getInitialEtcdClusterDetails(ipAddresses, hostnames);

        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(installWaitTime, String.valueOf(waitTime));
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(installReattemptsCount, String.valueOf(reattempts));
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(sshPubKey, pubKey);
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(etcdNodeName, hostnames.get(etcdNodeIndex));
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(etcdNodeIp, ipAddresses.get(etcdNodeIndex));
        k8sEtcdNodeConfig = k8sEtcdNodeConfig.replace(etcdInitialClusterNodes, initialClusterDetails);

        return k8sEtcdNodeConfig;
    }

    private UserVm createKubernetesAdditionalControlNode(final String joinIp, final int additionalControlNodeInstance,
                                                         final Long domainId, final Long accountId) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm additionalControlVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = getServiceOfferingForNodeTypeOnCluster(CONTROL, kubernetesCluster);
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
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
        String hostName = String.format("%s-control-%s", kubernetesClusterNodeNamePrefix, suffix);
        String k8sControlNodeConfig = null;
        try {
            k8sControlNodeConfig = getKubernetesAdditionalControlNodeConfig(joinIp, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes control configuration file", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        List<String> keypairs = new ArrayList<String>();
        if (StringUtils.isNotBlank(kubernetesCluster.getKeyPair())) {
            keypairs.add(kubernetesCluster.getKeyPair());
        }

        Long affinityGroupId = getExplicitAffinityGroup(domainId, accountId);
        if (kubernetesCluster.getSecurityGroupId() != null &&
                networkModel.checkSecurityGroupSupportForNetwork(owner, zone, networkIds,
                        List.of(kubernetesCluster.getSecurityGroupId()))) {
            List<Long> securityGroupIds = new ArrayList<>();
            securityGroupIds.add(kubernetesCluster.getSecurityGroupId());
            additionalControlVm = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, controlNodeTemplate, networkIds, securityGroupIds, owner,
                    hostName, hostName, null, null, null, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST,base64UserData, null, null, keypairs,
                    null, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null,
                    null, true, null, UserVmManager.CKS_NODE, null, null);
        } else {
            additionalControlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, controlNodeTemplate, networkIds, owner,
                    hostName, hostName, null, null, null, null,
                    Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                    null, addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null, null, true, UserVmManager.CKS_NODE, null, null, null);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Created control VM: {}, {} in the Kubernetes cluster: {}", additionalControlVm, hostName, kubernetesCluster);
        }
        return additionalControlVm;
    }

    private UserVm createEtcdNode(List<Network.IpAddresses> requestedIps, List<String> etcdNodeHostnames, int etcdNodeIndex, Long domainId, Long accountId) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        UserVm etcdNode = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = getServiceOfferingForNodeTypeOnCluster(ETCD, kubernetesCluster);
        List<Long> networkIds = Collections.singletonList(kubernetesCluster.getNetworkId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        List<String> guestIps = requestedIps.stream().map(Network.IpAddresses::getIp4Address).collect(Collectors.toList());
        String k8sControlNodeConfig = null;
        try {
            k8sControlNodeConfig = getEtcdNodeConfig(guestIps, etcdNodeHostnames, etcdNodeIndex, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()));
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Kubernetes control configuration file", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        List<String> keypairs = new ArrayList<String>();
        if (StringUtils.isNotBlank(kubernetesCluster.getKeyPair())) {
            keypairs.add(kubernetesCluster.getKeyPair());
        }
        Long affinityGroupId = getExplicitAffinityGroup(domainId, accountId);
        String hostName = etcdNodeHostnames.get(etcdNodeIndex);
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (zone.isSecurityGroupEnabled()) {
            List<Long> securityGroupIds = new ArrayList<>();
            securityGroupIds.add(kubernetesCluster.getSecurityGroupId());
            etcdNode = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, etcdTemplate, networkIds, securityGroupIds, owner,
                    hostName, hostName, null, null, null, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST,base64UserData, null, null, keypairs,
                    Map.of(kubernetesCluster.getNetworkId(), requestedIps.get(etcdNodeIndex)), addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null,
                    null, true, null, null, null, null);
        } else {
            etcdNode = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, etcdTemplate, networkIds, owner,
                    hostName, hostName, null, null, null, null,
                    Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                    Map.of(kubernetesCluster.getNetworkId(), requestedIps.get(etcdNodeIndex)), addrs, null, null, Objects.nonNull(affinityGroupId) ?
                            Collections.singletonList(affinityGroupId) : null, customParameterMap, null, null, null, null, true, UserVmManager.CKS_NODE, null, null, null);
        }

        if (logger.isInfoEnabled()) {
            logger.info(String.format("Created control VM ID : %s, %s in the Kubernetes cluster : %s", etcdNode.getUuid(), hostName, kubernetesCluster.getName()));
        }
        return etcdNode;
    }

    private Pair<UserVm, String> provisionKubernetesClusterControlVm(final Network network, final String publicIpAddress, final List<Network.IpAddresses> etcdIps,
                                                       final Long domainId, final Long accountId, Long asNumber) throws
            ManagementServerException, InsufficientCapacityException, ResourceUnavailableException {
        UserVm k8sControlVM = null;
        Pair<UserVm, String> k8sControlVMAndControlIP;
        k8sControlVMAndControlIP = createKubernetesControlNode(network, publicIpAddress, etcdIps, domainId, accountId, asNumber);
        k8sControlVM = k8sControlVMAndControlIP.first();
        addKubernetesClusterVm(kubernetesCluster.getId(), k8sControlVM.getId(), true, false, false, false);
        if (kubernetesCluster.getNodeRootDiskSize() > 0) {
            resizeNodeVolume(k8sControlVM);
        }
        startKubernetesVM(k8sControlVM, domainId, accountId, CONTROL);
        k8sControlVM = userVmDao.findById(k8sControlVM.getId());
        if (k8sControlVM == null) {
            throw new ManagementServerException(String.format("Failed to provision control VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
        }
        if (logger.isInfoEnabled()) {
            logger.info("Provisioned the control VM: {} in to the Kubernetes cluster: {}", k8sControlVM, kubernetesCluster);
        }
        return new Pair<>(k8sControlVM, k8sControlVMAndControlIP.second());
    }

    private List<UserVm> provisionKubernetesClusterAdditionalControlVms(final String controlIpAddress, final Long domainId,
                                                                        final Long accountId) throws
            InsufficientCapacityException, ManagementServerException, ResourceUnavailableException {
        List<UserVm> additionalControlVms = new ArrayList<>();
        if (kubernetesCluster.getControlNodeCount() > 1) {
            for (int i = 1; i < kubernetesCluster.getControlNodeCount(); i++) {
                UserVm vm = null;
                vm = createKubernetesAdditionalControlNode(controlIpAddress, i, domainId, accountId);
                addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId(), true, false, false, false);
                if (kubernetesCluster.getNodeRootDiskSize() > 0) {
                    resizeNodeVolume(vm);
                }
                startKubernetesVM(vm, domainId, accountId, CONTROL);
                vm = userVmDao.findById(vm.getId());
                if (vm == null) {
                    throw new ManagementServerException(String.format("Failed to provision additional control VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
                }
                additionalControlVms.add(vm);
                if (logger.isInfoEnabled()) {
                    logger.info("Provisioned additional control VM: {} in to the Kubernetes cluster: {}", vm, kubernetesCluster);
                }
            }
        }
        return additionalControlVms;
    }

    private Pair<List<UserVm>, List<Network.IpAddresses>> provisionEtcdCluster(final Network network, final Long domainId, final Long accountId)
            throws InsufficientCapacityException, ResourceUnavailableException, ManagementServerException {
        List<UserVm> etcdNodeVms = new ArrayList<>();
        List<Network.IpAddresses>  etcdNodeGuestIps = getEtcdNodeGuestIps(network, kubernetesCluster.getEtcdNodeCount());
        List<String> etcdHostnames = getEtcdNodeHostnames();
        for (int i = 0; i < kubernetesCluster.getEtcdNodeCount(); i++) {
            UserVm vm = createEtcdNode(etcdNodeGuestIps, etcdHostnames, i, domainId, accountId);
            addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId(), false, false, true, true);
            startKubernetesVM(vm, domainId, accountId, ETCD);
            vm = userVmDao.findById(vm.getId());
            if (vm == null) {
                throw new ManagementServerException(String.format("Failed to provision additional control VM for Kubernetes cluster : %s" , kubernetesCluster.getName()));
            }
            etcdNodeVms.add(vm);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned additional control VM : %s in to the Kubernetes cluster : %s", vm.getDisplayName(), kubernetesCluster.getName()));
            }
        }
        return new Pair<>(etcdNodeVms, etcdNodeGuestIps);
    }

    private List<Network.IpAddresses> getEtcdNodeGuestIps(final Network network, final long etcdNodeCount) {
        List<Network.IpAddresses> guestIps = new ArrayList<>();
        for (int i = 1; i <= etcdNodeCount; i++) {
            guestIps.add(new Network.IpAddresses(ipAddressManager.acquireGuestIpAddress(network, null), null));
        }
        return guestIps;
    }

    private Network startKubernetesClusterNetwork(final DeployDestination destination) throws ManagementServerException {
        final ReservationContext context = new ReservationContextImpl(null, null, null, owner);
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            String msg  = String.format("Network for Kubernetes cluster : %s not found", kubernetesCluster.getName());
            logger.warn(msg);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        try {
            networkMgr.startNetwork(network.getId(), destination, context);
            if (logger.isInfoEnabled()) {
                logger.info("Network: {} is started for the Kubernetes cluster: {}", network, kubernetesCluster);
            }
        } catch (ConcurrentOperationException | ResourceUnavailableException |InsufficientCapacityException e) {
            String msg = String.format("Failed to start Kubernetes cluster: %s as unable to start associated network: %s" , kubernetesCluster, network);
            logger.error(msg, e);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }
        return network;
    }

    protected void setupKubernetesClusterNetworkRules(Network network, List<UserVm> clusterVMs) throws ManagementServerException {
        if (manager.isDirectAccess(network)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Network: {} for Kubernetes cluster: {} is not an isolated network or ROUTED network, therefore, no need for network rules", network, kubernetesCluster);
            }
            return;
        }
        List<Long> clusterVMIds = clusterVMs.stream().map(UserVm::getId).collect(Collectors.toList());
        if (network.getVpcId() != null) {
            IpAddress publicIp = getVpcTierKubernetesPublicIp(network);
            if (publicIp == null) {
                throw new ManagementServerException(String.format("No public IP addresses found for VPC tier : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
            }
            setupKubernetesClusterVpcTierRules(publicIp, network, clusterVMIds);
            return;
        }
        IpAddress publicIp = getNetworkSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network : %s, Kubernetes cluster : %s",
                    network.getName(), kubernetesCluster.getName()));
        }
        setupKubernetesClusterIsolatedNetworkRules(publicIp, network, clusterVMIds, true);
    }

    protected void setupKubernetesEtcdNetworkRules(List<UserVm> etcdVms, Network network) throws ManagementServerException, ResourceUnavailableException {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Network : %s for Kubernetes cluster : %s is not an isolated network, therefore, no need for network rules", network.getName(), kubernetesCluster.getName()));
            }
        }
        List<Long> etcdVmIds = etcdVms.stream().map(UserVm::getId).collect(Collectors.toList());
        Integer startPort = KubernetesClusterService.KubernetesEtcdNodeStartPort.value();
        IpAddress publicIp = ipAddressDao.findByIpAndDcId(kubernetesCluster.getZoneId(), publicIpAddress);
        for (int i = 0; i < etcdVmIds.size(); i++) {
            int etcdStartPort = startPort + i;
            try {
                if (Objects.isNull(network.getVpcId())) {
                    provisionFirewallRules(publicIp, owner, etcdStartPort, etcdStartPort);
                } else if (network.getNetworkACLId() != NetworkACL.DEFAULT_ALLOW) {
                    try {
                        provisionVpcTierAllowPortACLRule(network, ETCD_NODE_CLIENT_REQUEST_PORT, ETCD_NODE_CLIENT_REQUEST_PORT);
                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("Provisioned ACL rule to open up port %d on %s for etcd nodes for Kubernetes cluster %s",
                                    ETCD_NODE_CLIENT_REQUEST_PORT, publicIpAddress, kubernetesCluster.getName()));
                        }
                    } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | InvalidParameterValueException | PermissionDeniedException e) {
                        throw new ManagementServerException(String.format("Failed to provision ACL rules for etcd client access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException |
                     NetworkRuleConflictException e) {
                throw new ManagementServerException(String.format("Failed to provision firewall rules for etcd nodes for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
            provisionPublicIpPortForwardingRule(publicIp, network, owner, etcdVmIds.get(i), etcdStartPort, DEFAULT_SSH_PORT);
        }
    }

    private void startKubernetesClusterVMs(Long domainId, Long accountId) {
        List <UserVm> clusterVms = getKubernetesClusterVMs();
        for (final UserVm vm : clusterVms) {
            if (vm == null) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            try {
                resizeNodeVolume(vm);
                KubernetesClusterVmMapVO map = kubernetesClusterVmMapDao.findByVmId(vm.getId());
                KubernetesServiceHelper.KubernetesClusterNodeType nodeType = getNodeTypeFromClusterVMMapRecord(map);
                startKubernetesVM(vm, domainId, accountId, nodeType);
            } catch (ManagementServerException ex) {
                logger.warn("Failed to start VM: {} in Kubernetes cluster: {} due to {}", vm, kubernetesCluster, ex);
                // don't bail out here. proceed further to stop the reset of the VM's
            }
        }
        for (final UserVm userVm : clusterVms) {
            UserVm vm = userVmDao.findById(userVm.getId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start all VMs in Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }
    }

    private KubernetesServiceHelper.KubernetesClusterNodeType getNodeTypeFromClusterVMMapRecord(KubernetesClusterVmMapVO map) {
        if (map.isControlNode()) {
            return CONTROL;
        } else if (map.isEtcdNode()) {
            return ETCD;
        } else {
            return WORKER;
        }
    }

    private boolean isKubernetesClusterKubeConfigAvailable(final long timeoutTime) {
        if (StringUtils.isEmpty(publicIpAddress)) {
            KubernetesClusterDetailsVO kubeConfigDetail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "kubeConfigData");
            if (kubeConfigDetail != null && StringUtils.isNotEmpty(kubeConfigDetail.getValue())) {
                return true;
            }
        }
        String kubeConfig = KubernetesClusterUtil.getKubernetesClusterConfig(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(), sshKeyFile, timeoutTime);
        if (StringUtils.isNotEmpty(kubeConfig)) {
            final String controlVMPrivateIpAddress = getControlVmPrivateIp();
            if (StringUtils.isNotEmpty(controlVMPrivateIpAddress)) {
                kubeConfig = kubeConfig.replace(String.format("server: https://%s:%d", controlVMPrivateIpAddress, CLUSTER_API_PORT),
                        String.format("server: https://%s:%d", publicIpAddress, CLUSTER_API_PORT));
            }
            kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), "kubeConfigData", Base64.encodeBase64String(kubeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset())), false);
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
        if (KubernetesClusterUtil.isKubernetesClusterDashboardServiceRunning(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(), sshKeyFile, timeoutTime, 15000)) {
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

    public boolean startKubernetesClusterOnCreate(Long domainId, Long accountId, Long asNumber) throws ManagementServerException, ResourceUnavailableException, InsufficientCapacityException {
        init();
        if (logger.isInfoEnabled()) {
            logger.info("Starting Kubernetes cluster: {}", kubernetesCluster);
        }
        final long startTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterStartTimeout.value() * 1000;
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StartRequested);
        DeployDestination dest = null;
        try {
            VMTemplateVO clusterTemplate = templateDao.findById(kubernetesCluster.getTemplateId());
            Map<String, DeployDestination> destinationMap = planKubernetesCluster(domainId, accountId, clusterTemplate.getHypervisorType());
            dest = destinationMap.get(WORKER.name());
        } catch (InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the cluster failed due to insufficient capacity in the Kubernetes cluster: %s", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        Network network = null;
        try {
            network = startKubernetesClusterNetwork(dest);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as its network cannot be started", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        Pair<String, Integer> publicIpSshPort = new Pair<>(null, null);
        try {
            publicIpSshPort = getKubernetesClusterServerIpSshPort(null, true);
        } catch (InsufficientAddressCapacityException | ResourceAllocationException | ResourceUnavailableException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as failed to acquire public IP" , kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        publicIpAddress = publicIpSshPort.first();
        if (StringUtils.isEmpty(publicIpAddress) &&
                (!manager.isDirectAccess(network) || kubernetesCluster.getControlNodeCount() > 1)) { // Shared network, single-control node cluster won't have an IP yet
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Kubernetes cluster : %s as no public IP found for the cluster" , kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
        }
        // Allow account creating the kubernetes cluster to access systemVM template
        if (isDefaultTemplateUsed()) {
            LaunchPermissionVO launchPermission = new LaunchPermissionVO(kubernetesCluster.getTemplateId(), owner.getId());
            launchPermissionDao.persist(launchPermission);
        }

        List<UserVm> etcdVms = new ArrayList<>();
        List<Network.IpAddresses> etcdGuestNodeIps = new ArrayList<>();
        if (kubernetesCluster.getEtcdNodeCount() > 0) {
            Pair<List<UserVm>, List<Network.IpAddresses>> etcdNodesAndIps = provisionEtcdCluster(network, domainId, accountId);
            etcdVms = etcdNodesAndIps.first();
            etcdGuestNodeIps = etcdNodesAndIps.second();
        }

        List<UserVm> clusterVMs = new ArrayList<>();
        Pair<UserVm, String> k8sControlVMAndIp = new Pair<>(null, null);
        UserVm k8sControlVM = null;
        try {
            k8sControlVMAndIp = provisionKubernetesClusterControlVm(network, publicIpAddress, etcdGuestNodeIps, domainId, accountId, asNumber);
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the control VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        k8sControlVM = k8sControlVMAndIp.first();
        clusterVMs.add(k8sControlVM);
        if (StringUtils.isEmpty(publicIpAddress)) {
            publicIpSshPort = getKubernetesClusterServerIpSshPort(k8sControlVM);
            publicIpAddress = publicIpSshPort.first();
            if (StringUtils.isEmpty(publicIpAddress)) {
                logTransitStateAndThrow(Level.WARN, String.format("Failed to start Kubernetes cluster : %s as no public IP found for the cluster", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed);
            }
        }
        try {
            List<UserVm> additionalControlVMs = provisionKubernetesClusterAdditionalControlVms(k8sControlVMAndIp.second(), domainId, accountId);
            clusterVMs.addAll(additionalControlVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning additional control VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        try {
            List<UserVm> nodeVMs = provisionKubernetesClusterNodeVms(kubernetesCluster.getNodeCount(), k8sControlVMAndIp.second(), domainId, accountId);
            clusterVMs.addAll(nodeVMs);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning node VM failed in the Kubernetes cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Kubernetes cluster: {} VMs successfully provisioned", kubernetesCluster);
        }
        try {
            setupKubernetesClusterNetworkRules(network, clusterVMs);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s, unable to setup network rules", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        try {
            setupKubernetesEtcdNetworkRules(etcdVms, network);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Kubernetes cluster : %s, unable to setup network rules for etcd nodes", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.CreateFailed, e);
        }
        attachIsoKubernetesVMs(etcdVms);
        attachIsoKubernetesVMs(clusterVMs);
        if (!KubernetesClusterUtil.isKubernetesClusterControlVmRunning(kubernetesCluster, publicIpAddress, publicIpSshPort.second(), startTimeoutTime)) {
            String msg = String.format("Failed to setup Kubernetes cluster : %s is not in usable state as the system is unable to access control node VMs of the cluster", kubernetesCluster.getName());
            if (kubernetesCluster.getControlNodeCount() > 1 && manager.isDirectAccess(network)) {
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
                getControlNodeLoginUser(), sshKeyFile, startTimeoutTime, 15000);
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
        taintControlNodes();
        deployProvider();
        updateLoginUserDetails(clusterVMs.stream().map(InternalIdentity::getId).collect(Collectors.toList()));
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }



    public boolean startStoppedKubernetesCluster(Long domainId, Long accountId) throws CloudRuntimeException {
        init();
        if (logger.isInfoEnabled()) {
            logger.info("Starting Kubernetes cluster: {}", kubernetesCluster);
        }
        final long startTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterStartTimeout.value() * 1000;
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StartRequested);
        startKubernetesClusterVMs(domainId, accountId);
        try {
            InetAddress address = InetAddress.getByName(new URL(kubernetesCluster.getEndpoint()).getHost());
        } catch (MalformedURLException | UnknownHostException ex) {
            logTransitStateAndThrow(Level.ERROR, String.format("Kubernetes cluster : %s has invalid API endpoint. Can not verify if cluster is in ready state", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        Pair<String, Integer> sshIpPort =  getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = sshIpPort.first();
        sshPort = sshIpPort.second();
        if (StringUtils.isEmpty(publicIpAddress)) {
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
        if (logger.isInfoEnabled()) {
            logger.info("Kubernetes cluster: {} successfully started", kubernetesCluster);
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
        if (StringUtils.isEmpty(publicIpAddress)) {
            return false;
        }
        long actualNodeCount = 0;
        try {
            actualNodeCount = KubernetesClusterUtil.getKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(), sshKeyFile);
        } catch (Exception e) {
            return false;
        }
        if (kubernetesCluster.getTotalNodeCount() != actualNodeCount) {
            return false;
        }
        if (StringUtils.isEmpty(sshIpPort.first())) {
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
