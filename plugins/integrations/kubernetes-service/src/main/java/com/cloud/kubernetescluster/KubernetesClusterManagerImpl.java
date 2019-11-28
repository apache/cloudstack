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
package com.cloud.kubernetescluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.CreateKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.DeleteKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.GetKubernetesClusterConfigCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.ListKubernetesClustersCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.ScaleKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.StartKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.StopKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetescluster.UpgradeKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.response.KubernetesClusterConfigResponse;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetescluster.dao.KubernetesClusterDao;
import com.cloud.kubernetescluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetescluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetesversion.KubernetesSupportedVersion;
import com.cloud.kubernetesversion.KubernetesSupportedVersionVO;
import com.cloud.kubernetesversion.KubernetesVersionManagerImpl;
import com.cloud.kubernetesversion.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.Nic;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

public class KubernetesClusterManagerImpl extends ManagerBase implements KubernetesClusterService {

    private static final Logger LOGGER = Logger.getLogger(KubernetesClusterManagerImpl.class);

    protected StateMachine2<KubernetesCluster.State, KubernetesCluster.Event, KubernetesCluster> _stateMachine = KubernetesCluster.State.getStateMachine();

    public static final String MIN_KUBERNETES_VERSION_HA_SUPPORT = "1.16";

    ScheduledExecutorService _gcExecutor;
    ScheduledExecutorService _stateScanner;

    @Inject
    protected KubernetesClusterDao kubernetesClusterDao;
    @Inject
    protected KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    protected KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Inject
    protected KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Inject
    protected CAManager caManager;
    @Inject
    protected SSHKeyPairDao sshKeyPairDao;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected ClusterDao clusterDao;
    @Inject
    protected ClusterDetailsDao clusterDetailsDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected VMTemplateDao templateDao;
    @Inject
    protected TemplateApiService templateService;
    @Inject
    protected VMTemplateZoneDao templateZoneDao;
    @Inject
    protected AccountService accountService;
    @Inject
    protected AccountDao accountDao;
    @Inject
    protected AccountManager accountManager;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected UserVmDao userVmDao;
    @Inject
    protected UserVmService userVmService;
    @Inject
    protected UserVmManager userVmManager;
    @Inject
    protected ConfigurationDao globalConfigDao;
    @Inject
    protected NetworkOfferingDao networkOfferingDao;
    @Inject
    protected NetworkService networkService;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected PhysicalNetworkDao physicalNetworkDao;
    @Inject
    protected NetworkOrchestrationService networkMgr;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected IPAddressDao ipAddressDao;
    @Inject
    protected PortForwardingRulesDao portForwardingRulesDao;
    @Inject
    protected FirewallService firewallService;
    @Inject
    protected RulesService rulesService;
    @Inject
    protected NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    protected CapacityManager capacityManager;
    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected FirewallRulesDao firewallRulesDao;
    @Inject
    protected IpAddressManager ipAddressManager;
    @Inject
    public LoadBalancingRulesService lbService;

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private String readResourceFile(String resource) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), Charset.defaultCharset().name());
    }

    private boolean isKubernetesServiceConfigured(DataCenter zone) {
        // Check Kubernetes VM template for zone
        String templateName = globalConfigDao.getValue(KubernetesServiceConfig.KubernetesClusterTemplateName.key());
        if (templateName == null || templateName.isEmpty()) {
            LOGGER.warn("Global setting " + KubernetesServiceConfig.KubernetesClusterTemplateName.key() + " is empty." +
                    "Template name need to be specified, for Kubernetes service to function.");
            return false;
        }
        final VMTemplateVO template = templateDao.findByTemplateName(templateName);
        if (template == null) {
            LOGGER.warn("Unable to find the template:" + templateName + " to be used for provisioning cluster");
            return false;
        }
        // Check network offering
        String networkOfferingName = globalConfigDao.getValue(KubernetesServiceConfig.KubernetesClusterNetworkOffering.key());
        if (networkOfferingName == null || networkOfferingName.isEmpty()) {
            LOGGER.warn("global setting " + KubernetesServiceConfig.KubernetesClusterNetworkOffering.key() + " is empty. " +
                    "Admin has not yet specified the network offering to be used for provisioning isolated network for the cluster.");
            return false;
        }
        NetworkOfferingVO networkOffering = networkOfferingDao.findByUniqueName(networkOfferingName);
        if (networkOffering == null) {
            LOGGER.warn("Network offering with name :" + networkOfferingName + " specified by admin is not found.");
            return false;
        }
        if (networkOffering.getState() == NetworkOffering.State.Disabled) {
            LOGGER.warn("Network offering :" + networkOfferingName + "is not enabled.");
            return false;
        }
        List<String> services = networkOfferingServiceMapDao.listServicesForNetworkOffering(networkOffering.getId());
        if (services == null || services.isEmpty() || !services.contains("SourceNat")) {
            LOGGER.warn("Network offering :" + networkOfferingName + " does not have necessary services to provision Kubernetes cluster");
            return false;
        }
        if (!networkOffering.isEgressDefaultPolicy()) {
            LOGGER.warn("Network offering :" + networkOfferingName + "has egress default policy turned off should be on to provision Kubernetes cluster.");
            return false;
        }
        long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
        PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            LOGGER.warn("Unable to find physical network with ID: " + physicalNetworkId + " and tag: " + networkOffering.getTags());
            return false;
        }

        return true;
    }

    private String generateClusterToken(KubernetesCluster kubernetesCluster) {
        if (kubernetesCluster == null) return "";
        String token = kubernetesCluster.getUuid();
        token = token.replaceAll("-", "");
        token = token.substring(0, 22);
        token = token.substring(0, 6) + "." + token.substring(6);
        return token;
    }

    private String generateClusterHACertificateKey(KubernetesCluster kubernetesCluster) {
        if (kubernetesCluster == null) return "";
        String uuid = kubernetesCluster.getUuid();
        StringBuilder token = new StringBuilder(uuid.replaceAll("-", ""));
        while (token.length() < 64) {
            token.append(token);
        }
        return token.toString().substring(0, 64);
    }

    private KubernetesClusterVmMapVO addKubernetesClusterVm(final long kubernetesClusterId, final long vmId) {
        return Transaction.execute(new TransactionCallback<KubernetesClusterVmMapVO>() {
            @Override
            public KubernetesClusterVmMapVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVmMapVO newClusterVmMap = new KubernetesClusterVmMapVO(kubernetesClusterId, vmId);
                kubernetesClusterVmMapDao.persist(newClusterVmMap);
                return newClusterVmMap;
            }
        });
    }

    @Override
    public KubernetesCluster findById(final Long id) {
        return kubernetesClusterDao.findById(id);
    }

    // perform a cold start (which will provision resources as well)
    private boolean startKubernetesClusterOnCreate(final long kubernetesClusterId) throws ManagementServerException {

        // Starting a Kubernetes cluster has below workflow
        //   - start the network
        //   - provision the master /node VM
        //   - provision node VM's (as many as cluster size)
        //   - update the book keeping data of the VM's provisioned for the cluster
        //   - setup networking (add Firewall and PF rules)
        //   - wait till Kubernetes API server on master VM to come up
        //   - wait till addon services (dashboard etc) to come up
        //   - update API and dashboard URL endpoints in Kubernetes cluster details

        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);

        LOGGER.debug(String.format("Starting Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.StartRequested);

        Account account = accountDao.findById(kubernetesCluster.getAccountId());

        DeployDestination dest = null;
        try {
            dest = plan(kubernetesClusterId, kubernetesCluster.getZoneId());
        } catch (InsufficientCapacityException e) {
            String msg = String.format("Provisioning the cluster failed due to insufficient capacity in the Kubernetes cluster: %s", kubernetesCluster.getUuid());
            LOGGER.error(msg, e);
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }
        final ReservationContext context = new ReservationContextImpl(null, null, null, account);

        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            String msg  = String.format("Network for Kubernetes cluster ID: %s not found", kubernetesCluster.getUuid());
            LOGGER.warn(msg);
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        try {
            networkMgr.startNetwork(network.getId(), dest, context);
            LOGGER.debug(String.format("Network ID: %s is started for the  Kubernetes cluster ID: %s", network.getUuid(), kubernetesCluster.getUuid()));
        } catch (Exception e) {
            String msg = String.format("Failed to start Kubernetes cluster ID: %s as unable to start associated network ID: %s" , kubernetesCluster.getUuid(), network.getUuid());
            LOGGER.error(msg, e);
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }

        IPAddressVO publicIp = null;
        List<IPAddressVO> ips = ipAddressDao.listByAssociatedNetwork(kubernetesCluster.getNetworkId(), true);
        if (ips == null || ips.isEmpty()) {
            String msg = String.format("Failed to start Kubernetes cluster ID: %s as no public IP found for associated network ID: %s" , kubernetesCluster.getUuid(), network.getUuid());
            LOGGER.warn(msg);
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        publicIp = ips.get(0);

        List<Long> clusterVMIds = new ArrayList<>();

        UserVm k8sMasterVM = null;
        try {
            k8sMasterVM = createKubernetesMaster(kubernetesCluster, ips);
            addKubernetesClusterVm(kubernetesCluster.getId(), k8sMasterVM.getId());
            startKubernetesVM(k8sMasterVM, kubernetesCluster);
            clusterVMIds.add(k8sMasterVM.getId());
            k8sMasterVM = userVmDao.findById(k8sMasterVM.getId());
            LOGGER.debug(String.format("Provisioned the master VM ID: %s in to the Kubernetes cluster ID: %s", k8sMasterVM.getUuid(), kubernetesCluster.getUuid()));
        } catch (Exception e) {
            String msg = String.format("Provisioning the master VM failed in the Kubernetes cluster ID: %s", kubernetesCluster.getUuid());
            LOGGER.warn(msg, e);
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }

        String masterIP = k8sMasterVM.getPrivateIpAddress();

        if (kubernetesCluster.getMasterNodeCount() > 1) {
            for (int i = 1; i < kubernetesCluster.getMasterNodeCount(); i++) {
                UserVm vm = null;
                try {
                    vm = createKubernetesAdditionalMaster(kubernetesCluster, publicIp.getAddress().toString(), i);
                    addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId());
                    startKubernetesVM(vm, kubernetesCluster);
                    clusterVMIds.add(vm.getId());
                    LOGGER.debug(String.format("Provisioned additional master VM ID: %s in to the Kubernetes cluster ID: %s", vm.getUuid(), kubernetesCluster.getUuid()));
                } catch (Exception e) {
                    String msg = String.format("Provisioning additional master VM %d/%d failed in the Kubernetes cluster ID: %s", i+1, kubernetesCluster.getMasterNodeCount(), kubernetesCluster.getUuid());
                    LOGGER.warn(msg, e);
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
                    throw new ManagementServerException(msg, e);
                }
            }
        }

        for (int i = 1; i <= kubernetesCluster.getNodeCount(); i++) {
            UserVm vm = null;
            try {
                vm = createKubernetesNode(kubernetesCluster, masterIP, i);
                addKubernetesClusterVm(kubernetesCluster.getId(), vm.getId());
                startKubernetesVM(vm, kubernetesCluster);
                clusterVMIds.add(vm.getId());
                LOGGER.debug(String.format("Provisioned node master VM ID: %s in to the Kubernetes cluster ID: %s", vm.getUuid(), kubernetesCluster.getUuid()));
            } catch (Exception e) {
                String msg = String.format("Provisioning node VM %d/%d failed in the Kubernetes cluster ID: %s", i, kubernetesCluster.getNodeCount(), kubernetesCluster.getUuid());
                LOGGER.warn(msg, e);
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
                throw new ManagementServerException(msg, e);
            }
        }
        LOGGER.debug(String.format("Kubernetes cluster ID: %s VMs successfully provisioned", kubernetesCluster.getUuid()));

        setupKubernetesClusterNetworkRules(publicIp, account, kubernetesClusterId, clusterVMIds);
        attachIsoKubernetesVMs(kubernetesCluster, clusterVMIds);

        int retryCounter = 0;
        int maxRetries = 15;
        boolean k8sApiServerSetup = false;

        while (retryCounter < maxRetries) {
            try {
                String versionOutput = IOUtils.toString(new URL(String.format("https://%s:%d/version", publicIp.getAddress().addr(), 6443)), StandardCharsets.UTF_8);
                if (!Strings.isNullOrEmpty(versionOutput)) {
                    LOGGER.debug(String.format("Kubernetes cluster ID: %s API has been successfully provisioned, %s", kubernetesCluster.getUuid(), versionOutput));
                    k8sApiServerSetup = true;
                    kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
                    kubernetesCluster.setEndpoint(String.format("https://%s:%d/", publicIp.getAddress().addr(), 6443));
                    kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesCluster);
                    break;
                }
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Waiting for Kubernetes cluster: " + kubernetesCluster.getName() + " API endpoint to be available. retry: " + retryCounter + "/" + maxRetries);
                }
            }
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster: %s API endpoint to be available", kubernetesCluster.getUuid()), ie);
            }
            retryCounter++;
        }

        boolean k8sKubeConfigCopied = false;
        if (k8sApiServerSetup) {
            retryCounter = 0;
            maxRetries = 5;
            String kubeConfig = "";
            while (retryCounter < maxRetries && kubeConfig.isEmpty()) {
                try {
                    Boolean devel = Boolean.valueOf(globalConfigDao.getValue("developer"));
                    String keyFile = String.format("%s/.ssh/id_rsa", System.getProperty("user.home"));
                    if (devel) {
                        keyFile += ".cloud";
                    }
                    File pkFile = new File(keyFile);
                    Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core",
                            pkFile, null, "sudo cat /etc/kubernetes/admin.conf",
                            10000, 10000, 10000);

                    if (result.first() && !Strings.isNullOrEmpty(result.second())) {
                        kubeConfig = result.second();
                        kubeConfig = kubeConfig.replace(String.format("server: https://%s:6443", k8sMasterVM.getPrivateIpAddress()),
                                String.format("server: https://%s:6443", publicIp.getAddress().addr()));
                        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), "kubeConfigData", Base64.encodeBase64String(kubeConfig.getBytes(Charset.forName("UTF-8"))), false);
                        k8sKubeConfigCopied = true;
                        LOGGER.debug(String.format("Kubernetes cluster: %s kube-config has been successdully retrieved", kubernetesCluster.getUuid()));
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to retrieve kube-config file for cluster with ID " + kubernetesCluster.getUuid() + ": " + e);
                }
                retryCounter++;
            }
        }

        if (k8sKubeConfigCopied) {
            retryCounter = 0;
            maxRetries = 30;
            // Dashbaord service is a docker image downloaded at run time.
            // So wait for some time and check if dashbaord service is up running.
            while (retryCounter < maxRetries) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Waiting for dashboard service for the Kubernetes cluster: " + kubernetesCluster.getName()
                            + " to come up. Attempt: " + retryCounter + " of max retries " + maxRetries);
                }

                if (isAddOnServiceRunning(kubernetesCluster.getId(), "kubernetes-dashboard", "kubernetes-dashboard")) {

                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationSucceeded);

                    kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
                    kubernetesCluster.setConsoleEndpoint("https://" + publicIp.getAddress() + ":6443/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy#!/overview?namespace=_all");
                    kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesCluster);

                    detachIsoKubernetesVMs(kubernetesCluster, clusterVMIds);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Kubernetes cluster name:" + kubernetesCluster.getName() + " is successfully started");
                    }

                    return true;
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }
                retryCounter++;
            }
            LOGGER.warn("Failed to setup Kubernetes cluster " + kubernetesCluster.getName() + " in usable state as" +
                    " unable to bring dashboard add on service up");
        } else {
            LOGGER.warn("Failed to setup Kubernetes cluster " + kubernetesCluster.getName() + " in usable state as" +
                    " unable to bring the API server up");
        }

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);

        detachIsoKubernetesVMs(kubernetesCluster, clusterVMIds);

        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Failed to deploy Kubernetes cluster: " + kubernetesCluster.getUuid() + " as unable to setup up in usable state");
    }

    private boolean startStoppedKubernetesCluster(long kubernetesClusterId) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new ManagementServerException("Invalid Kubernetes cluster ID");
        }

        if (kubernetesCluster.getRemoved() != null) {
            throw new ManagementServerException(String.format("Kubernetes cluster ID: %s is already deleted", kubernetesCluster.getUuid()));
        }

        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Running)) {
            LOGGER.debug(String.format("Kubernetes cluster ID: %s is in running state", kubernetesCluster.getUuid()));
            return true;
        }

        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Starting)) {
            LOGGER.debug(String.format("Kubernetes cluster ID: %s is already in starting state", kubernetesCluster.getUuid()));
            return true;
        }

        LOGGER.debug(String.format("Starting Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.StartRequested);

        for (final KubernetesClusterVmMapVO vmMapVO : kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId)) {
            final UserVmVO vm = userVmDao.findById(vmMapVO.getVmId());
            try {
                if (vm == null) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ManagementServerException("Failed to start all VMs in Kubernetes cluster ID: " + kubernetesClusterId);
                }
                startKubernetesVM(vm, kubernetesCluster);
            } catch (ServerApiException ex) {
                LOGGER.warn("Failed to start VM in Kubernetes cluster ID:" + kubernetesClusterId + " due to " + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }

        for (final KubernetesClusterVmMapVO vmMapVO : kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId)) {
            final UserVmVO vm = userVmDao.findById(vmMapVO.getVmId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                throw new ManagementServerException("Failed to start all VMs in Kubernetes cluster ID: " + kubernetesClusterId);
            }
        }

        InetAddress address = null;
        try {
            address = InetAddress.getByName(new URL(kubernetesCluster.getEndpoint()).getHost());
        } catch (MalformedURLException | UnknownHostException ex) {
            // API end point is generated by CCS, so this situation should not arise.
            LOGGER.warn("Kubernetes cluster ID:" + kubernetesClusterId + " has invalid api endpoint. Can not " +
                    "verify if cluster is in ready state.");
            throw new ManagementServerException("Can not verify if Kubernetes cluster ID:" + kubernetesClusterId + " is in usable state.");
        }

        // wait for fixed time for K8S api server to be avaialble
        int retryCounter = 0;
        int maxRetries = 10;
        boolean k8sApiServerSetup = false;
        while (retryCounter < maxRetries) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address.getHostAddress(), 6443), 10000);
                k8sApiServerSetup = true;
                break;
            } catch (IOException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Waiting for Kubernetes cluster: " + kubernetesCluster.getName() + " API endpoint to be available. retry: " + retryCounter + "/" + maxRetries);
                }
                try {
                    Thread.sleep(50000);
                } catch (InterruptedException ex) {
                }
                retryCounter++;
            }
        }

        if (!k8sApiServerSetup) {
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
            throw new ManagementServerException("Failed to setup Kubernetes cluster ID: " + kubernetesClusterId + " is usable state.");
        }

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationSucceeded);
        LOGGER.debug(String.format("Kubernetes cluster ID: %s successfully started", kubernetesCluster.getUuid()));
        return true;
    }

    // Open up  firewall port 6443, secure port on which Kubernetes API server is running. Also create port-forwarding
    // rule to forward public IP traffic to master VM private IP
    // Open up  firewall ports 2222 to 2222+n for SSH access. Also create port-forwarding
    // rule to forward public IP traffic to all node VM private IP
    private void setupKubernetesClusterNetworkRules(IPAddressVO publicIp, Account account, long kubernetesClusterId,
                                                   List<Long> clusterVMIds) throws ManagementServerException {

        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);

        List<String> sourceCidrList = new ArrayList<String>();
        sourceCidrList.add("0.0.0.0/0");

        try {
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
            startPortField.set(rule, new Integer(6443));

            Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
            endPortField.setAccessible(true);
            endPortField.set(rule, new Integer(6443));

            Field cidrField = rule.getClass().getDeclaredField("cidrlist");
            cidrField.setAccessible(true);
            cidrField.set(rule, sourceCidrList);

            firewallService.createIngressFirewallRule(rule);
            firewallService.applyIngressFwRules(publicIp.getId(), account);

            LOGGER.debug(String.format("Provisioned firewall rule to open up port 6443 on %s for Kubernetes cluster ID: %s",
                    publicIp.getAddress().addr(), kubernetesCluster.getUuid()));
        } catch (Exception e) {
            LOGGER.warn("Failed to provision firewall rules for the Kubernetes cluster: " + kubernetesCluster.getName()
                    + " due to exception: " + getStackTrace(e));
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to provision firewall rules for the Kubernetes " +
                    "cluster: " + kubernetesCluster.getName());
        }

        try {
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
            startPortField.set(rule, new Integer(2222));

            Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
            endPortField.setAccessible(true);
            endPortField.set(rule, new Integer(2222 + clusterVMIds.size() - 1)); // clusterVMIds contains all nodes including master

            Field cidrField = rule.getClass().getDeclaredField("cidrlist");
            cidrField.setAccessible(true);
            cidrField.set(rule, sourceCidrList);

            firewallService.createIngressFirewallRule(rule);
            firewallService.applyIngressFwRules(publicIp.getId(), account);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Provisioned firewall rule to open up port 2222 on " + publicIp.getAddress() +
                        " for cluster " + kubernetesCluster.getName());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to provision firewall rules for the Kubernetes cluster: " + kubernetesCluster.getName()
                    + " due to exception: " + getStackTrace(e));
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to provision firewall rules for the Kubernetes " +
                    "cluster: " + kubernetesCluster.getName());
        }

        // Load balancer rule fo API access for master node VMs
        try {
            LoadBalancer lb = lbService.createPublicLoadBalancerRule(null, "api-lb", "LB rule for API access",
                    6443, 6443, 6443, 6443,
                    publicIp.getId(), NetUtils.TCP_PROTO, "roundrobin", kubernetesCluster.getNetworkId(),
                    kubernetesCluster.getAccountId(), false, NetUtils.TCP_PROTO, true);

            Map<Long, List<String>> vmIdIpMap = new HashMap<>();
            for (int i=0; i<kubernetesCluster.getMasterNodeCount(); ++i) {
                List<String> ips = new ArrayList<>();
                Nic masterVmNic = networkModel.getNicInNetwork(clusterVMIds.get(i), kubernetesCluster.getNetworkId());
                ips.add(masterVmNic.getIPv4Address());
                vmIdIpMap.put(clusterVMIds.get(i), ips);
            }
            lbService.assignToLoadBalancer(lb.getId(), null, vmIdIpMap);
        } catch (Exception e) {
            LOGGER.warn("Failed to provision load balancer rule for API access for the Kubernetes cluster: " + kubernetesCluster.getName()
                    + " due to exception: " + getStackTrace(e));
            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to provision load balancer rule for API access for the Kubernetes " +
                    "cluster: " + kubernetesCluster.getName());
        }

        // Port forwarding rule fo SSH access on each node VM
        final long publicIpId = publicIp.getId();
        final long networkId = kubernetesCluster.getNetworkId();
        final long accountId = account.getId();
        final long domainId = account.getDomainId();

        for (int i = 0; i < clusterVMIds.size(); ++i) {
            long vmId = clusterVMIds.get(i);
            Nic vmNic = networkModel.getNicInNetwork(vmId, kubernetesCluster.getNetworkId());
            final Ip vmIp = new Ip(vmNic.getIPv4Address());
            final long vmIdFinal = vmId;
            final int srcPortFinal = 2222 + i;

            try {
                PortForwardingRuleVO pfRule = Transaction.execute(new TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>() {
                    @Override
                    public PortForwardingRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                        PortForwardingRuleVO newRule =
                                new PortForwardingRuleVO(null, publicIpId,
                                        srcPortFinal, srcPortFinal,
                                        vmIp,
                                        22, 22,
                                        "tcp", networkId, accountId, domainId, vmIdFinal);
                        newRule.setDisplay(true);
                        newRule.setState(FirewallRule.State.Add);
                        newRule = portForwardingRulesDao.persist(newRule);
                        return newRule;
                    }
                });
                rulesService.applyPortForwardingRules(publicIp.getId(), account);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Provisioning SSH port forwarding rule from port 2222 to 22 on " + publicIp.getAddress() +
                            " to the VM IP :" + vmIp + " in Kubernetes cluster " + kubernetesCluster.getName());
                }
            } catch (RuntimeException rte) {
                LOGGER.warn("Failed to activate SSH port forwarding rules for the Kubernetes cluster " + kubernetesCluster.getName() + " due to " + rte);
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
                throw new ManagementServerException("Failed to activate SSH port forwarding rules for the cluster: " + kubernetesCluster.getName(), rte);
            } catch (Exception e) {
                LOGGER.warn("Failed to activate SSH port forwarding rules for the Kubernetes cluster " + kubernetesCluster.getName() + " due to " + e);
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.CreateFailed);
                throw new ManagementServerException("Failed to activate SSH port forwarding rules for the cluster: " + kubernetesCluster.getName(), e);
            }
        }
    }

    // Open up  firewall ports 2222 to 2222+n for SSH access. Also create port-forwarding
    // rule to forward public IP traffic to all node VM private IP. Existing node VMs before scaling
    // will already be having these rules
    private void scaleKubernetesClusterNetworkRules(IPAddressVO publicIp, Account account, long kubernetesClusterId,
                                                    List<Long> clusterVMIds) throws ManagementServerException {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);

        List<String> sourceCidrList = new ArrayList<String>();
        sourceCidrList.add("0.0.0.0/0");
        boolean firewallRuleFound = false;
        int existingFirewallRuleSourcePortEnd = 2222;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO firewallRule : firewallRules) {
            if (firewallRule.getSourcePortStart() == 2222) {
                firewallRuleFound = true;
                existingFirewallRuleSourcePortEnd = firewallRule.getSourcePortEnd();
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                break;
            }
        }
        if (!firewallRuleFound) {
            throw new ManagementServerException("Firewall rule for node SSH access can't be provisioned!");
        }
        try {
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
            startPortField.set(rule, new Integer(2222));

            Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
            endPortField.setAccessible(true);
            endPortField.set(rule, new Integer(2222 + (int)kubernetesCluster.getNodeCount()));

            Field cidrField = rule.getClass().getDeclaredField("cidrlist");
            cidrField.setAccessible(true);
            cidrField.set(rule, sourceCidrList);

            firewallService.createIngressFirewallRule(rule);
            firewallService.applyIngressFwRules(publicIp.getId(), account);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Provisioned firewall rule to open up port 2222 on " + publicIp.getAddress() +
                        " for cluster " + kubernetesCluster.getName());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to provision firewall rules for the Kubernetes cluster: " + kubernetesCluster.getName()
                    + " due to exception: " + getStackTrace(e));
            throw new ManagementServerException("Failed to provision firewall rules for the Kubernetes " +
                    "cluster: " + kubernetesCluster.getName());
        }

        if (clusterVMIds != null && !clusterVMIds.isEmpty()) { // Upscaling, add new port-forwarding rules
            // Apply port forwarding only to new VMs
            final long publicIpId = publicIp.getId();
            final long networkId = kubernetesCluster.getNetworkId();
            final long accountId = account.getId();
            final long domainId = account.getDomainId();
            for (int i = 0; i < clusterVMIds.size(); ++i) {
                long vmId = clusterVMIds.get(i);
                Nic vmNic = networkModel.getNicInNetwork(vmId, kubernetesCluster.getNetworkId());
                final Ip vmIp = new Ip(vmNic.getIPv4Address());
                final long vmIdFinal = vmId;
                final int srcPortFinal = existingFirewallRuleSourcePortEnd + 1 + i;

                try {
                    PortForwardingRuleVO pfRule = Transaction.execute(new TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>() {
                        @Override
                        public PortForwardingRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                            PortForwardingRuleVO newRule =
                                    new PortForwardingRuleVO(null, publicIpId,
                                            srcPortFinal, srcPortFinal,
                                            vmIp,
                                            22, 22,
                                            "tcp", networkId, accountId, domainId, vmIdFinal);
                            newRule.setDisplay(true);
                            newRule.setState(FirewallRule.State.Add);
                            newRule = portForwardingRulesDao.persist(newRule);
                            return newRule;
                        }
                    });
                    rulesService.applyPortForwardingRules(publicIp.getId(), account);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Provisioning SSH port forwarding rule from port 2222 to 22 on " + publicIp.getAddress() +
                                " to the VM IP :" + vmIp + " in Kubernetes cluster " + kubernetesCluster.getName());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to activate SSH port forwarding rules for the Kubernetes cluster " + kubernetesCluster.getName() + " due to " + e);
                    throw new ManagementServerException("Failed to activate SSH port forwarding rules for the cluster: " + kubernetesCluster.getName(), e);
                }
            }
        }
    }

    private boolean validateNetwork(Network network) {
        NetworkOffering networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
        if (networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("This network is for system use only, network id " + network.getId());
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)) {
            throw new InvalidParameterValueException("This network does not support userdata that is required for k8s, network id " + network.getId());
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.Firewall)) {
            throw new InvalidParameterValueException("This network does not support firewall that is required for k8s, network id " + network.getId());
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.PortForwarding)) {
            throw new InvalidParameterValueException("This network does not support port forwarding that is required for k8s, network id " + network.getId());
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)) {
            throw new InvalidParameterValueException("This network does not support dhcp that is required for k8s, network id " + network.getId());
        }

        List<? extends IpAddress> addrs = networkModel.listPublicIpsAssignedToGuestNtwk(network.getId(), true);
        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            throw new InvalidParameterValueException("The network ID:" + network.getId() + " does not have source NAT ip assoicated with it. " +
                    "To provision a Kubernetes Cluster, a isolated network with source NAT is required.");
        } else {
            for (IpAddress addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = ipAddressDao.findById(addr.getId());
                }
            }
            if (sourceNatIp == null) {
                throw new InvalidParameterValueException("The network ID:" + network.getId() + " does not have source NAT ip assoicated with it. " +
                        "To provision a Kubernetes Cluster, a isolated network with source NAT is required.");
            }
        }
        List<FirewallRuleVO> rules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO rule : rules) {
            Integer startPort = rule.getSourcePortStart();
            Integer endPort = rule.getSourcePortEnd();
            LOGGER.debug("Network rule : " + startPort + " " + endPort);
            if (startPort <= 6443 && 6443 <= endPort) {
                throw new InvalidParameterValueException("The network ID:" + network.getId() + " has conflicting firewall rules to provision" +
                        " Kubernetes cluster.");
            }
        }

        rules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.PortForwarding);
        for (FirewallRuleVO rule : rules) {
            Integer startPort = rule.getSourcePortStart();
            Integer endPort = rule.getSourcePortEnd();
            LOGGER.debug("Network rule : " + startPort + " " + endPort);
            if (startPort <= 6443 && 6443 <= endPort) {
                throw new InvalidParameterValueException("The network ID:" + network.getId() + " has conflicting port forwarding rules to provision" +
                        " Kubernetes cluster.");
            }
        }
        return true;
    }

    private boolean validateServiceOffering(ServiceOffering serviceOffering) {
        if (serviceOffering.isDynamic()) {
            throw new InvalidParameterValueException(String.format("Custom service offerings are not supported for creating clusters, service offering ID: %s", serviceOffering.getUuid()));
        }
        if (serviceOffering.getCpu() < 2 || serviceOffering.getRamSize() < 2048) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, Kubernetes cluster template(CoreOS) needs minimum 2 vCPUs and 2 GB RAM", serviceOffering.getUuid()));
        }
        return true;
    }

    private void validateDockerRegistryParams(final String dockerRegistryUserName,
                                              final String dockerRegistryPassword,
                                              final String dockerRegistryUrl,
                                              final String dockerRegistryEmail) {
        // if no params related to docker registry specified then nothing to validate so return true
        if ((dockerRegistryUserName == null || dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword == null || dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl == null || dockerRegistryUrl.isEmpty()) &&
                (dockerRegistryEmail == null || dockerRegistryEmail.isEmpty())) {
            return;
        }

        // all params related to docker registry must be specified or nothing
        if (!((dockerRegistryUserName != null && !dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword != null && !dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl != null && !dockerRegistryUrl.isEmpty()) &&
                (dockerRegistryEmail != null && !dockerRegistryEmail.isEmpty()))) {
            throw new InvalidParameterValueException("All the docker private registry parameters (username, password, url, email) required are specified");
        }

        try {
            URL url = new URL(dockerRegistryUrl);
        } catch (MalformedURLException e) {
            throw new InvalidParameterValueException("Invalid docker registry url specified");
        }

        Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(dockerRegistryEmail);
        if (!matcher.find()) {
            throw new InvalidParameterValueException("Invalid docker registry email specified");
        }
    }

    private DeployDestination plan(final long nodesCount, final long dcId, final ServiceOffering offering) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        List<HostVO> hosts = resourceManager.listAllHostsInOneZoneByType(Type.Routing, dcId);
        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
            hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found = false;
        for (int i = 1; i <= nodesCount + 1; i++) {
            suitable_host_found = false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO h = hp.first();
                int reserved = hp.second();
                reserved++;
                ClusterVO cluster = clusterDao.findById(h.getClusterId());
                ClusterDetailsVO cluster_detail_cpu = clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking host " + h.getId() + " for capacity already reserved " + reserved);
                }
                if (capacityManager.checkIfHostHasCapacity(h.getId(), cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found host " + h.getId() + " has enough capacity cpu = " + cpu_requested * reserved + " ram =" + ram_requested * reserved);
                    }
                    hostEntry.setValue(new Pair<HostVO, Integer>(h, reserved));
                    suitable_host_found = true;
                    break;
                }
            }
            if (suitable_host_found) {
                continue;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Suitable hosts not found in datacenter " + dcId + " for node " + i);
                }
                break;
            }
        }
        if (suitable_host_found) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Suitable hosts found in datacenter " + dcId + " creating deployment destination");
            }
            return new DeployDestination(dataCenterDao.findById(dcId), null, null, null);
        }
        String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%1$s memory=%2$s)",
                cpu_requested * nodesCount, ram_requested * nodesCount);
        LOGGER.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, dcId);
    }

    private DeployDestination plan(final KubernetesCluster kubernetesCluster) throws InsufficientServerCapacityException {
        return plan(kubernetesCluster.getId(), kubernetesCluster.getZoneId());
    }

    private DeployDestination plan(final long kubernetesClusterId, final long dcId) throws InsufficientServerCapacityException {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        ServiceOffering offering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking deployment destination for kubernetesClusterId= " + kubernetesClusterId + " in dcId=" + dcId);
        }

        return plan(kubernetesCluster.getTotalNodeCount(), dcId, offering);
    }

    private boolean isAddOnServiceRunning(Long clusterId, final String nameSpace, String svcName) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        //FIXME: whole logic needs revamp. Assumption that management server has public network access is not practical
        IPAddressVO publicIp = null;
        List<IPAddressVO> ips = ipAddressDao.listByAssociatedNetwork(kubernetesCluster.getNetworkId(), true);
        publicIp = ips.get(0);
        Runtime r = Runtime.getRuntime();
        int nodePort = 0;
        try {
            Boolean devel = Boolean.valueOf(globalConfigDao.getValue("developer"));
            String keyFile = String.format("%s/.ssh/id_rsa", System.getProperty("user.home"));
            if (devel) {
                keyFile += ".cloud";
            }
            File pkFile = new File(keyFile);
            String cmd = "sudo kubectl get pods --all-namespaces";
            if (!Strings.isNullOrEmpty(nameSpace)) {
                cmd = String.format("sudo kubectl get pods --namespace=%s", nameSpace);
            }
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core",
                    pkFile, null, cmd,
                    10000, 10000, 10000);
            if (result.first() && !Strings.isNullOrEmpty(result.second())) {
                String[] lines = result.second().split("\n");
                for (String line :
                        lines) {
                    if (line.contains(svcName) && line.contains("Running")) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Service :" + svcName + " for the Kubernetes cluster "
                                    + kubernetesCluster.getName() + " is running");
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("KUBECTL: " + e);
        }
        return false;
    }

    private boolean cleanupKubernetesClusterResources(Long kubernetesClusterId) throws ManagementServerException {
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (!(cluster.getState().equals(KubernetesCluster.State.Running)
                || cluster.getState().equals(KubernetesCluster.State.Stopped)
                || cluster.getState().equals(KubernetesCluster.State.Alert)
                || cluster.getState().equals(KubernetesCluster.State.Error)
                || cluster.getState().equals(KubernetesCluster.State.Destroying))) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cannot perform delete operation on cluster:" + cluster.getName() + " in state " + cluster.getState());
            }
            throw new PermissionDeniedException("Cannot perform delete operation on cluster: " + cluster.getName() + " in state " + cluster.getState());
        }
        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.DestroyRequested);
        boolean failedVmDestroy = false;
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(cluster.getId());
        if ((clusterVMs != null) && !clusterVMs.isEmpty()) {
            for (KubernetesClusterVmMapVO clusterVM : clusterVMs) {
                long vmID = clusterVM.getVmId();

                // delete only if VM exists and is not removed
                UserVmVO userVM = userVmDao.findById(vmID);
                if (userVM == null || userVM.isRemoved()) {
                    continue;
                }
                try {
                    UserVm vm = userVmService.destroyVm(vmID, true);
                    if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                        LOGGER.warn(String.format("VM '%s' with uuid '%s' should have been expunging by now but is '%s'... retrying..."
                                , vm.getInstanceName()
                                , vm.getUuid()
                                , vm.getState().toString()));
                    }
                    vm = userVmService.expungeVm(vmID);
                    if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                        LOGGER.error(String.format("VM '%s' is now in state '%s'. I will probably fail at deleting it's cluster."
                                , vm.getInstanceName()
                                , vm.getState().toString()));
                    }
                    kubernetesClusterVmMapDao.expunge(clusterVM.getId());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Destroyed VM: " + userVM.getInstanceName() + " as part of cluster: " + cluster.getName() + " destroy.");
                    }
                } catch (Exception e) {
                    failedVmDestroy = true;
                    LOGGER.warn("Failed to destroy VM :" + userVM.getInstanceName() + " part of the cluster: " + cluster.getName() +
                            " due to " + e);
                    LOGGER.warn("Moving on with destroying remaining resources provisioned for the cluster: " + cluster.getName());
                }
            }
        }
        boolean cleanupNetwork = true;
        try {
            final KubernetesClusterDetailsVO clusterDetails = kubernetesClusterDetailsDao.findDetail(kubernetesClusterId, "networkCleanup");
            cleanupNetwork = Boolean.parseBoolean(clusterDetails.getValue());
        } catch (Exception e) {}

        // if there are VM's that were not expunged, we can not delete the network
        if (!failedVmDestroy) {
            if (cleanupNetwork) {
                if(clusterVMs!=null  && !clusterVMs.isEmpty()) { // Wait for few seconds to get all VMs really expunged
                    final int maxRetries = 3;
                    int retryCounter = 0;
                    while (retryCounter < maxRetries) {
                        boolean allVMsRemoved = true;
                        for (KubernetesClusterVmMap clusterVM : clusterVMs) {
                            UserVmVO userVM = userVmDao.findById(clusterVM.getVmId());
                            if (userVM != null && !userVM.isRemoved()) {
                                allVMsRemoved = false;
                                break;
                            }
                        }
                        if (allVMsRemoved) {
                            break;
                        }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {}
                        retryCounter++;
                    }
                }
                NetworkVO network = null;
                try {
                    network = networkDao.findById(cluster.getNetworkId());
                    if (network != null && network.getRemoved() == null) {
                        Account owner = accountManager.getAccount(network.getAccountId());
                        User callerUser = accountManager.getActiveUser(CallContext.current().getCallingUserId());
                        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
                        boolean networkDestroyed = networkMgr.destroyNetwork(cluster.getNetworkId(), context, true);
                        if (!networkDestroyed) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Failed to destroy network: " + cluster.getNetworkId() +
                                        " as part of cluster: " + cluster.getName() + " destroy");
                            }
                            processFailedNetworkDelete(kubernetesClusterId);
                            throw new ManagementServerException("Failed to delete the network as part of Kubernetes cluster name:" + cluster.getName() + " clean up");
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Destroyed network: " + network.getName() + " as part of cluster: " + cluster.getName() + " destroy");
                        }
                    }
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Failed to destroy network: " + cluster.getNetworkId() +
                                " as part of cluster: " + cluster.getName() + "  destroy due to " + e);
                    }
                    processFailedNetworkDelete(kubernetesClusterId);
                    throw new ManagementServerException("Failed to delete the network as part of Kubernetes cluster name:" + cluster.getName() + " clean up");
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("There are VM's that are not expunged in Kubernetes cluster " + cluster.getName());
            }
            processFailedNetworkDelete(kubernetesClusterId);
            throw new ManagementServerException("Failed to destroy one or more VM's as part of Kubernetes cluster name:" + cluster.getName() + " clean up");
        }

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationSucceeded);

        cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        cluster.setCheckForGc(false);
        kubernetesClusterDao.update(cluster.getId(), cluster);

        kubernetesClusterDao.remove(cluster.getId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Kubernetes cluster name:" + cluster.getName() + " is successfully deleted");
        }

        return true;
    }

    private void processFailedNetworkDelete(long kubernetesClusterId) {
        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        cluster.setCheckForGc(true);
        kubernetesClusterDao.update(cluster.getId(), cluster);
    }

    private UserVm createKubernetesMaster(final KubernetesClusterVO kubernetesCluster, final List<IPAddressVO> ips) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {
        UserVm masterVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        VirtualMachineTemplate template = templateDao.findById(kubernetesCluster.getTemplateId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Account owner = accountDao.findById(kubernetesCluster.getAccountId());
        final String masterIp = ipAddressManager.acquireGuestIpAddress(networkDao.findById(kubernetesCluster.getNetworkId()), null);
        Network.IpAddresses addrs = new Network.IpAddresses(masterIp, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = kubernetesCluster.getName() + "-k8s-master";
        boolean haSupported = false;
        final KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version != null) {
            try {
                if (KubernetesVersionManagerImpl.compareKubernetesVersion(version.getKubernetesVersion(), MIN_KUBERNETES_VERSION_HA_SUPPORT) >= 0) {
                    haSupported = true;
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to compare Kubernetes version for cluster version ID: %s with %s", version.getUuid(), MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }
        String k8sMasterConfig = null;
        try {
            k8sMasterConfig = readResourceFile("/conf/k8s-master.yml");
            final String apiServerCert = "{{ k8s_master.apiserver.crt }}";
            final String apiServerKey = "{{ k8s_master.apiserver.key }}";
            final String caCert = "{{ k8s_master.ca.crt }}";
            final String sshPubKey = "{{ k8s.ssh.pub.key }}";
            final String clusterToken = "{{ k8s_master.cluster.token }}";
            final String clusterInitArgsKey = "{{ k8s_master.cluster.initargs }}";
            final List<String> addresses = new ArrayList<>();
            addresses.add(masterIp);
            for (final IPAddressVO ip : ips) {
                addresses.add(ip.getAddress().addr());
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
            String pubKey = "- \"" + globalConfigDao.getValue("ssh.publickey") + "\"";
            String sshKeyPair = kubernetesCluster.getKeyPair();
            if (!Strings.isNullOrEmpty(sshKeyPair)) {
                SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
                if (sshkp != null) {
                    pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
                }
            }
            k8sMasterConfig = k8sMasterConfig.replace(sshPubKey, pubKey);
            k8sMasterConfig = k8sMasterConfig.replace(clusterToken, generateClusterToken(kubernetesCluster));
            String initArgs = "";
            if (haSupported) {
                initArgs = String.format("--control-plane-endpoint %s:6443 --upload-certs --certificate-key %s ",
                        ips.get(0).getAddress().addr(),
                        generateClusterHACertificateKey(kubernetesCluster));
            }
            initArgs += String.format("--apiserver-cert-extra-sans=%s", ips.get(0).getAddress().addr());
            k8sMasterConfig = k8sMasterConfig.replace(clusterInitArgsKey, initArgs);
        } catch (Exception e) {
            LOGGER.error("Failed to read Kubernetes master configuration file due to " + e);
            throw new ManagementServerException("Failed to read Kubernetes master configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sMasterConfig.getBytes(Charset.forName("UTF-8")));
        masterVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, kubernetesCluster.getDescription(), null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                null, addrs, null, null, null, customParameterMap, null, null, null, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created master VM: " + hostName + " in the Kubernetes cluster: " + kubernetesCluster.getName());
        }
        return masterVm;
    }

    private UserVm createKubernetesAdditionalMaster(final KubernetesClusterVO kubernetesCluster, final String joinIp, final int additionalMasterNodeInstance) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {
        UserVm nodeVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        VirtualMachineTemplate template = templateDao.findById(kubernetesCluster.getTemplateId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Account owner = accountDao.findById(kubernetesCluster.getAccountId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = String.format("%s-k8s-master-%s", kubernetesCluster.getName(), additionalMasterNodeInstance);
        String k8sMasterConfig = null;
        try {
            k8sMasterConfig = readResourceFile("/conf/k8s-master-add.yml");
            final String joinIpKey = "{{ k8s_master.join_ip }}";
            final String clusterTokenKey = "{{ k8s_master.cluster.token }}";
            final String sshPubKey = "{{ k8s.ssh.pub.key }}";
            final String clusterHACertificateKey = "{{ k8s_master.cluster.ha.certificate.key }}";
            String pubKey = "- \"" + globalConfigDao.getValue("ssh.publickey") + "\"";
            String sshKeyPair = kubernetesCluster.getKeyPair();
            if (!Strings.isNullOrEmpty(sshKeyPair)) {
                SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
                if (sshkp != null) {
                    pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
                }
            }
            k8sMasterConfig = k8sMasterConfig.replace(sshPubKey, pubKey);
            k8sMasterConfig = k8sMasterConfig.replace(joinIpKey, joinIp);
            k8sMasterConfig = k8sMasterConfig.replace(clusterTokenKey, generateClusterToken(kubernetesCluster));
            k8sMasterConfig = k8sMasterConfig.replace(clusterHACertificateKey, generateClusterHACertificateKey(kubernetesCluster));
        } catch (Exception e) {
            LOGGER.warn("Failed to read node configuration file due to " + e);
            throw new ManagementServerException("Failed to read cluster node configuration file.", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sMasterConfig.getBytes(Charset.forName("UTF-8")));
        nodeVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, kubernetesCluster.getDescription(), null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                null, addrs, null, null, null, customParameterMap, null, null, null, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created cluster additional master node VM: " + hostName + " in the Kubernetes cluster: " + kubernetesCluster.getName());
        }
        return nodeVm;
    }

    private UserVm createKubernetesNode(KubernetesClusterVO kubernetesCluster, String joinIp, int nodeInstance) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {
        UserVm nodeVm = null;
        DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        VirtualMachineTemplate template = templateDao.findById(kubernetesCluster.getTemplateId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(kubernetesCluster.getNetworkId());
        Account owner = accountDao.findById(kubernetesCluster.getAccountId());
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        long rootDiskSize = kubernetesCluster.getNodeRootDiskSize();
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (rootDiskSize > 0) {
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSize));
        }
        String hostName = String.format("%s-k8s-node-%s", kubernetesCluster.getName(), nodeInstance);
        String k8sNodeConfig = null;
        try {
            k8sNodeConfig = readResourceFile("/conf/k8s-node.yml");
            final String sshPubKey = "{{ k8s.ssh.pub.key }}";
            final String joinIpKey = "{{ k8s_master.join_ip }}";
            final String clusterTokenKey = "{{ k8s_master.cluster.token }}";
            String pubKey = "- \"" + globalConfigDao.getValue("ssh.publickey") + "\"";
            String sshKeyPair = kubernetesCluster.getKeyPair();
            if (!Strings.isNullOrEmpty(sshKeyPair)) {
                SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
                if (sshkp != null) {
                    pubKey += "\n  - \"" + sshkp.getPublicKey() + "\"";
                }
            }
            k8sNodeConfig = k8sNodeConfig.replace(sshPubKey, pubKey);
            k8sNodeConfig = k8sNodeConfig.replace(joinIpKey, joinIp);
            k8sNodeConfig = k8sNodeConfig.replace(clusterTokenKey, generateClusterToken(kubernetesCluster));
            /* genarate /.docker/config.json file on the nodes only if Kubernetes cluster is created to
             * use docker private registry */
            String dockerUserName = null;
            String dockerPassword = null;
            String dockerRegistryUrl = null;
            String dockerRegistryEmail = null;
            List<KubernetesClusterDetailsVO> details = kubernetesClusterDetailsDao.listDetails(kubernetesCluster.getId());
            for (KubernetesClusterDetailsVO detail : details) {
                if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_USER_NAME)) {
                    dockerUserName = detail.getValue();
                }
                if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_PASSWORD)) {
                    dockerPassword = detail.getValue();
                }
                if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_URL)) {
                    dockerRegistryUrl = detail.getValue();
                }
                if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_EMAIL)) {
                    dockerRegistryEmail = detail.getValue();
                }
            }
            if (!Strings.isNullOrEmpty(dockerUserName) && !Strings.isNullOrEmpty(dockerPassword)) {
                // do write file for  /.docker/config.json through the code instead of k8s-node.yml as we can no make a section
                // optional or conditionally applied
                String dockerConfigString = "write-files:\n" +
                        "  - path: /.docker/config.json\n" +
                        "    owner: core:core\n" +
                        "    permissions: '0644'\n" +
                        "    content: |\n" +
                        "      {\n" +
                        "        \"auths\": {\n" +
                        "          {{docker.url}}: {\n" +
                        "            \"auth\": {{docker.secret}},\n" +
                        "            \"email\": {{docker.email}}\n" +
                        "          }\n" +
                        "         }\n" +
                        "      }";
                k8sNodeConfig = k8sNodeConfig.replace("write-files:", dockerConfigString);
                final String dockerUrlKey = "{{docker.url}}";
                final String dockerAuthKey = "{{docker.secret}}";
                final String dockerEmailKey = "{{docker.email}}";
                final String usernamePasswordKey = dockerUserName + ":" + dockerPassword;
                String base64Auth = Base64.encodeBase64String(usernamePasswordKey.getBytes(Charset.forName("UTF-8")));
                k8sNodeConfig = k8sNodeConfig.replace(dockerUrlKey, "\"" + dockerRegistryUrl + "\"");
                k8sNodeConfig = k8sNodeConfig.replace(dockerAuthKey, "\"" + base64Auth + "\"");
                k8sNodeConfig = k8sNodeConfig.replace(dockerEmailKey, "\"" + dockerRegistryEmail + "\"");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read node configuration file due to " + e);
            throw new ManagementServerException("Failed to read cluster node configuration file.", e);
        }
        String base64UserData = Base64.encodeBase64String(k8sNodeConfig.getBytes(Charset.forName("UTF-8")));
        nodeVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, kubernetesCluster.getDescription(), null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, kubernetesCluster.getKeyPair(),
                null, addrs, null, null, null, customParameterMap, null, null, null, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created cluster node VM: " + hostName + " in the Kubernetes cluster: " + kubernetesCluster.getName());
        }
        return nodeVm;
    }

    private void startKubernetesVM(final UserVm vm, final KubernetesClusterVO kubernetesCluster) throws ServerApiException {

        try {
            StartVMCmd startVm = new StartVMCmd();
            startVm = ComponentContext.inject(startVm);
            Field f = startVm.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(startVm, vm.getId());
            userVmService.startVirtualMachine(startVm);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Started VM in the Kubernetes cluster: " + kubernetesCluster.getName());
            }
        } catch (ConcurrentOperationException ex) {
            LOGGER.warn("Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName() + " due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName(), ex);
        } catch (ResourceUnavailableException ex) {
            LOGGER.warn("Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName() + " due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName(), ex);
        } catch (InsufficientCapacityException ex) {
            LOGGER.warn("Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName() + " due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName(), ex);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName() + " due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName(), ex);
        } catch (Exception ex) {
            LOGGER.warn("Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName() + " due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the Kubernetes cluster name:" + kubernetesCluster.getName(), ex);
        }

        UserVm startVm = userVmDao.findById(vm.getId());
        if (!startVm.getState().equals(VirtualMachine.State.Running)) {
            LOGGER.warn("Failed to start VM instance.");
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM instance in Kubernetes cluster " + kubernetesCluster.getName());
        }
    }

    private void attachIsoKubernetesVMs(KubernetesCluster kubernetesCluster, List<Long> clusterVMIds) throws ServerApiException {
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version == null) {
            LOGGER.error(String .format("Unable to find Kubernetes version for cluster ID: %s", kubernetesCluster.getUuid()));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String .format("Unable to find Kubernetes version for cluster ID: %s", kubernetesCluster.getUuid()));
        }
        VMTemplateVO iso = templateDao.findById(version.getIsoId());
        if (iso == null) {
            LOGGER.error(String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Binaries ISO not found.",  kubernetesCluster.getUuid()));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Binaries ISO not found.",  kubernetesCluster.getUuid()));
        }
        if (!iso.getFormat().equals(Storage.ImageFormat.ISO)) {
            LOGGER.error(String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Invalid Binaries ISO.",  kubernetesCluster.getUuid()));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Invalid Binaries ISO.",  kubernetesCluster.getUuid()));
        }
        if (!iso.getState().equals(VirtualMachineTemplate.State.Active)) {
            LOGGER.error(String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Binaries ISO not active.",  kubernetesCluster.getUuid()));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to attach ISO to Kubernetes cluster ID: %s. Binaries ISO not active.",  kubernetesCluster.getUuid()));
        }
        for (int i = 0; i < clusterVMIds.size(); ++i) {
            UserVm vm = userVmDao.findById(clusterVMIds.get(i));
            try {
                templateService.attachIso(iso.getId(), vm.getId());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Attached binaries ISO for VM: %s in cluster: %s", vm.getUuid(), kubernetesCluster.getName()));
                }
            } catch (CloudRuntimeException ex) {
                LOGGER.warn(String.format("Failed to attach binaries ISO for VM: %s in the Kubernetes cluster name: %s due to Exception: ", vm.getDisplayName(), kubernetesCluster.getName()), ex);
                // throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to attach binaries ISO for VM: %s in the Kubernetes cluster name: %s", vm.getDisplayName(), kubernetesCluster.getName()), ex);
            }
        }
    }

    private void detachIsoKubernetesVMs(final KubernetesCluster kubernetesCluster, List<Long> clusterVMIds) throws ServerApiException {
        for (int i = 0; i < clusterVMIds.size(); ++i) {
            UserVm vm = userVmDao.findById(clusterVMIds.get(i));
            boolean result = false;
            try {
                result = templateService.detachIso(vm.getId());
            } catch (CloudRuntimeException ex) {
                LOGGER.warn(String.format("Failed to detach binaries ISO from VM ID: %s in the Kubernetes cluster ID: %s ", vm.getUuid(), kubernetesCluster.getUuid()), ex);
            }
            if (result) {
                LOGGER.debug(String.format("Detached Kubernetes binaries from VM ID: %s in the Kubernetes cluster ID: %s", vm.getUuid(), kubernetesCluster.getUuid()));
            } else {
                LOGGER.warn(String.format("Failed to detach binaries ISO from VM ID: %s in the Kubernetes cluster ID: %s ", vm.getUuid(), kubernetesCluster.getUuid()));
            }
        }
    }

    private void stopClusterVM(final KubernetesClusterVmMapVO vmMapVO) throws ServerApiException {
        try {
            userVmService.stopVirtualMachine(vmMapVO.getVmId(), false);
        } catch (ConcurrentOperationException ex) {
            LOGGER.warn("Failed to stop Kubernetes cluster VM due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public KubernetesClusterResponse createKubernetesClusterResponse(long kubernetesClusterId) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        KubernetesClusterResponse response = new KubernetesClusterResponse();
        response.setObjectName(KubernetesCluster.class.getSimpleName().toLowerCase());
        response.setId(kubernetesCluster.getUuid());
        response.setName(kubernetesCluster.getName());
        response.setDescription(kubernetesCluster.getDescription());
        DataCenterVO zone = ApiDBUtils.findZoneById(kubernetesCluster.getZoneId());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());
        response.setMasterNodes(kubernetesCluster.getMasterNodeCount());
        response.setClusterSize(kubernetesCluster.getNodeCount());
        VMTemplateVO template = ApiDBUtils.findTemplateById(kubernetesCluster.getTemplateId());
        response.setTemplateId(template.getUuid());
        ServiceOfferingVO offering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        response.setServiceOfferingId(offering.getUuid());
        response.setServiceOfferingName(offering.getName());
        KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version != null) {
            response.setKubernetesVersionId(version.getUuid());
            response.setKubernetesVersionName(version.getName());
        }
        response.setKeypair(kubernetesCluster.getKeyPair());
        response.setState(kubernetesCluster.getState().toString());
        response.setCores(String.valueOf(kubernetesCluster.getCores()));
        response.setMemory(String.valueOf(kubernetesCluster.getMemory()));
        NetworkVO ntwk = networkDao.findByIdIncludingRemoved(kubernetesCluster.getNetworkId());
        response.setEndpoint(kubernetesCluster.getEndpoint());
        response.setNetworkId(ntwk.getUuid());
        response.setAssociatedNetworkName(ntwk.getName());
        response.setConsoleEndpoint(kubernetesCluster.getConsoleEndpoint());
        List<String> vmIds = new ArrayList<String>();
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (vmList != null && !vmList.isEmpty()) {
            for (KubernetesClusterVmMapVO vmMapVO : vmList) {
                UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
                if (userVM != null) {
                    vmIds.add(userVM.getUuid());
                }
            }
        }
        response.setVirtualMachineIds(vmIds);
        return response;
    }

    protected boolean stateTransitTo(long kubernetesClusterId, KubernetesCluster.Event e) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        try {
            return _stateMachine.transitTo(kubernetesCluster, e, null, kubernetesClusterDao);
        } catch (NoTransitionException nte) {
            LOGGER.warn("Failed to transistion state of the Kubernetes cluster: " + kubernetesCluster.getName()
                    + " in state " + kubernetesCluster.getState().toString() + " on event " + e.toString());
            return false;
        }
    }

    @Override
    public KubernetesCluster createKubernetesCluster(CreateKubernetesClusterCmd cmd)
            throws InsufficientCapacityException, ResourceAllocationException, ManagementServerException {
        final String name = cmd.getName();
        final String displayName = cmd.getDisplayName();
        final Long zoneId = cmd.getZoneId();
        final Long kubernetesVersionId = cmd.getKubernetesVersionId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final Long networkId = cmd.getNetworkId();
        final String sshKeyPair= cmd.getSSHKeyPairName();
        final Long masterNodeCount = cmd.getMasterNodes();
        final Long clusterSize = cmd.getClusterSize();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final String dockerRegistryEmail = cmd.getDockerRegistryEmail();
        final Long nodeRootDiskSize = cmd.getNodeRootDiskSize();

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the Kubernetes cluster name:" + name);
        }

        if (masterNodeCount < 1 || masterNodeCount > 100) {
            throw new InvalidParameterValueException("Invalid cluster master nodes count " + masterNodeCount);
        }

        if (clusterSize < 1 || clusterSize > 100) {
            throw new InvalidParameterValueException("Invalid cluster size " + clusterSize);
        }

        DataCenter zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by ID:" + zoneId);
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone:" + zone.getId() + " is currently disabled.");
        }

        final KubernetesSupportedVersion clusterKubernetesVersion = kubernetesSupportedVersionDao.findById(kubernetesVersionId);
        if (clusterKubernetesVersion == null) {
            throw new InvalidParameterValueException("Unable to find given Kubernetes version in supported versions");
        }
        if (clusterKubernetesVersion.getZoneId() != null && !clusterKubernetesVersion.getZoneId().equals(zone.getId())) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s is not available for zone ID: %s", clusterKubernetesVersion.getUuid(), zone.getUuid()));
        }
        if (masterNodeCount > 1 ) {
            try {
                if (KubernetesVersionManagerImpl.compareKubernetesVersion(clusterKubernetesVersion.getKubernetesVersion(), MIN_KUBERNETES_VERSION_HA_SUPPORT) < 0) {
                    throw new InvalidParameterValueException(String.format("HA support is available only for Kubernetes version %s and above. Given version ID: %s is %s", MIN_KUBERNETES_VERSION_HA_SUPPORT, clusterKubernetesVersion.getUuid(), clusterKubernetesVersion.getKubernetesVersion()));
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to compare Kubernetes version for given version ID: %s with %s", clusterKubernetesVersion.getUuid(), MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }

        if (clusterKubernetesVersion.getZoneId() != null && clusterKubernetesVersion.getZoneId() != zone.getId()) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s is not available for zone ID: %s", clusterKubernetesVersion.getUuid(), zone.getUuid()));
        }

        ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("No service offering with ID:" + serviceOfferingId);
        } else {
        }

        if (sshKeyPair != null && !sshKeyPair.isEmpty()) {
            SSHKeyPairVO sshKeyPairVO = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshKeyPairVO == null) {
                throw new InvalidParameterValueException("Given SSH key pair with name:" + sshKeyPair + " was not found for the account " + owner.getAccountName());
            }
        }

        if (!isKubernetesServiceConfigured(zone)) {
            throw new ManagementServerException("Kubernetes service has not been configured properly to provision Kubernetes clusters.");
        }

        VMTemplateVO template = templateDao.findByTemplateName(globalConfigDao.getValue(KubernetesServiceConfig.KubernetesClusterTemplateName.key()));
        List<VMTemplateZoneVO> listZoneTemplate = templateZoneDao.listByZoneTemplate(zone.getId(), template.getId());
        if (listZoneTemplate == null || listZoneTemplate.isEmpty()) {
            LOGGER.warn("The template:" + template.getId() + " is not available for use in zone:" + zoneId + " to provision Kubernetes cluster name:" + name);
            throw new ManagementServerException("Kubernetes service has not been configured properly to provision Kubernetes clusters.");
        }

        if (!validateServiceOffering(serviceOfferingDao.findById(serviceOfferingId))) {
            throw new InvalidParameterValueException("This service offering is not suitable for k8s cluster, service offering id is " + networkId);
        }

        validateDockerRegistryParams(dockerRegistryUserName, dockerRegistryPassword, dockerRegistryUrl, dockerRegistryEmail);

        plan(masterNodeCount + clusterSize, zoneId, serviceOfferingDao.findById(serviceOfferingId));

        Network network = null;
        if (networkId != null) {
            if (kubernetesClusterDao.listByNetworkId(networkId).isEmpty()) {
                network = networkService.getNetwork(networkId);
                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by ID " + networkId);
                }
                if (!validateNetwork(network)) {
                    throw new InvalidParameterValueException("This network is not suitable for k8s cluster, network id is " + networkId);
                }
                networkModel.checkNetworkPermissions(owner, network);
            } else {
                throw new InvalidParameterValueException("This network is already under use by another k8s cluster, network id is " + networkId);
            }
        } else { // user has not specified network in which cluster VM's to be provisioned, so create a network for Kubernetes cluster
            NetworkOfferingVO networkOffering = networkOfferingDao.findByUniqueName(
                    globalConfigDao.getValue(KubernetesServiceConfig.KubernetesClusterNetworkOffering.key()));

            long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
            PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating network for account " + owner + " from the network offering id=" +
                        networkOffering.getId() + " as a part of cluster: " + name + " deployment process");
            }

            try {
                network = networkMgr.createGuestNetwork(networkOffering.getId(), name + "-network", owner.getAccountName() + "-network",
                        null, null, null, false, null, owner, null, physicalNetwork, zone.getId(), ControlledEntity.ACLType.Account, null, null, null, null, true, null, null);
            } catch (Exception e) {
                LOGGER.warn("Unable to create a network for the Kubernetes cluster due to " + e);
                throw new ManagementServerException("Unable to create a network for the Kubernetes cluster.");
            }
        }

        final Network defaultNetwork = network;
        final VMTemplateVO finalTemplate = template;
        final long cores = serviceOffering.getCpu() * (masterNodeCount + clusterSize);
        final long memory = serviceOffering.getRamSize() * (masterNodeCount + clusterSize);

        final KubernetesClusterVO cluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
            @Override
            public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVO newCluster = new KubernetesClusterVO(name, displayName, zoneId, clusterKubernetesVersion.getId(),
                        serviceOfferingId, finalTemplate.getId(), defaultNetwork.getId(), owner.getDomainId(),
                        owner.getAccountId(), masterNodeCount, clusterSize, KubernetesCluster.State.Created, sshKeyPair, cores, memory, nodeRootDiskSize, "", "");
                kubernetesClusterDao.persist(newCluster);
                return newCluster;
            }
        });

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<KubernetesClusterDetailsVO> details = new ArrayList<>();
                if (!Strings.isNullOrEmpty(dockerRegistryUserName)) {
                    details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.DOCKER_REGISTRY_USER_NAME, dockerRegistryUserName, true));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryPassword)) {
                    details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.DOCKER_REGISTRY_PASSWORD, dockerRegistryPassword, false));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryUrl)) {
                    details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.DOCKER_REGISTRY_URL, dockerRegistryUrl, true));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryEmail)) {
                    details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.DOCKER_REGISTRY_EMAIL, dockerRegistryEmail, true));
                }
                details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.USERNAME, "admin", true));
                SecureRandom random = new SecureRandom();
                String randomPassword = new BigInteger(130, random).toString(32);
                details.add(new KubernetesClusterDetailsVO(cluster.getId(), ApiConstants.PASSWORD, randomPassword, false));
                details.add(new KubernetesClusterDetailsVO(cluster.getId(), "networkCleanup", String.valueOf(networkId == null), true));
                kubernetesClusterDetailsDao.saveDetails(details);
            }
        });

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("A Kubernetes cluster name:" + name + " ID:" + cluster.getId() + " has been created.");
        }

        return cluster;
    }


    // Start operation can be performed at two diffrent life stages of Kubernetes cluster. First when a freshly created cluster
    // in which case there are no resources provisisioned for the Kubernetes cluster. So during start all the resources
    // are provisioned from scratch. Second kind of start, happens on  Stopped Kubernetes cluster, in which all resources
    // are provisioned (like volumes, nics, networks etc). It just that VM's are not in running state. So just
    // start the VM's (which can possibly implicitly start the network also).
    @Override
    public boolean startKubernetesCluster(long kubernetesClusterId, boolean onCreate) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        if (onCreate) {
            // Start for Kubernetes cluster in 'Created' state
            return startKubernetesClusterOnCreate(kubernetesClusterId);
        } else {
            // Start for Kubernetes cluster in 'Stopped' state. Resources are already provisioned, just need to be started
            return startStoppedKubernetesCluster(kubernetesClusterId);
        }
    }

    @Override
    public boolean stopKubernetesCluster(long kubernetesClusterId) throws ManagementServerException {

        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new ManagementServerException("Failed to find Kubernetes cluster ID: " + kubernetesClusterId);
        }

        if (kubernetesCluster.getRemoved() != null) {
            throw new ManagementServerException("Kubernetes cluster ID:" + kubernetesClusterId + " is already deleted.");
        }

        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Kubernetes cluster ID: " + kubernetesClusterId + " is already stopped.");
            }
            return true;
        }

        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopping)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Kubernetes cluster ID: " + kubernetesClusterId + " is getting stopped.");
            }
            return true;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stopping Kubernetes cluster: " + kubernetesCluster.getName());
        }

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.StopRequested);

        for (final KubernetesClusterVmMapVO vmMapVO : kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId)) {
            final UserVmVO vm = userVmDao.findById(vmMapVO.getVmId());
            try {
                if (vm == null) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ManagementServerException("Failed to start all VMs in Kubernetes cluster ID: " + kubernetesClusterId);
                }
                stopClusterVM(vmMapVO);
            } catch (ServerApiException ex) {
                LOGGER.warn("Failed to stop VM in Kubernetes cluster ID:" + kubernetesClusterId + " due to " + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }

        for (final KubernetesClusterVmMapVO vmMapVO : kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId)) {
            final UserVmVO vm = userVmDao.findById(vmMapVO.getVmId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Stopped)) {
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                throw new ManagementServerException("Failed to stop all VMs in Kubernetes cluster ID: " + kubernetesClusterId);
            }
        }

        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationSucceeded);
        return true;
    }

    @Override
    public boolean deleteKubernetesCluster(Long kubernetesClusterId) throws ManagementServerException {
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Invalid cluster id specified");
        }
        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, cluster);
        return cleanupKubernetesClusterResources(kubernetesClusterId);
    }

    @Override
    public ListResponse<KubernetesClusterResponse> listKubernetesClusters(ListKubernetesClustersCmd cmd) {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long clusterId = cmd.getId();
        final String state = cmd.getState();
        final String name = cmd.getName();

        List<KubernetesClusterResponse> responsesList = new ArrayList<KubernetesClusterResponse>();
        if (state != null && !state.isEmpty()) {
            if (!KubernetesCluster.State.Running.toString().equals(state) &&
                    !KubernetesCluster.State.Stopped.toString().equals(state) &&
                    !KubernetesCluster.State.Destroyed.toString().equals(state)) {
                throw new InvalidParameterValueException("Invalid value for Kubernetes cluster state is specified");
            }
        }
        if (clusterId != null) {
            KubernetesClusterVO cluster = kubernetesClusterDao.findById(clusterId);
            if (cluster == null) {
                throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
            }
            accountManager.checkAccess(caller, SecurityChecker.AccessType.ListEntry, false, cluster);
            responsesList.add(createKubernetesClusterResponse(clusterId));
        } else {
            SearchCriteria<KubernetesClusterVO> sc = kubernetesClusterDao.createSearchCriteria();
            Filter searchFilter = new Filter(KubernetesClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
            if (state != null && !state.isEmpty()) {
                sc.addAnd("state", SearchCriteria.Op.EQ, state);
            }
            if (accountManager.isNormalUser(caller.getId())) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, caller.getAccountId());
            } else if (accountManager.isDomainAdmin(caller.getId())) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, caller.getDomainId());
            }
            if (name != null && !name.isEmpty()) {
                sc.addAnd("name", SearchCriteria.Op.LIKE, name);
            }
            List<KubernetesClusterVO> kubernetesClusters = kubernetesClusterDao.search(sc, searchFilter);
            for (KubernetesClusterVO cluster : kubernetesClusters) {
                KubernetesClusterResponse clusterResponse = createKubernetesClusterResponse(cluster.getId());
                responsesList.add(clusterResponse);
            }
        }
        ListResponse<KubernetesClusterResponse> response = new ListResponse<KubernetesClusterResponse>();
        response.setResponses(responsesList);
        return response;
    }

    public KubernetesClusterConfigResponse getKubernetesClusterConfig(GetKubernetesClusterConfigCmd cmd) {
        final Long clusterId = cmd.getId();
        KubernetesCluster kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
        }
        KubernetesClusterConfigResponse response = new KubernetesClusterConfigResponse();
        response.setId(kubernetesCluster.getUuid());
        response.setName(kubernetesCluster.getName());
        String configData = "";
        try {
            configData = new String(Base64.decodeBase64(kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "kubeConfigData").getValue()));
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to retrieve Kubernetes config for cluster ID: %s", kubernetesCluster.getUuid()), e);
        }
        response.setConfigData(configData);
        response.setObjectName("clusterconfig");
        return response;
    }

    @Override
    public boolean scaleKubernetesCluster(ScaleKubernetesClusterCmd cmd) throws ManagementServerException, ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {
        final Long kubernetesClusterId = cmd.getId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Long clusterSize = cmd.getClusterSize();
        if (kubernetesClusterId == null || kubernetesClusterId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null || kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }

        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);

        if (serviceOfferingId == null && clusterSize == null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster ID: %s cannot be scaled, either a new service offering or a new cluster size must be passed", kubernetesCluster.getUuid()));
        }

        ServiceOffering serviceOffering = null;
        if (serviceOfferingId != null) {
            serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Failed to find service offering ID: " + serviceOfferingId);
            } else {
                if (serviceOffering.isDynamic()) {
                    throw new InvalidParameterValueException(String.format("Custom service offerings are not supported for Kubernetes clusters. Kubernetes cluster ID: %s, service offering ID: %s", kubernetesCluster.getUuid(), serviceOffering.getUuid()));
                }
                if (serviceOffering.getCpu() < 2 || serviceOffering.getRamSize() < 2048) {
                    throw new InvalidParameterValueException(String.format("Kubernetes cluster ID: %s cannot be scaled with service offering ID: %s, Kubernetes cluster template(CoreOS) needs minimum 2 vCPUs and 2 GB RAM", kubernetesCluster.getUuid(), serviceOffering.getUuid()));
                }
            }
        }

        if (!(kubernetesCluster.getState().equals(KubernetesCluster.State.Created) ||
                kubernetesCluster.getState().equals(KubernetesCluster.State.Running) ||
                kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped))) {
            throw new PermissionDeniedException(String.format("Kubernetes cluster ID: %s is in %s state", kubernetesCluster.getUuid(), kubernetesCluster.getState().toString()));
        }

        if (clusterSize != null) {
            if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) { // Cannot scale stopped cluster currently for cluster size
                throw new PermissionDeniedException(String.format("Kubernetes cluster ID: %s is in %s state", kubernetesCluster.getUuid(), kubernetesCluster.getState().toString()));
            }
            if (clusterSize < 1) {
                throw new InvalidParameterValueException(String.format("Kubernetes cluster ID: %s cannot be scaled for size, %d", kubernetesCluster.getUuid(), clusterSize));
            }
        }

        LOGGER.debug(String.format("Scaling Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));

        final KubernetesCluster.State clusterState = kubernetesCluster.getState();
        final long originalNodeCount = kubernetesCluster.getNodeCount();

        final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        if (existingServiceOffering == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, service offering for the Kubernetes cluster not found!", kubernetesCluster.getUuid()));
        }
        final boolean serviceOfferingScalingNeeded = serviceOffering != null && serviceOffering.getId() != existingServiceOffering.getId();
        final boolean clusterSizeScalingNeeded = clusterSize != null && clusterSize != originalNodeCount;
        if (serviceOfferingScalingNeeded) {
            List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
            if (vmList == null || vmList.isEmpty() || vmList.size() - 1 < originalNodeCount) {
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, it is in unstable state as not enough existing VM instances found!", kubernetesCluster.getUuid()));
            } else {
                for (KubernetesClusterVmMapVO vmMapVO : vmList) {
                    VMInstanceVO vmInstance = vmInstanceDao.findById(vmMapVO.getVmId());
                    if (vmInstance != null && vmInstance.getState().equals(VirtualMachine.State.Running) && vmInstance.getHypervisorType() != Hypervisor.HypervisorType.XenServer && vmInstance.getHypervisorType() != Hypervisor.HypervisorType.VMware && vmInstance.getHypervisorType() != Hypervisor.HypervisorType.Simulator) {
                        LOGGER.info("Scaling the VM dynamically is not supported for VMs running on Hypervisor " + vmInstance.getHypervisorType());
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, scaling Kubernetes cluster with running VMs on hypervisor %s is not supported!", kubernetesCluster.getUuid(), vmInstance.getHypervisorType()));
                    }
                }
            }
            if (serviceOffering.getRamSize() < existingServiceOffering.getRamSize() ||
                    serviceOffering.getCpu()*serviceOffering.getSpeed() < existingServiceOffering.getCpu()*existingServiceOffering.getSpeed()) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, service offering for the Kubernetes cluster cannot be scaled down!", kubernetesCluster.getUuid()));
            }

            // ToDo: Check capacity with new service offering at this point, how?

            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.ScaleUpRequested);

            final long size = (clusterSize == null ? kubernetesCluster.getNodeCount() : clusterSize);
            final long cores = serviceOffering.getCpu() * size;
            final long memory = serviceOffering.getRamSize() * size;
            kubernetesCluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
                @Override
                public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                    KubernetesClusterVO updatedCluster = kubernetesClusterDao.createForUpdate(kubernetesClusterId);
                    updatedCluster.setNodeCount(size);
                    updatedCluster.setCores(cores);
                    updatedCluster.setMemory(memory);
                    updatedCluster.setServiceOfferingId(serviceOfferingId);
                    kubernetesClusterDao.persist(updatedCluster);
                    return updatedCluster;
                }
            });
            if (kubernetesCluster == null) {
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to update Kubernetes cluster!", kubernetesCluster.getUuid()));
            }
            kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
            final long tobeScaledVMCount = Math.min(vmList.size(), size+1);
            for (long i=0; i<tobeScaledVMCount; i++) {
                KubernetesClusterVmMapVO vmMapVO = vmList.get((int)i);
                UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
                boolean result = false;
                try {
                    result = userVmManager.upgradeVirtualMachine(userVM.getId(), serviceOffering.getId(), new HashMap<String, String>());
                } catch (Exception e) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to scale cluster VM ID: %s! %s", kubernetesCluster.getUuid(), userVM.getUuid(), e.getMessage()), e);
                }
                if (!result) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to scale cluster VM ID: %s!", kubernetesCluster.getUuid(), userVM.getUuid()));
                }
            }
        }

        if (clusterSizeScalingNeeded) {
            // Check capacity and transition state
            final long newVmRequiredCount = clusterSize - originalNodeCount;
            final ServiceOffering clusterServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
            if (clusterServiceOffering == null) {
                String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, service offering not found", kubernetesCluster.getUuid());
                LOGGER.error(msg);
                stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
            }
            if (newVmRequiredCount > 0) {
                if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.ScaleUpRequested);
                }
                try {
                    if (clusterState.equals(KubernetesCluster.State.Running)) {
                        plan(newVmRequiredCount, kubernetesCluster.getZoneId(), clusterServiceOffering);
                    } else {
                        plan(kubernetesCluster.getTotalNodeCount() + newVmRequiredCount, kubernetesCluster.getZoneId(), clusterServiceOffering);
                    }
                } catch (InsufficientCapacityException e) {
                    String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, insufficient capacity", kubernetesCluster.getUuid());
                    LOGGER.error(msg);
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, msg, e);
                }
            } else {
                if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.ScaleDownRequested);
                }
            }

            if (!serviceOfferingScalingNeeded) { // Else already updated
                // Update KubernetesClusterVO
                final long cores = clusterServiceOffering.getCpu() * clusterSize;
                final long memory = clusterServiceOffering.getRamSize() * clusterSize;

                kubernetesCluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
                    @Override
                    public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                        KubernetesClusterVO updatedCluster = kubernetesClusterDao.createForUpdate(kubernetesClusterId);
                        updatedCluster.setNodeCount(clusterSize);
                        updatedCluster.setCores(cores);
                        updatedCluster.setMemory(memory);
                        kubernetesClusterDao.persist(updatedCluster);
                        return updatedCluster;
                    }
                });
                if (kubernetesCluster == null) {
                    String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update cluster", kubernetesCluster.getUuid());
                    LOGGER.warn(msg);
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
                }
                kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
            }

            // Perform size scaling
            if (clusterState.equals(KubernetesCluster.State.Running)) {
                List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
                if (vmList == null || vmList.isEmpty() || vmList.size() - 1 < originalNodeCount) {
                    String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, t is in unstable state as not enough existing VM instances found", kubernetesCluster.getUuid());
                    LOGGER.error(msg);
                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
                }
                IPAddressVO publicIp = null;
                List<IPAddressVO> ips = ipAddressDao.listByAssociatedNetwork(kubernetesCluster.getNetworkId(), true);
                if (ips == null || ips.isEmpty() || ips.get(0) == null) {
                    String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, unable to retrieve associated public IP", kubernetesCluster.getUuid());
                    LOGGER.error(msg);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
                }
                publicIp = ips.get(0);
                Boolean devel = Boolean.valueOf(globalConfigDao.getValue("developer"));
                String keyFile = String.format("%s/.ssh/id_rsa", System.getProperty("user.home"));
                if (devel) {
                    keyFile += ".cloud";
                }
                File pkFile = new File(keyFile);
                Account account = accountDao.findById(kubernetesCluster.getAccountId());
                if (newVmRequiredCount < 0) { // downscale
                    int i = vmList.size() - 1;
                    while (i > 1 && vmList.size() > clusterSize + 1) { // Reverse order as first VM will be k8s master
                        KubernetesClusterVmMapVO vmMapVO = vmList.get(i);
                        UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());

                        // Gracefully remove-delete k8s node
                        int retryCounter = 0;
                        int maxRetries = 3;
                        while (retryCounter < maxRetries) {
                            retryCounter++;
                            try {
                                Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core",
                                        pkFile, null, String.format("sudo kubectl drain %s --ignore-daemonsets --delete-local-data", userVM.getHostName()),
                                        10000, 10000, 30000);
                                if (!result.first()) {
                                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Draining Kubernetes node unsuccessful");
                                }
                                result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core",
                                        pkFile, null, String.format("sudo kubectl delete node %s", userVM.getHostName()),
                                        10000, 10000, 30000);
                                if (!result.first()) {
                                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Deleting Kubernetes node unsuccessful");
                                }
                                break;
                            } catch (Exception e) {
                                if (retryCounter < maxRetries) {
                                    try {
                                        Thread.sleep(30000);
                                    } catch (InterruptedException ie) {}
                                } else {
                                    String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, failed to remove Kubernetes node", kubernetesCluster.getUuid());
                                    LOGGER.warn(msg, e);
                                    stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
                                }
                            }
                        }

                        // Remove port-forwarding network rules
                        List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByNetwork(kubernetesCluster.getNetworkId());
                        for (PortForwardingRuleVO pfRule : pfRules) {
                            if (pfRule.getVirtualMachineId() == userVM.getId()) {
                                portForwardingRulesDao.remove(pfRule.getId());
                                break;
                            }
                        }
                        rulesService.applyPortForwardingRules(publicIp.getId(), account);

                        // Expunge VM
                        UserVm vm = userVmService.destroyVm(userVM.getId(), true);
                        if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, VM '%s' is now in state '%s'."
                                    , kubernetesCluster.getUuid()
                                    , vm.getInstanceName()
                                    , vm.getState().toString()));
                        }
                        vm = userVmService.expungeVm(userVM.getId());
                        if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, VM '%s' is now in state '%s'."
                                    , kubernetesCluster.getUuid()
                                    , vm.getInstanceName()
                                    , vm.getState().toString()));
                        }

                        // Expunge cluster VMMapVO
                        kubernetesClusterVmMapDao.expunge(vmMapVO.getId());

                        i--;
                    }

                    // Scale network rules to update firewall rule
                    try {
                        scaleKubernetesClusterNetworkRules(publicIp, account, kubernetesClusterId, null);
                    } catch (Exception e) {
                        String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update network rules", kubernetesCluster.getUuid());
                        LOGGER.error(msg, e);
                        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
                    }
                } else { // upscale, same node count handled above
                    UserVmVO masterVm = userVmDao.findById(vmList.get(0).getVmId());
                    String masterIP = masterVm.getPrivateIpAddress();
                    List<Long> clusterVMIds = new ArrayList<>();

                    // Create new node VMs
                    for (int i = (int) originalNodeCount + 1; i <= clusterSize; i++) {
                        UserVm vm = null;
                        try {
                            vm = createKubernetesNode(kubernetesCluster, masterIP, i);
                            addKubernetesClusterVm(kubernetesCluster.getId(), vm .getId());
                            startKubernetesVM(vm, kubernetesCluster);
                            clusterVMIds.add(vm.getId());
                            LOGGER.debug(String.format("Provisioned a node VM in to the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
                        } catch (Exception e) {
                            String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, unable to provision node VM in the cluster", kubernetesCluster.getUuid());
                            LOGGER.error(msg, e);
                            stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
                        }
                    }

                    // Scale network rules to update firewall rule and add port-forwarding rules
                    try {
                        scaleKubernetesClusterNetworkRules(publicIp, account, kubernetesClusterId, clusterVMIds);
                    } catch (Exception e) {
                        String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update network rules", kubernetesCluster.getUuid());
                        LOGGER.error(msg, e);
                        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
                    }

                    // Attach binaries ISO to new VMs
                    attachIsoKubernetesVMs(kubernetesCluster, clusterVMIds);

                    // Check if new nodes are added in k8s cluster
                    int retryCounter = 0;
                    int maxRetries = 15; // Max wait for 30 mins as online install can take time, same as while creating cluster
                    while (retryCounter < maxRetries) {
                        try {
                            Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core",
                                    pkFile, null, "sudo kubectl get nodes -o json | jq \".items[].metadata.name\" | wc -l",
                                    20000, 10000, 30000);
                            if (result.first()) {
                                int nodesCount = Integer.parseInt(result.second().trim());
                                if (nodesCount == kubernetesCluster.getTotalNodeCount()) {
                                    LOGGER.debug(String.format("Scaling finished successfully for Kubernetes cluster ID: %s, new nodes are ready now", kubernetesCluster.getUuid()));
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to retrieve node count for Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
                        }
                        try {
                            LOGGER.debug(String.format("Waiting 30s for scaling to finish and new nodes to become ready for Kubernetes cluster ID: %s. %d/%d", kubernetesCluster.getUuid(), retryCounter+1, maxRetries));
                            Thread.sleep(30000);
                        } catch (InterruptedException ex) {
                            LOGGER.warn(String.format("Failed to wait for 30s for scaling to finish and new nodes to become ready for Kubernetes cluster ID: %s. %d/%d", kubernetesCluster.getUuid(), retryCounter+1, maxRetries), ex);
                        }
                        retryCounter++;
                    }

                    // Detach binaries ISO from new VMs
                    detachIsoKubernetesVMs(kubernetesCluster, clusterVMIds);

                    // Throw exception if nodes count for k8s cluster timed out
                    if (retryCounter >= maxRetries) { // Scaling failed
                        String msg = String.format("Scaling unsuccessful for Kubernetes cluster ID: %s as it does not have desired number of nodes in ready state", kubernetesCluster.getUuid());
                        LOGGER.warn(msg);
                        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
                    }
                }
            }
        }
        stateTransitTo(kubernetesClusterId, KubernetesCluster.Event.OperationSucceeded);
        return true;
    }

    @Override
    public boolean upgradeKubernetesCluster(UpgradeKubernetesClusterCmd cmd) throws ManagementServerException {
        // Validate parameters
        final Long kubernetesClusterId = cmd.getId();
        final Long upgradeVersionId = cmd.getKubernetesVersionId();
        if (kubernetesClusterId == null || kubernetesClusterId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        if (upgradeVersionId == null || upgradeVersionId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes version ID");
        }
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null || kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        if (!KubernetesCluster.State.Running.equals(kubernetesCluster.getState())) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster ID: %s is not in running state", kubernetesCluster.getUuid()));
        }
        KubernetesSupportedVersionVO upgradeVersion = kubernetesSupportedVersionDao.findById(upgradeVersionId);
        if (upgradeVersion == null || upgradeVersion.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version ID");
        }
        KubernetesSupportedVersionVO clusterVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (clusterVersion == null || clusterVersion.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Invalid Kubernetes version associated with cluster ID: %s",
                    kubernetesCluster.getUuid()));
        }
        if (KubernetesVersionManagerImpl.compareKubernetesVersion(
                upgradeVersion.getKubernetesVersion(), clusterVersion.getKubernetesVersion()) <= 0) {
            throw new InvalidParameterValueException(String.format("Invalid Kubernetes version associated with cluster ID: %s",
                    kubernetesCluster.getUuid()));
        }
        // Check upgradeVersion is either path upgrade or immediate minor upgrade
        try {
            KubernetesVersionManagerImpl.canUpgradeKubernetesVersion(clusterVersion.getKubernetesVersion(), upgradeVersion.getKubernetesVersion());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(e.getMessage());
        }

        // Get public IP
        IPAddressVO publicIp = null;
        List<IPAddressVO> ips = ipAddressDao.listByAssociatedNetwork(kubernetesCluster.getNetworkId(), true);
        if (CollectionUtils.isEmpty(ips)) {
            String msg = String.format("Upgrade failed for Kubernetes cluster ID: %s, unable to retrieve associated public IP", kubernetesCluster.getUuid());
            LOGGER.warn(msg);
            throw new ManagementServerException(msg);
        }
        publicIp = ips.get(0);

        kubernetesCluster.setKubernetesVersionId(upgradeVersion.getId());
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (CollectionUtils.isEmpty(clusterVMs)) {
            String msg = String.format("Upgrade failed for Kubernetes cluster ID: %s, unable to retrieve VMs for cluster", kubernetesCluster.getUuid());
            throw new ManagementServerException(msg);
        }
        List<Long> vmIds = new ArrayList<>();
        for (KubernetesClusterVmMapVO vmMap : clusterVMs) {
            vmIds.add(vmMap.getVmId());
        }
        Collections.sort(vmIds);

        boolean devel = Boolean.parseBoolean(globalConfigDao.getValue("developer"));
        String keyFile = String.format("%s/.ssh/id_rsa", System.getProperty("user.home"));
        if (devel) {
            keyFile += ".cloud";
        }
        File pkFile = new File(keyFile);

        File upgradeScriptFile = null;
        try {
            String upgradeScriptData = readResourceFile("/script/upgrade-kubernetes.sh");
            upgradeScriptFile = File.createTempFile("upgrade-kuberntes", ".sh");
            BufferedWriter upgradeScriptFileWriter = new BufferedWriter(new FileWriter(upgradeScriptFile));
            upgradeScriptFileWriter.write(upgradeScriptData);
            upgradeScriptFileWriter.close();
        } catch (IOException e) {
            String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to prepare upgrade script", kubernetesCluster.getUuid());
            LOGGER.error(msg, e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
        }

        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.UpgradeRequested);

        // Attach ISO
        attachIsoKubernetesVMs(kubernetesCluster, vmIds);

        // Upgrade master
        Pair<Boolean, String> result = null;
        for (int i = 0; i < vmIds.size(); ++i) {
            UserVm vm = userVmDao.findById(vmIds.get(i));
            result = null;
            try {
                result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core", pkFile, null,
                        String.format("sudo kubectl drain %s --ignore-daemonsets --delete-local-data", vm.getHostName()),
                        10000, 10000, 60000);
            } catch (Exception e) {
                String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to drain Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                LOGGER.error(msg, e);
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
            }
            if (!result.first()) {
                String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to drain Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                LOGGER.error(String.format("%s. Output:\n%s", msg, result.second()));
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
            }
            try {
                SshHelper.scpTo(publicIp.getAddress().addr(), 2222 + i, "core", pkFile, null,
                        "~/", upgradeScriptFile.getAbsolutePath(), "0755");

                result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222 + i, "core", pkFile, null,
                        String.format("sudo ./%s %s", upgradeScriptFile.getName(), i == 0 ? upgradeVersion.getKubernetesVersion() : "''"),
                        10000, 10000, 5 * 60 * 1000);
            } catch (Exception e) {
                String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to upgrade Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                LOGGER.error(msg, e);
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
            }
            if (!result.first()) {
                String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to upgrade Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                LOGGER.error(String.format("%s. Output:\n%s", msg, result.second()));
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
            }
            int retryCounter = 0;
            int maxRetries = 3;
            while (retryCounter < maxRetries) {
                try {
                    result = SshHelper.sshExecute(publicIp.getAddress().addr(), 2222, "core", pkFile, null,
                            String.format("sudo kubectl uncordon %s", vm.getHostName()),
                            10000, 10000, 30000);
                } catch (Exception e) {
                    String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to uncordon Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                    LOGGER.error(msg, e);
                    stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg, e);
                }
                if (result.first()) {
                    break;
                }
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ie) {
                    LOGGER.warn(String.format("Error while waiting for uncordon Kubernetes cluster ID: %s  node running on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid()), ie);
                }
                retryCounter++;
            }
            if (!result.first()) {
                String msg = String.format("Failed to upgrade Kubernetes cluster ID: %s, unable to uncordon Kubernetes node on VM ID: %s", kubernetesCluster.getUuid(), vm.getUuid());
                LOGGER.error(String.format("%s. Output:\n%s", msg, result.second()));
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
            }
        }

        // Detach ISO
        detachIsoKubernetesVMs(kubernetesCluster, vmIds);

        boolean updated = kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesCluster);
        if (!updated) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        } else {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        }
        return updated;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateKubernetesClusterCmd.class);
        cmdList.add(StartKubernetesClusterCmd.class);
        cmdList.add(StopKubernetesClusterCmd.class);
        cmdList.add(DeleteKubernetesClusterCmd.class);
        cmdList.add(ListKubernetesClustersCmd.class);
        cmdList.add(GetKubernetesClusterConfigCmd.class);
        cmdList.add(ScaleKubernetesClusterCmd.class);
        cmdList.add(UpgradeKubernetesClusterCmd.class);
        return cmdList;
    }

    // Garbage collector periodically run through the Kubernetes clusters marked for GC. For each Kubernetes cluster
    // marked for GC, attempt is made to destroy cluster.
    public class KubernetesClusterGarbageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("KubernetesCluster.GC.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                List<KubernetesClusterVO> kubernetesClusters = kubernetesClusterDao.findKubernetesClustersToGarbageCollect();
                for (KubernetesCluster kubernetesCluster : kubernetesClusters) {
                    LOGGER.debug(String.format("Running Kubernetes cluster garbage collector on Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
                    try {
                        if (cleanupKubernetesClusterResources(kubernetesCluster.getId())) {
                            LOGGER.debug(String.format("Garbage collection complete for Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
                        } else {
                            LOGGER.warn(String.format("Garbage collection failed for Kubernetes cluster ID: %s, it will be attempted to garbage collected in next run", kubernetesCluster.getUuid()));
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to destroy Kubernetes cluster ID: %s during GC", kubernetesCluster.getUuid()), e);
                        // proceed further with rest of the Kubernetes cluster garbage collection
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Kubernetes cluster gc: ", e);
            }
        }
    }

    /* Kubernetes cluster scanner checks if the Kubernetes cluster is in desired state. If it detects Kubernetes cluster
       is not in desired state, it will trigger an event and marks the Kubernetes cluster to be 'Alert' state. For e.g a
       Kubernetes cluster in 'Running' state should mean all the cluster of node VM's in the custer should be running and
       number of the node VM's should be of cluster size, and the master node VM's is running. It is possible due to
       out of band changes by user or hosts going down, we may end up one or more VM's in stopped state. in which case
       scanner detects these changes and marks the cluster in 'Alert' state. Similarly cluster in 'Stopped' state means
       all the cluster VM's are in stopped state any mismatch in states should get picked up by Kubernetes cluster and
       mark the Kubernetes cluster to be 'Alert' state. Through recovery API, or reconciliation clusters in 'Alert' will
       be brought back to known good state or desired state.
     */
    public class KubernetesClusterStatusScanner extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("KubernetesCluster.State.Scanner.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                // run through Kubernetes clusters in 'Running' state and ensure all the VM's are Running in the cluster
                List<KubernetesClusterVO> runningKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Running);
                for (KubernetesCluster kubernetesCluster : runningKubernetesClusters) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Running Kubernetes cluster state scanner on Kubernetes cluster name:" + kubernetesCluster.getName());
                    }
                    try {
                        if (!isClusterInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to run through VM states of Kubernetes cluster due to " + e);
                    }
                }

                // run through Kubernetes clusters in 'Stopped' state and ensure all the VM's are Stopped in the cluster
                List<KubernetesClusterVO> stoppedKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Stopped);
                for (KubernetesCluster kubernetesCluster : stoppedKubernetesClusters) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Running Kubernetes cluster state scanner on Kubernetes cluster name:" + kubernetesCluster.getName() + " for state " + KubernetesCluster.State.Stopped);
                    }
                    try {
                        if (!isClusterInDesiredState(kubernetesCluster, VirtualMachine.State.Stopped)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to run through VM states of Kubernetes cluster due to " + e);
                    }
                }

                // run through Kubernetes clusters in 'Alert' state and reconcile state as 'Running' if the VM's are running
                List<KubernetesClusterVO> alertKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Alert);
                for (KubernetesCluster kubernetesCluster : alertKubernetesClusters) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Running Kubernetes cluster state scanner on Kubernetes cluster name:" + kubernetesCluster.getName() + " for state " + KubernetesCluster.State.Alert);
                    }
                    try {
                        if (isClusterInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            // mark the cluster to be running
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.RecoveryRequested);
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to run through VM states of Kubernetes cluster status scanner due to " + e);
                    }
                }

                // run through Kubernetes clusters in 'Starting' state and reconcile state as 'Running' or 'Error' if the VM's are running
                List<KubernetesClusterVO> startingKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Starting);
                for (KubernetesCluster kubernetesCluster : startingKubernetesClusters) {
                    if (!Strings.isNullOrEmpty(kubernetesCluster.getEndpoint()) ||
                            (new Date()).getTime() - kubernetesCluster.getCreated().getTime() < 10*60*1000) {
                        continue;
                    }
                    LOGGER.debug(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster ID: %s for state: %s", kubernetesCluster.getUuid(), KubernetesCluster.State.Starting.toString()));
                    try {
                        if (isClusterInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            // mark the cluster to be running
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run through VM states of Kubernetes cluster ID: %s status scanner", kubernetesCluster.getUuid()), e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Kubernetes cluster state scanner", e);
            }
        }
    }

    // checks if Kubernetes cluster is in desired state
    boolean isClusterInDesiredState(KubernetesCluster kubernetesCluster, VirtualMachine.State state) {
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());

        // check cluster is running at desired capacity include master nodes as well
        if (clusterVMs.size() < kubernetesCluster.getTotalNodeCount()) {
            LOGGER.debug(String.format("Found only %d VMs in the Kubernetes cluster ID: %s while expected %d VMs to be in state: %s",
                    clusterVMs.size(), kubernetesCluster.getUuid(), kubernetesCluster.getTotalNodeCount(), state.toString()));
            return false;
        }
        // check if all the VM's are in same state
        for (KubernetesClusterVmMapVO clusterVm : clusterVMs) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(clusterVm.getVmId());
            if (vm.getState() != state) {
                LOGGER.debug(String.format("Found VM ID: %s in the Kubernetes cluster ID: %s in state: %s while expected to be in state: %s. So moving the cluster to Alert state for reconciliation",
                        vm.getUuid(), kubernetesCluster.getUuid(), vm.getState().toString(), state.toString()));
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean start() {
        _gcExecutor.scheduleWithFixedDelay(new KubernetesClusterGarbageCollector(), 300, 300, TimeUnit.SECONDS);
        _stateScanner.scheduleWithFixedDelay(new KubernetesClusterStatusScanner(), 300, 30, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _gcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Kubernetes-Cluster-Scavenger"));
        _stateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Kubernetes-Cluster-State-Scanner"));

        return true;
    }
}
