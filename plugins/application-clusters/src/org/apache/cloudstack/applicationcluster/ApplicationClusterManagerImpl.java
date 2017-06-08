/*
 * Copyright 2016 ShapeBlue Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.applicationcluster;

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
import com.cloud.network.IpAddress;
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
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.ssl.CertService;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.applicationcluster.CreateApplicationClusterCmd;
import org.apache.cloudstack.api.command.user.applicationcluster.DeleteApplicationClusterCmd;
import org.apache.cloudstack.api.command.user.applicationcluster.ListApplicationClusterCACertCmd;
import org.apache.cloudstack.api.command.user.applicationcluster.ListApplicationClusterCmd;
import org.apache.cloudstack.api.command.user.applicationcluster.StartApplicationClusterCmd;
import org.apache.cloudstack.api.command.user.applicationcluster.StopApplicationClusterCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.response.ApplicationClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterDao;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterDetailsDao;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterVmMapDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.security.auth.x500.X500Principal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//SubjectKeyIdentifierStructure;

@Local(value = {ApplicationClusterManager.class})
public class ApplicationClusterManagerImpl extends ManagerBase implements ApplicationClusterManager, ApplicationClusterService {

    private static final Logger s_logger = Logger.getLogger(ApplicationClusterManagerImpl.class);

    protected StateMachine2<ApplicationCluster.State, ApplicationCluster.Event, ApplicationCluster> _stateMachine = ApplicationCluster.State.getStateMachine();

    ScheduledExecutorService _gcExecutor;
    ScheduledExecutorService _stateScanner;

    @Inject
    protected KeystoreDao keystoreDao;
    @Inject
    protected ApplicationClusterDao _applicationClusterDao;
    @Inject
    protected ApplicationClusterVmMapDao _clusterVmMapDao;
    @Inject
    protected ApplicationClusterDetailsDao _applicationClusterDetailsDao;
    @Inject
    protected SSHKeyPairDao _sshKeyPairDao;
    @Inject
    protected UserVmService _userVmService;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected ServiceOfferingDao _offeringDao;
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected ConfigurationDao _globalConfigDao;
    @Inject
    protected NetworkService _networkService;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    protected NetworkOrchestrationService _networkMgr;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected IPAddressDao _publicIpAddressDao;
    @Inject
    protected PortForwardingRulesDao _portForwardingDao;
    @Inject
    private FirewallService _firewallService;
    @Inject
    protected RulesService _rulesService;
    @Inject
    private NetworkOfferingServiceMapDao _ntwkOfferingServiceMapDao;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected ApplicationClusterVmMapDao _applicationClusterVmMapDao;
    @Inject
    protected ServiceOfferingDao _srvOfferingDao;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private VMTemplateZoneDao _templateZoneDao;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected ClusterDetailsDao _clusterDetailsDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    protected CertService certService;

    @Override
    public ApplicationCluster findById(final Long id) {
        return _applicationClusterDao.findById(id);
    }

    @Override
    public ApplicationCluster createContainerCluster(final String name,
                                                 final String displayName,
                                                 final Long zoneId,
                                                 final Long serviceOfferingId,
                                                 final Account owner,
                                                 final Long networkId,
                                                 final String sshKeyPair,
                                                 final Long clusterSize,
                                                 final String dockerRegistryUserName,
                                                 final String dockerRegistryPassword,
                                                 final String dockerRegistryUrl,
                                                 final String dockerRegistryEmail)
            throws InsufficientCapacityException, ResourceAllocationException, ManagementServerException {

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the container cluster name:" + name);
        }

        if (clusterSize < 1 || clusterSize > 100) {
            throw new InvalidParameterValueException("invalid cluster size " + clusterSize);
        }

        DataCenter zone =  _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id:" + zoneId);
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone:" + zone.getId() + " is currently disabled.");
        }

        ServiceOffering serviceOffering = _srvOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("No service offering with id:" + serviceOfferingId);
        }

        if(sshKeyPair != null && !sshKeyPair.isEmpty()) {
            SSHKeyPairVO sshkp = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp == null) {
                throw new InvalidParameterValueException("Given SSH key pair with name:" + sshKeyPair + " was not found for the account " + owner.getAccountName());
            }
        }

        if (!isApplicationClusterServiceConfigured(zone)) {
            throw new ManagementServerException("Container service has not been configured properly to provision container clusters.");
        }

        VMTemplateVO template = _templateDao.findByTemplateName(_globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterTemplateName.key()));
        List<VMTemplateZoneVO> listZoneTemplate = _templateZoneDao.listByZoneTemplate(zone.getId(), template.getId());
        if (listZoneTemplate == null || listZoneTemplate.isEmpty()) {
            s_logger.warn("The template:" + template.getId() + " is not available for use in zone:" + zoneId + " to provision container cluster name:" + name);
            throw new ManagementServerException("Container service has not been configured properly to provision container clusters.");
        }

        if (!validateServiceOffering(_srvOfferingDao.findById(serviceOfferingId))) {
            throw new InvalidParameterValueException("This service offering is not suitable for an application cluster, service offering id is " + networkId);
        }

        validateDockerRegistryParams(dockerRegistryUserName, dockerRegistryPassword, dockerRegistryUrl, dockerRegistryEmail);

        plan(clusterSize, zoneId, _srvOfferingDao.findById(serviceOfferingId));

        Network network = null;
        if (networkId != null) {
            if (_applicationClusterDao.listByNetworkId(networkId).isEmpty()) {
                network = _networkService.getNetwork(networkId);
                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by ID " + networkId);
                }
                if (!validateNetwork(network)){
                    throw new InvalidParameterValueException("This network is not suitable for an application cluster, network id is " + networkId);
                }
                _networkModel.checkNetworkPermissions(owner, network);
            }
            else {
                throw new InvalidParameterValueException("This network is already under use by another application cluster, network id is " + networkId);
            }
        } else { // user has not specified network in which cluster VM's to be provisioned, so create a network for container cluster
            NetworkOfferingVO networkOffering = _networkOfferingDao.findByUniqueName(
                    _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterNetworkOffering.key()));

            long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
            PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating network for account " + owner + " from the network offering id=" +
                        networkOffering.getId() + " as a part of cluster: " + name + " deployment process");
            }

            try {
                network = _networkMgr.createGuestNetwork(networkOffering.getId(), name + "-network", owner.getAccountName() + "-network",
                        null, null, null, null, owner, null, physicalNetwork, zone.getId(), ControlledEntity.ACLType.Account, null, null, null, null, true, null);
            } catch(Exception e) {
                s_logger.warn("Unable to create a network for the container cluster due to " + e);
                throw new ManagementServerException("Unable to create a network for the container cluster.");
            }
        }

        final Network defaultNetwork = network;
        final VMTemplateVO finalTemplate = template;
        final long cores = serviceOffering.getCpu() * clusterSize;
        final long memory = serviceOffering.getRamSize() * clusterSize;

        final ApplicationClusterVO cluster = Transaction.execute(new TransactionCallback<ApplicationClusterVO>() {
            @Override
            public ApplicationClusterVO doInTransaction(TransactionStatus status) {
                ApplicationClusterVO newCluster = new ApplicationClusterVO(name, displayName, zoneId,
                        serviceOfferingId, finalTemplate.getId(), defaultNetwork.getId(), owner.getDomainId(),
                        owner.getAccountId(), clusterSize, ApplicationCluster.State.Created, sshKeyPair, cores, memory, "", "");
                _applicationClusterDao.persist(newCluster);
                return newCluster;
            }
        });

        Transaction.execute(new TransactionCallback<ApplicationClusterDetailsVO>() {
            @Override
            public ApplicationClusterDetailsVO doInTransaction(TransactionStatus status) {
                ApplicationClusterDetailsVO clusterDetails = new ApplicationClusterDetailsVO();
                clusterDetails.setClusterId(cluster.getId());
                clusterDetails.setRegistryUsername(dockerRegistryUserName);
                clusterDetails.setRegistryPassword(dockerRegistryPassword);
                clusterDetails.setRegistryUrl(dockerRegistryUrl);
                clusterDetails.setRegistryEmail(dockerRegistryEmail);
                clusterDetails.setUsername("admin");
                SecureRandom random = new SecureRandom();
                String randomPassword = new BigInteger(130, random).toString(32);
                clusterDetails.setPassword(randomPassword);
                clusterDetails.setNetworkCleanup(networkId == null);
                _applicationClusterDetailsDao.persist(clusterDetails);
                return clusterDetails;
            }
        });

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("A container cluster name:" + name + " id:" + cluster.getId() + " has been created.");
        }

        return cluster;
    }


    // Start operation can be performed at two diffrent life stages of container cluster. First when a freshly created cluster
    // in which case there are no resources provisisioned for the container cluster. So during start all the resources
    // are provisioned from scratch. Second kind of start, happens on  Stopped container cluster, in which all resources
    // are provisioned (like volumes, nics, networks etc). It just that VM's are not in running state. So just
    // start the VM's (which can possibly implicitly start the network also).
    @Override
    public boolean startContainerCluster(long containerClusterId, boolean onCreate) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        if (onCreate) {
            // Start for container cluster in 'Created' state
            return startContainerClusterOnCreate(containerClusterId);
        } else {
            // Start for container cluster in 'Stopped' state. Resources are already provisioned, just need to be started
            return startStoppedContainerCluster(containerClusterId);
        }
    }

    // perform a cold start (which will provision resources as well)
    private boolean startContainerClusterOnCreate(final long containerClusterId) throws ManagementServerException {

        // Starting a contriner cluster has below workflow
        //   - start the newtwork
        //   - provision the master /node VM
        //   - priovision node VM's (as many as cluster size)
        //   - update the booke keeping data of the VM's provisioned for the cluster
        //   - setup networking (add Firewall and PF rules)
        //   - wait till kubernetes API server on master VM to come up
        //   - wait till addon services (dashboard etc) to come up
        //   - update API and dashboard URL endpoints in container cluster details

        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting container cluster: " + containerCluster.getName());
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.StartRequested);

        Account account = _accountDao.findById(containerCluster.getAccountId());

        DeployDestination dest = null;
        try {
            dest = plan(containerClusterId, containerCluster.getZoneId());
        }
        catch (InsufficientCapacityException e){
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            s_logger.warn("Provisioning the cluster failed due to insufficient capacity in the container cluster: " + containerCluster.getName() + " due to " + e);
            throw new ManagementServerException("Provisioning the cluster failed due to insufficient capacity in the container cluster: " + containerCluster.getName(), e);
        }
        final ReservationContext context = new ReservationContextImpl(null, null, null, account);

        try {
            _networkMgr.startNetwork(containerCluster.getNetworkId(), dest, context);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network:" + containerCluster.getNetworkId() + " is started for the  container cluster: " + containerCluster.getName());
            }
        } catch (RuntimeException e) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            s_logger.warn("Starting the network failed as part of starting container cluster " + containerCluster.getName() + " due to " + e);
            throw new ManagementServerException("Failed to start the network while creating container cluster name:" + containerCluster.getName(), e);
        } catch(Exception e) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            s_logger.warn("Starting the network failed as part of starting container cluster " + containerCluster.getName() + " due to " + e);
            throw new ManagementServerException("Failed to start the network while creating container cluster name:" + containerCluster.getName(), e);
        }

        IPAddressVO publicIp = null;
        List<IPAddressVO> ips = _publicIpAddressDao.listByAssociatedNetwork(containerCluster.getNetworkId(), true);
        if (ips == null || ips.isEmpty()) {
            s_logger.warn("Network:" + containerCluster.getNetworkId() + " for the container cluster name:" + containerCluster.getName() + " does not have " +
                    "public IP's assocated with it. So aborting container cluster strat.");
            throw new ManagementServerException("Failed to start the network while creating container cluster name:" + containerCluster.getName());
        }
        publicIp = ips.get(0);

        UserVm masterNode = null;
        try {
            masterNode = createK8SMaster(containerCluster, publicIp);

            final long clusterId = containerCluster.getId();
            final long masterVmId = masterNode.getId();
            Transaction.execute(new TransactionCallback<ApplicationClusterVmMapVO>() {
                @Override
                public ApplicationClusterVmMapVO doInTransaction(TransactionStatus status) {
                    ApplicationClusterVmMapVO newClusterVmMap = new ApplicationClusterVmMapVO(clusterId, masterVmId);
                    _clusterVmMapDao.persist(newClusterVmMap);
                    return newClusterVmMap;
                }
            });

            startClusterVM(masterNode, containerCluster);
            masterNode = _vmDao.findById(masterNode.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Provisioned the master VM's in to the container cluster name:" + containerCluster.getName());
            }
        } catch (RuntimeException e) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            s_logger.warn("Provisioning the master VM' failed in the container cluster: " + containerCluster.getName() + " due to " + e);
            throw new ManagementServerException("Provisioning the master VM' failed in the container cluster: " + containerCluster.getName(), e);
        } catch (Exception e) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            s_logger.warn("Provisioning the master VM' failed in the container cluster: " + containerCluster.getName() + " due to " + e);
            throw new ManagementServerException("Provisioning the master VM' failed in the container cluster: " + containerCluster.getName(), e);
        }

        String masterIP = masterNode.getPrivateIpAddress();

        long anyNodeVmId = 0;
        UserVm k8anyNodeVM = null;
        for (int i=1; i <= containerCluster.getNodeCount(); i++) {
            UserVm vm = null;
            try {
                vm = createK8SNode(containerCluster, masterIP, i);
                final long nodeVmId = vm.getId();
                ApplicationClusterVmMapVO clusterNodeVmMap = Transaction.execute(new TransactionCallback<ApplicationClusterVmMapVO>() {
                    @Override
                    public ApplicationClusterVmMapVO doInTransaction(TransactionStatus status) {
                        ApplicationClusterVmMapVO newClusterVmMap = new ApplicationClusterVmMapVO(containerClusterId, nodeVmId);
                        _clusterVmMapDao.persist(newClusterVmMap);
                        return newClusterVmMap;
                    }
                });
                startClusterVM(vm, containerCluster);

                vm = _vmDao.findById(vm.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Provisioned a node VM in to the container cluster: " + containerCluster.getName());
                }
            } catch (RuntimeException e) {
                stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
                s_logger.warn("Provisioning the node VM failed in the container cluster " + containerCluster.getName() + " due to " + e);
                throw new ManagementServerException("Provisioning the node VM failed in the container cluster " + containerCluster.getName(), e);
            } catch (Exception e) {
                stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
                s_logger.warn("Provisioning the node VM failed in the container cluster " + containerCluster.getName() + " due to " + e);
                throw new ManagementServerException("Provisioning the node VM failed in the container cluster " + containerCluster.getName(), e);
            }

            if (anyNodeVmId == 0) {
                anyNodeVmId = vm.getId();
                k8anyNodeVM = vm;
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Container cluster : " + containerCluster.getName() + " VM's are successfully provisioned.");
        }

        setupContainerClusterNetworkRules(publicIp, account, containerClusterId, masterNode.getId());

        int retryCounter = 0;
        int maxRetries = 10;
        boolean k8sApiServerSetup = false;

        while (retryCounter < maxRetries) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(publicIp.getAddress().addr(), 443), 10000);
                k8sApiServerSetup = true;
                containerCluster = _applicationClusterDao.findById(containerClusterId);
                containerCluster.setEndpoint("https://" + publicIp.getAddress() + "/");
                _applicationClusterDao.update(containerCluster.getId(), containerCluster);
                break;
            } catch (IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Waiting for container cluster: " + containerCluster.getName() + " API endpoint to be available. retry: " + retryCounter + "/" + maxRetries);
                }
                try { Thread.sleep(50000); } catch (InterruptedException ex) {}
                retryCounter++;
            }
        }

        if (k8sApiServerSetup) {

            retryCounter = 0;
            maxRetries = 10;
            // Dashbaord service is a docker image downloaded at run time.
            // So wait for some time and check if dashbaord service is up running.
            while (retryCounter < maxRetries) {

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Waiting for dashboard service for the container cluster: " + containerCluster.getName()
                            + " to come up. Attempt: " + retryCounter + " of max retries " + maxRetries);
                }

                if (isAddOnServiceRunning(containerCluster.getId(), "kubernetes-dashboard")) {

                    stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationSucceeded);

                    containerCluster = _applicationClusterDao.findById(containerClusterId);
                    containerCluster.setConsoleEndpoint("https://" + publicIp.getAddress() + "/api/v1/proxy/namespaces/kube-system/services/kubernetes-dashboard");
                    _applicationClusterDao.update(containerCluster.getId(), containerCluster);

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Container cluster name:" + containerCluster.getName() + " is successfully started");
                    }

                    return true;
                }

                try { Thread.sleep(30000);} catch (InterruptedException ex) {}
                retryCounter++;
            }
            s_logger.warn("Failed to setup container cluster " + containerCluster.getName() + " in usable state as" +
                    " unable to bring dashboard add on service up");
        } else {
            s_logger.warn("Failed to setup container cluster " + containerCluster.getName() + " in usable state as" +
                    " unable to bring the API server up");
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);

        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Failed to deploy container cluster: " + containerCluster.getId() + " as unable to setup up in usable state");
    }

    private boolean startStoppedContainerCluster(long containerClusterId) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        final ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);
        if (containerCluster == null) {
            throw new ManagementServerException("Failed to find container cluster id: " + containerClusterId);
        }

        if (containerCluster.getRemoved() != null) {
            throw new ManagementServerException("Container cluster id:" + containerClusterId + " is already deleted.");
        }

        if (containerCluster.getState().equals(ApplicationCluster.State.Running) ){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Container cluster id: " + containerClusterId + " is already Running.");
            }
            return true;
        }

        if (containerCluster.getState().equals(ApplicationCluster.State.Starting) ){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Container cluster id: " + containerClusterId + " is getting started.");
            }
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting container cluster: " + containerCluster.getName());
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.StartRequested);

        for (final ApplicationClusterVmMapVO vmMapVO : _clusterVmMapDao.listByClusterId(containerClusterId)) {
            final UserVmVO vm = _userVmDao.findById(vmMapVO.getVmId());
            try {
                if (vm == null) {
                    stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
                    throw new ManagementServerException("Failed to start all VMs in container cluster id: " + containerClusterId);
                }
                startClusterVM(vm, containerCluster);
            } catch (ServerApiException ex) {
                s_logger.warn("Failed to start VM in container cluster id:" + containerClusterId + " due to " + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }

        for (final ApplicationClusterVmMapVO vmMapVO : _clusterVmMapDao.listByClusterId(containerClusterId)) {
            final UserVmVO vm = _userVmDao.findById(vmMapVO.getVmId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
                throw new ManagementServerException("Failed to start all VMs in container cluster id: " + containerClusterId);
            }
        }

        InetAddress address=null;
        try {
            address = InetAddress.getByName(new URL(containerCluster.getEndpoint()).getHost());
        } catch (MalformedURLException | UnknownHostException ex) {
            // API end point is generated by CCS, so this situation should not arise.
            s_logger.warn("Container cluster id:" + containerClusterId + " has invalid api endpoint. Can not " +
                    "verify if cluster is in ready state.");
            throw new ManagementServerException("Can not verify if container cluster id:" + containerClusterId + " is in usable state.");
        }

        // wait for fixed time for K8S api server to be avaialble
        int retryCounter = 0;
        int maxRetries = 10;
        boolean k8sApiServerSetup = false;
        while (retryCounter < maxRetries) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address.getHostAddress(), 443), 10000);
                k8sApiServerSetup = true;
                break;
            } catch (IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Waiting for container cluster: " + containerCluster.getName() + " API endpoint to be available. retry: " + retryCounter + "/" + maxRetries);
                }
                try { Thread.sleep(50000); } catch (InterruptedException ex) {}
                retryCounter++;
            }
        }

        if (!k8sApiServerSetup) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
            throw new ManagementServerException("Failed to setup container cluster id: " + containerClusterId + " is usable state.");
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationSucceeded);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(" Container cluster name:" + containerCluster.getName() + " is successfully started.");
        }
        return true;
    }

    // Open up  firewall port 443, secure port on which kubernetes API server is running. Also create portforwarding
    // rule to forward public IP traffic to master VM private IP
    private void setupContainerClusterNetworkRules(IPAddressVO publicIp, Account account, long containerClusterId,
                                                   long masterVmId) throws  ManagementServerException {

        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);

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
            startPortField.set(rule, new Integer(443));

            Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
            endPortField.setAccessible(true);
            endPortField.set(rule, new Integer(443));

            Field cidrField = rule.getClass().getDeclaredField("cidrlist");
            cidrField.setAccessible(true);
            cidrField.set(rule, sourceCidrList);

            _firewallService.createIngressFirewallRule(rule);
            _firewallService.applyIngressFwRules(publicIp.getId(), account);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Provisioned firewall rule to open up port 443 on " + publicIp.getAddress() +
                        " for cluster " + containerCluster.getName());
            }
        } catch (RuntimeException rte) {
            s_logger.warn("Failed to provision firewall rules for the container cluster: " + containerCluster.getName()
                    + " due to exception: " + getStackTrace(rte));
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to provision firewall rules for the container " +
                    "cluster: " + containerCluster.getName(), rte);
        } catch (Exception e) {
            s_logger.warn("Failed to provision firewall rules for the container cluster: " + containerCluster.getName()
                    + " due to exception: " + getStackTrace(e));
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to provision firewall rules for the container " +
                    "cluster: " + containerCluster.getName());
        }

        Nic masterVmNic = _networkModel.getNicInNetwork(masterVmId, containerCluster.getNetworkId());
        // handle Nic interface method change between releases 4.5 and 4.6 and above through reflection
        Method m = null;
        try {
            m = Nic.class.getMethod("getIp4Address");
        } catch (NoSuchMethodException e1) {
            try {
                m = Nic.class.getMethod("getIPv4Address");
            } catch (NoSuchMethodException e2) {
                stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
                throw new ManagementServerException("Failed to activate port forwarding rules for the cluster: " + containerCluster.getName());
            }
        }
        Ip masterIp = null;
        try {
            masterIp = new Ip(m.invoke(masterVmNic).toString());
        } catch (InvocationTargetException | IllegalAccessException ie) {
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to activate port forwarding rules for the cluster: " + containerCluster.getName());
        }
        final Ip masterIpFinal = masterIp;
        final long publicIpId = publicIp.getId();
        final long networkId = containerCluster.getNetworkId();
        final long accountId = account.getId();
        final long domainId = account.getDomainId();
        final long masterVmIdFinal = masterVmId;

        try {
            PortForwardingRuleVO pfRule = Transaction.execute(new TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>() {
                @Override
                public PortForwardingRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                    PortForwardingRuleVO newRule =
                            new PortForwardingRuleVO(null, publicIpId,
                                    443, 443,
                                    masterIpFinal,
                                    443, 443,
                                    "tcp", networkId, accountId, domainId, masterVmIdFinal);
                    newRule.setDisplay(true);
                    newRule.setState(FirewallRule.State.Add);
                    newRule = _portForwardingDao.persist(newRule);
                    return newRule;
                }
            });
            _rulesService.applyPortForwardingRules(publicIp.getId(), account);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Provisioning port forwarding rule from port 443 on " + publicIp.getAddress() +
                        " to the master VM IP :" + masterIpFinal + " in container cluster " + containerCluster.getName());
            }
        } catch (RuntimeException rte) {
            s_logger.warn("Failed to activate port forwarding rules for the container cluster " + containerCluster.getName() + " due to "  + rte);
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to activate port forwarding rules for the cluster: " + containerCluster.getName(), rte);
        } catch (Exception e) {
            s_logger.warn("Failed to activate port forwarding rules for the container cluster " + containerCluster.getName() + " due to "  + e);
            stateTransitTo(containerClusterId, ApplicationCluster.Event.CreateFailed);
            throw new ManagementServerException("Failed to activate port forwarding rules for the cluster: " + containerCluster.getName(), e);
        }
    }

    public boolean validateNetwork(Network network) {
        NetworkOffering nwkoff = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (nwkoff.isSystemOnly()){
            throw new InvalidParameterValueException("This network is for system use only, network id " + network.getId());
        }
        if (! _networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)){
            throw new InvalidParameterValueException("This network does not support userdata that is required for k8s, network id " + network.getId());
        }
        if (! _networkModel.areServicesSupportedInNetwork(network.getId(), Service.Firewall)){
            throw new InvalidParameterValueException("This network does not support firewall that is required for k8s, network id " + network.getId());
        }
        if (! _networkModel.areServicesSupportedInNetwork(network.getId(), Service.PortForwarding)){
            throw new InvalidParameterValueException("This network does not support port forwarding that is required for k8s, network id " + network.getId());
        }
        if (! _networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)){
            throw new InvalidParameterValueException("This network does not support dhcp that is required for k8s, network id " + network.getId());
        }

        List<? extends IpAddress> addrs = _networkModel.listPublicIpsAssignedToGuestNtwk(network.getId(), true);
        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            throw new InvalidParameterValueException("The network id:" + network.getId() + " does not have source NAT ip assoicated with it. " +
                    "To provision a Container Cluster, a isolated network with source NAT is required." );
        } else {
            for (IpAddress addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = _publicIpAddressDao.findById(addr.getId());
                }
            }
            if (sourceNatIp == null) {
                throw new InvalidParameterValueException("The network id:" + network.getId() + " does not have source NAT ip assoicated with it. " +
                        "To provision a Container Cluster, a isolated network with source NAT is required." );
            }
        }
        List<FirewallRuleVO> rules= _firewallDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO rule : rules) {
            Integer startPort = rule.getSourcePortStart();
            Integer endPort = rule.getSourcePortEnd();
            s_logger.debug("Network rule : " + startPort + " " + endPort);
            if (startPort <= 443 && 443 <= endPort) {
                throw new InvalidParameterValueException("The network id:" + network.getId() + " has conflicting firewall rules to provision" +
                        " container cluster." );
            }
        }

        rules= _firewallDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.PortForwarding);
        for (FirewallRuleVO rule : rules) {
            Integer startPort = rule.getSourcePortStart();
            Integer endPort = rule.getSourcePortEnd();
            s_logger.debug("Network rule : " + startPort + " " + endPort);
            if (startPort <= 443 && 443 <= endPort) {
                throw new InvalidParameterValueException("The network id:" + network.getId() + " has conflicting port forwarding rules to provision" +
                        " container cluster." );
            }
        }
        return true;
    }

    public boolean validateServiceOffering(ServiceOffering offering) {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final int ram_requested = offering.getRamSize();
        if (offering.isDynamic()){
            throw new InvalidParameterValueException("This service offering is not suitable for k8s cluster as this is dynamic, service offering id is " + offering.getId());
        }
        if (ram_requested < 64){
            throw new InvalidParameterValueException("This service offering is not suitable for k8s cluster as this has less than 256M of Ram, service offering id is " +  offering.getId());
        }
        if( cpu_requested < 200) {
            throw new InvalidParameterValueException("This service offering is not suitable for k8s cluster as this has less than 600MHz of CPU, service offering id is " +  offering.getId());
        }
        return true;
    }

    private void validateDockerRegistryParams(final String dockerRegistryUserName,
                                                 final String dockerRegistryPassword,
                                                 final String dockerRegistryUrl,
                                                 final String dockerRegistryEmail) {
        // if no params related to docker registry specified then nothing to validate so return true
        if ((dockerRegistryUserName == null || dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword == null || dockerRegistryPassword.isEmpty())  &&
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
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(dockerRegistryEmail);
        if (!matcher.find()) {
            throw new InvalidParameterValueException("Invalid docker registry email specified");
        }
    }

    public DeployDestination plan(final long clusterSize, final long dcId, final ServiceOffering offering) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        List<HostVO> hosts = _resourceMgr.listAllHostsInOneZoneByType(Type.Routing, dcId);
        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
           hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found=false;
        for (int i=1; i <= clusterSize+1; i++) {
            suitable_host_found=false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO h = hp.first();
                int reserved = hp.second();
                reserved++;
                ClusterVO cluster = _clusterDao.findById(h.getClusterId());
                ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());
                if (s_logger.isDebugEnabled()){
                    s_logger.debug("Checking host " + h.getId() + " for capacity already reserved " + reserved);
                }
                if (_capacityMgr.checkIfHostHasCapacity(h.getId(), cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    if (s_logger.isDebugEnabled()){
                        s_logger.debug("Found host " + h.getId() + " has enough capacity cpu = " + cpu_requested * reserved + " ram =" + ram_requested * reserved);
                    }
                    hostEntry.setValue(new Pair<HostVO, Integer>(h, reserved));
                    suitable_host_found = true;
                    break;
                }
            }
            if (suitable_host_found){
                continue;
            }
            else {
                 if (s_logger.isDebugEnabled()){
                     s_logger.debug("Suitable hosts not found in datacenter " + dcId + " for node " + i);
                 }
                break;
            }
        }
        if (suitable_host_found){
            if (s_logger.isDebugEnabled()){
                s_logger.debug("Suitable hosts found in datacenter " + dcId + " creating deployment destination");
            }
            return new DeployDestination(_dcDao.findById(dcId), null, null, null);
        }
        String msg = String.format("Cannot find enough capacity for container_cluster(requested cpu=%1$s memory=%2$s)",
                cpu_requested*clusterSize, ram_requested*clusterSize);
        s_logger.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, dcId);
    }

    public DeployDestination plan(final long containerClusterId, final long dcId) throws InsufficientServerCapacityException {
        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);
        ServiceOffering offering = _srvOfferingDao.findById(containerCluster.getServiceOfferingId());

        if (s_logger.isDebugEnabled()){
            s_logger.debug("Checking deployment destination for containerClusterId= " + containerClusterId + " in dcId=" + dcId);
        }

        return plan(containerCluster.getNodeCount() + 1, dcId, offering);
    }

    @Override
    public boolean stopContainerCluster(long containerClusterId) throws ManagementServerException {

        final ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);
        if (containerCluster == null) {
            throw new ManagementServerException("Failed to find container cluster id: " + containerClusterId);
        }

        if (containerCluster.getRemoved() != null) {
            throw new ManagementServerException("Container cluster id:" + containerClusterId + " is already deleted.");
        }

        if (containerCluster.getState().equals(ApplicationCluster.State.Stopped) ){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Container cluster id: " + containerClusterId + " is already stopped.");
            }
            return true;
        }

        if (containerCluster.getState().equals(ApplicationCluster.State.Stopping) ){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Container cluster id: " + containerClusterId + " is getting stopped.");
            }
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping container cluster: " + containerCluster.getName());
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.StopRequested);

        for (final ApplicationClusterVmMapVO vmMapVO : _clusterVmMapDao.listByClusterId(containerClusterId)) {
            final UserVmVO vm = _userVmDao.findById(vmMapVO.getVmId());
            try {
                if (vm == null) {
                    stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
                    throw new ManagementServerException("Failed to start all VMs in container cluster id: " + containerClusterId);
                }
                stopClusterVM(vmMapVO);
            } catch (ServerApiException ex) {
                s_logger.warn("Failed to stop VM in container cluster id:" + containerClusterId + " due to " + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }

        for (final ApplicationClusterVmMapVO vmMapVO : _clusterVmMapDao.listByClusterId(containerClusterId)) {
            final UserVmVO vm = _userVmDao.findById(vmMapVO.getVmId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Stopped)) {
                stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
                throw new ManagementServerException("Failed to stop all VMs in container cluster id: " + containerClusterId);
            }
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationSucceeded);
        return true;
    }

    private boolean isAddOnServiceRunning(Long clusterId, String svcName) {

        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(clusterId);

        //FIXME: whole logic needs revamp. Assumption that management server has public network access is not practical
        IPAddressVO publicIp = null;
        List<IPAddressVO> ips = _publicIpAddressDao.listByAssociatedNetwork(containerCluster.getNetworkId(), true);
        publicIp = ips.get(0);

        Runtime r = Runtime.getRuntime();
        int nodePort = 0;
        try {
            ApplicationClusterDetailsVO clusterDetails = _applicationClusterDetailsDao.findByClusterId(containerCluster.getId());
            String execStr = "kubectl -s https://" + publicIp.getAddress().addr() + "/ --username=admin "
                    + " --password=" + clusterDetails.getPassword()
                    + " get pods --insecure-skip-tls-verify=true --namespace=kube-system";
            Process p = r.exec(execStr);
            p.waitFor();
            BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF8"));
            String line = "";
            while ((line = b.readLine()) != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("KUBECTL : " + line);
                }
                if (line.contains(svcName) && line.contains("Running")) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Service :" + svcName + " for the container cluster "
                                + containerCluster.getName() + " is running");
                    }
                    b.close();
                    return true;
                }
            }
            b.close();
        } catch (IOException excep) {
            s_logger.warn("KUBECTL: " + excep);
        } catch (InterruptedException e) {
            s_logger.warn("KUBECTL: " + e);
        }
        return false;
    }

    @Override
    public boolean deleteContainerCluster(Long containerClusterId) throws ManagementServerException {

        ApplicationClusterVO cluster = _applicationClusterDao.findById(containerClusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Invalid cluster id specified");
        }

        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, cluster);

        return cleanupContainerClusterResources(containerClusterId);
    }

    private boolean cleanupContainerClusterResources(Long containerClusterId) throws ManagementServerException {

        ApplicationClusterVO cluster = _applicationClusterDao.findById(containerClusterId);

        if (!(cluster.getState().equals(ApplicationCluster.State.Running)
                || cluster.getState().equals(ApplicationCluster.State.Stopped)
                || cluster.getState().equals(ApplicationCluster.State.Alert)
                || cluster.getState().equals(ApplicationCluster.State.Error)
                || cluster.getState().equals(ApplicationCluster.State.Destroying))) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Cannot perform delete operation on cluster:" + cluster.getName() + " in state " + cluster.getState() );
            }
            throw new PermissionDeniedException("Cannot perform delete operation on cluster: " + cluster.getName() + " in state" + cluster.getState() );
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.DestroyRequested);

        boolean failedVmDestroy = false;
        List<ApplicationClusterVmMapVO> clusterVMs = _applicationClusterVmMapDao.listByClusterId(cluster.getId());
        if ( (clusterVMs != null) && !clusterVMs.isEmpty()) {
            for (ApplicationClusterVmMapVO clusterVM: clusterVMs) {
                long vmID = clusterVM.getVmId();

                // delete only if VM exists and is not removed
                UserVmVO userVM = _vmDao.findById(vmID);
                if (userVM== null || userVM.isRemoved()) {
                    continue;
                }

                try {
                    _userVmService.destroyVm(vmID,true);
                    _applicationClusterVmMapDao.expunge(clusterVM.getId());
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Destroyed VM: " + userVM.getInstanceName() + " as part of cluster: " + cluster.getName() + " destroy.");
                    }
                } catch (Exception e ) {
                    failedVmDestroy = true;
                    s_logger.warn("Failed to destroy VM :" + userVM.getInstanceName() + " part of the cluster: " + cluster.getName() +
                            " due to " + e);
                    s_logger.warn("Moving on with destroying remaining resources provisioned for the cluster: " + cluster.getName());
                }
            }
        }
        ApplicationClusterDetailsVO clusterDetails = _applicationClusterDetailsDao.findByClusterId(containerClusterId);
        boolean cleanupNetwork = clusterDetails.getNetworkCleanup();

        // if there are VM's that were not expunged, we can not delete the network
        if(!failedVmDestroy) {
            if (cleanupNetwork) {
                NetworkVO network = null;
                try {
                    network = _networkDao.findById(cluster.getNetworkId());
                    if (network != null && network.getRemoved() == null) {
                        Account owner = _accountMgr.getAccount(network.getAccountId());
                        User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
                        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
                        boolean networkDestroyed = _networkMgr.destroyNetwork(cluster.getNetworkId(), context, true);
                        if (!networkDestroyed) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Failed to destroy network: " + cluster.getNetworkId() +
                                        " as part of cluster: " + cluster.getName()+ " destroy");
                            }
                            processFailedNetworkDelete(containerClusterId);
                            throw new ManagementServerException("Failed to delete the network as part of container cluster name:" + cluster.getName() + " clean up");
                        }
                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Destroyed network: " +  network.getName() + " as part of cluster: " + cluster.getName() + " destroy");
                        }
                    }
                } catch (Exception e) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Failed to destroy network: " + cluster.getNetworkId() +
                                " as part of cluster: " + cluster.getName() + "  destroy due to " + e);
                    }
                    processFailedNetworkDelete(containerClusterId);
                    throw new ManagementServerException("Failed to delete the network as part of container cluster name:" + cluster.getName() + " clean up");
                }
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("There are VM's that are not expunged in container cluster " + cluster.getName());
            }
            processFailedNetworkDelete(containerClusterId);
            throw new ManagementServerException("Failed to destroy one or more VM's as part of container cluster name:" + cluster.getName() + " clean up");
        }

        stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationSucceeded);

        cluster = _applicationClusterDao.findById(containerClusterId);
        cluster.setCheckForGc(false);
        _applicationClusterDao.update(cluster.getId(), cluster);

        _applicationClusterDao.remove(cluster.getId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Container cluster name:" + cluster.getName() + " is successfully deleted");
        }

        return true;
    }

    void processFailedNetworkDelete(long containerClusterId) {
        stateTransitTo(containerClusterId, ApplicationCluster.Event.OperationFailed);
        ApplicationClusterVO cluster = _applicationClusterDao.findById(containerClusterId);
        cluster.setCheckForGc(true);
        _applicationClusterDao.update(cluster.getId(), cluster);
    }

    UserVm createK8SMaster(final ApplicationClusterVO containerCluster, final IPAddressVO publicIP) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        UserVm masterVm = null;

        DataCenter zone = _dcDao.findById(containerCluster.getZoneId());
        ServiceOffering serviceOffering = _offeringDao.findById(containerCluster.getServiceOfferingId());
        VirtualMachineTemplate template = _templateDao.findById(containerCluster.getTemplateId());

        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(containerCluster.getNetworkId());

        Account owner = _accountDao.findById(containerCluster.getAccountId());

        Network.IpAddresses addrs = new Network.IpAddresses(null, null);

        Map<String, String> customparameterMap = new HashMap<String, String>();

        String hostName = containerCluster.getName() + "-k8s-master";

        String k8sMasterConfig = null;
        try {
            String masterCloudConfig = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterMasterCloudConfig.key());
            k8sMasterConfig = readFile(masterCloudConfig);

            final String user = "{{ k8s_master.user }}";
            final String password = "{{ k8s_master.password }}";
            final String apiServerCert = "{{ k8s_master.apiserver.crt }}";
            final String apiServerKey = "{{ k8s_master.apiserver.key }}";
            final String caCert = "{{ k8s_master.ca.crt }}";

            final KeystoreVO rootCA = keystoreDao.findByName(CCS_ROOTCA_KEYPAIR);
            final PrivateKey rootCAPrivateKey = pemToRSAPrivateKey(rootCA.getKey());
            final X509Certificate rootCACert = pemToX509Cert(rootCA.getCertificate());
            final KeyPair keyPair = generateRandomKeyPair();
            final String tlsClientCert = x509CertificateToPem(generateClientCertificate(rootCAPrivateKey, rootCACert, keyPair, publicIP.getAddress().addr(), true));
            final String tlsPrivateKey = rsaPrivateKeyToPem(keyPair.getPrivate());

            k8sMasterConfig = k8sMasterConfig.replace(apiServerCert, tlsClientCert.replace("\n", "\n      "));
            k8sMasterConfig = k8sMasterConfig.replace(apiServerKey, tlsPrivateKey.replace("\n", "\n      "));
            k8sMasterConfig = k8sMasterConfig.replace(caCert, rootCA.getCertificate().replace("\n", "\n      "));

            ApplicationClusterDetailsVO clusterDetails = _applicationClusterDetailsDao.findByClusterId(containerCluster.getId());
            k8sMasterConfig = k8sMasterConfig.replace(password, clusterDetails.getPassword());
            k8sMasterConfig = k8sMasterConfig.replace(user, clusterDetails.getUserName());
        } catch (RuntimeException e ) {
            s_logger.error("Failed to read kubernetes master configuration file due to " + e);
            throw new ManagementServerException("Failed to read kubernetes master configuration file", e);
        } catch (Exception e) {
            s_logger.error("Failed to read kubernetes master configuration file due to " + e);
            throw new ManagementServerException("Failed to read kubernetes master configuration file", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sMasterConfig.getBytes(Charset.forName("UTF-8")));

        masterVm = _userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, containerCluster.getDescription(), null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, containerCluster.getKeyPair(),
                null, addrs, null, null, null, customparameterMap, null);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created master VM: " + hostName + " in the container cluster: " + containerCluster.getName());
        }

        return masterVm;
    }


    UserVm createK8SNode(ApplicationClusterVO containerCluster, String masterIp, int nodeInstance) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        UserVm nodeVm = null;

        DataCenter zone = _dcDao.findById(containerCluster.getZoneId());
        ServiceOffering serviceOffering = _offeringDao.findById(containerCluster.getServiceOfferingId());
        VirtualMachineTemplate template = _templateDao.findById(containerCluster.getTemplateId());

        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(containerCluster.getNetworkId());

        Account owner = _accountDao.findById(containerCluster.getAccountId());

        Network.IpAddresses addrs = new Network.IpAddresses(null, null);

        Map<String, String> customparameterMap = new HashMap<String, String>();

        String hostName = containerCluster.getName() + "-k8s-node-" + String.valueOf(nodeInstance);

        String k8sNodeConfig = null;
        try {
            String nodeCloudConfig = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterNodeCloudConfig.key());
            k8sNodeConfig = readFile(nodeCloudConfig).toString();
            String masterIPString = "{{ k8s_master.default_ip }}";
            final String clientCert = "{{ k8s_node.client.crt }}";
            final String clientKey = "{{ k8s_node.client.key }}";
            final String caCert = "{{ k8s_node.ca.crt }}";

            final KeystoreVO rootCA = keystoreDao.findByName(CCS_ROOTCA_KEYPAIR);
            final PrivateKey rootCAPrivateKey = pemToRSAPrivateKey(rootCA.getKey());
            final X509Certificate rootCACert = pemToX509Cert(rootCA.getCertificate());
            final KeyPair keyPair = generateRandomKeyPair();
            final String tlsClientCert = x509CertificateToPem(generateClientCertificate(rootCAPrivateKey, rootCACert, keyPair, "", false));
            final String tlsPrivateKey = rsaPrivateKeyToPem(keyPair.getPrivate());

            k8sNodeConfig = k8sNodeConfig.replace(masterIPString, masterIp);
            k8sNodeConfig = k8sNodeConfig.replace(clientCert, tlsClientCert.replace("\n", "\n      "));
            k8sNodeConfig = k8sNodeConfig.replace(clientKey, tlsPrivateKey.replace("\n", "\n      "));
            k8sNodeConfig = k8sNodeConfig.replace(caCert, rootCA.getCertificate().replace("\n", "\n      "));

            ApplicationClusterDetailsVO clusterDetails = _applicationClusterDetailsDao.findByClusterId(containerCluster.getId());

            /* genarate /.docker/config.json file on the nodes only if container cluster is created to
             * use docker private registry */
            String dockerUserName = clusterDetails.getRegistryUsername();
            String dockerPassword = clusterDetails.getRegistryPassword();
            if (dockerUserName != null && !dockerUserName.isEmpty() && dockerPassword != null && !dockerPassword.isEmpty()) {
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
                String dockerUrl = "{{docker.url}}";
                String dockerAuth = "{{docker.secret}}";
                String dockerEmail = "{{docker.email}}";
                String usernamePassword = dockerUserName + ":" + dockerPassword;
                String base64Auth = Base64.encodeBase64String(usernamePassword.getBytes(Charset.forName("UTF-8")));
                k8sNodeConfig = k8sNodeConfig.replace(dockerUrl, "\"" + clusterDetails.getRegistryUrl() + "\"");
                k8sNodeConfig = k8sNodeConfig.replace(dockerAuth, "\"" + base64Auth + "\"");
                k8sNodeConfig = k8sNodeConfig.replace(dockerEmail, "\"" + clusterDetails.getRegistryEmail() + "\"");
            }
        } catch (RuntimeException e ) {
            s_logger.warn("Failed to read node configuration file due to " + e );
            throw new ManagementServerException("Failed to read cluster node configuration file.", e);
        } catch (Exception e) {
            s_logger.warn("Failed to read node configuration file due to " + e );
            throw new ManagementServerException("Failed to read cluster node configuration file.", e);
        }

        String base64UserData = Base64.encodeBase64String(k8sNodeConfig.getBytes(Charset.forName("UTF-8")));

        nodeVm = _userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner,
                hostName, containerCluster.getDescription(), null, null, null,
                null, BaseCmd.HTTPMethod.POST, base64UserData, containerCluster.getKeyPair(),
                null, addrs, null, null, null, customparameterMap, null);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created cluster node VM: " + hostName + " in the container cluster: " + containerCluster.getName());
        }

        return nodeVm;
    }

    private void startClusterVM(final UserVm vm, final ApplicationClusterVO containerCluster) throws ServerApiException {

        try {
            StartVMCmd startVm = new StartVMCmd();
            startVm = ComponentContext.inject(startVm);
            Field f = startVm.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(startVm, vm.getId());
            _userVmService.startVirtualMachine(startVm);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Started VM in the container cluster: " + containerCluster.getName());
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Failed to start VM in the container cluster name:" + containerCluster.getName() + " due to Exception: " , ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the container cluster name:" + containerCluster.getName(), ex);
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to start VM in the container cluster name:" + containerCluster.getName() + " due to Exception: " , ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the container cluster name:" + containerCluster.getName(), ex);
        } catch (InsufficientCapacityException ex) {
            s_logger.warn("Failed to start VM in the container cluster name:" + containerCluster.getName() + " due to Exception: " , ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the container cluster name:" + containerCluster.getName(), ex);
        } catch (RuntimeException ex) {
            s_logger.warn("Failed to start VM in the container cluster name:" + containerCluster.getName() + " due to Exception: " , ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the container cluster name:" + containerCluster.getName(), ex);
        } catch (Exception ex) {
            s_logger.warn("Failed to start VM in the container cluster name:" + containerCluster.getName() + " due to Exception: " , ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM in the container cluster name:" + containerCluster.getName(), ex);
        }

        UserVm startVm = _vmDao.findById(vm.getId());
        if (!startVm.getState().equals(VirtualMachine.State.Running)) {
            s_logger.warn("Failed to start VM instance.");
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start VM instance in container cluster " + containerCluster.getName());
        }
    }

    private void stopClusterVM(final ApplicationClusterVmMapVO vmMapVO) throws ServerApiException {
        try {
            _userVmService.stopVirtualMachine(vmMapVO.getVmId(), false);
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Failed to stop container cluster VM due to Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public ListResponse<ApplicationClusterResponse>  listApplicationClusters(ListApplicationClusterCmd cmd) {

        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        ListResponse<ApplicationClusterResponse> response = new ListResponse<ApplicationClusterResponse>();

        List<ApplicationClusterResponse> responsesList = new ArrayList<ApplicationClusterResponse>();
        SearchCriteria<ApplicationClusterVO> sc = _applicationClusterDao.createSearchCriteria();

        String state = cmd.getState();
        if (state != null && !state.isEmpty()) {
            if ( !ApplicationCluster.State.Running.toString().equals(state) &&
                    !ApplicationCluster.State.Stopped.toString().equals(state) &&
                    !ApplicationCluster.State.Destroyed.toString().equals(state)) {
                throw new InvalidParameterValueException("Invalid vlaue for cluster state is specified");
            }
        }

        if (cmd.getId() != null) {
            ApplicationClusterVO cluster = _applicationClusterDao.findById(cmd.getId());
            if (cluster == null) {
                throw new InvalidParameterValueException("Invalid cluster id specified");
            }
            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ListEntry, false, cluster);
            responsesList.add(createContainerClusterResponse(cmd.getId()));
        } else {
            Filter searchFilter = new Filter(ApplicationClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

            if (state != null && !state.isEmpty()) {
                sc.addAnd("state", SearchCriteria.Op.EQ, state);
            }

            if (_accountMgr.isNormalUser(caller.getId())) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, caller.getAccountId());
            } else if (_accountMgr.isDomainAdmin(caller.getId())) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, caller.getDomainId());
            }

            String name = cmd.getName();
            if (name != null && !name.isEmpty()) {
                sc.addAnd("name", SearchCriteria.Op.LIKE, name);
            }

            List<ApplicationClusterVO> containerClusters = _applicationClusterDao.search(sc, searchFilter);
            for (ApplicationClusterVO cluster : containerClusters) {
                ApplicationClusterResponse clusterReponse = createContainerClusterResponse(cluster.getId());
                responsesList.add(clusterReponse);
            }
        }
        response.setResponses(responsesList);
        return response;
    }

    public ApplicationClusterResponse createContainerClusterResponse(long containerClusterId) {

        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);
        ApplicationClusterResponse response = new ApplicationClusterResponse();

        response.setId(containerCluster.getUuid());

        response.setName(containerCluster.getName());

        response.setDescription(containerCluster.getDescription());

        DataCenterVO zone = ApiDBUtils.findZoneById(containerCluster.getZoneId());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());

        response.setClusterSize(String.valueOf(containerCluster.getNodeCount()));

        VMTemplateVO template = ApiDBUtils.findTemplateById(containerCluster.getTemplateId());
        response.setTemplateId(template.getUuid());

        ServiceOfferingVO offering = _srvOfferingDao.findById(containerCluster.getServiceOfferingId());
        response.setServiceOfferingId(offering.getUuid());

        response.setServiceOfferingName(offering.getName());

        response.setKeypair(containerCluster.getKeyPair());

        response.setState(containerCluster.getState().toString());

        response.setCores(String.valueOf(containerCluster.getCores()));

        response.setMemory(String.valueOf(containerCluster.getMemory()));

        response.setObjectName("applicationcluster");

        NetworkVO ntwk = _networkDao.findByIdIncludingRemoved(containerCluster.getNetworkId());

        response.setEndpoint(containerCluster.getEndpoint());

        response.setNetworkId(ntwk.getUuid());

        response.setAssociatedNetworkName(ntwk.getName());

        response.setConsoleEndpoint(containerCluster.getConsoleEndpoint());

        List<String> vmIds = new ArrayList<String>();
        List<ApplicationClusterVmMapVO> vmList = _applicationClusterVmMapDao.listByClusterId(containerCluster.getId());
        if (vmList != null && !vmList.isEmpty()) {
            for (ApplicationClusterVmMapVO vmMapVO: vmList) {
                UserVmVO userVM = _userVmDao.findById(vmMapVO.getVmId());
                if (userVM != null) {
                    vmIds.add(userVM.getUuid());
                }
            }
        }

        response.setVirtualMachineIds(vmIds);

        ApplicationClusterDetailsVO clusterDetails = _applicationClusterDetailsDao.findByClusterId(containerCluster.getId());
        if (clusterDetails != null) {
            response.setUsername(clusterDetails.getUserName());
            response.setPassword(clusterDetails.getPassword());
        }

        return response;
    }

    static String readFile(String path) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected boolean stateTransitTo(long containerClusterId, ApplicationCluster.Event e) {
        ApplicationClusterVO containerCluster = _applicationClusterDao.findById(containerClusterId);
        try {
            return _stateMachine.transitTo(containerCluster, e, null, _applicationClusterDao);
        } catch (NoTransitionException nte) {
            s_logger.warn("Failed to transistion state of the container cluster: " + containerCluster.getName()
                    + " in state " + containerCluster.getState().toString() + " on event " + e.toString());
            return false;
        }
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private boolean isApplicationClusterServiceConfigured(DataCenter zone) {

        String templateName = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterTemplateName.key());
        if (templateName == null || templateName.isEmpty()) {
            s_logger.warn("Global setting " + ApplicationClusterConfig.ApplicationClusterTemplateName.key() + " is empty." +
                    "Template name need to be specified, for container service to function.");
            return false;
        }

        final VMTemplateVO template = _templateDao.findByTemplateName(templateName);
        if (template == null) {
           s_logger.warn("Unable to find the template:" + templateName  + " to be used for provisioning cluster");
            return false;
        }

        String masterCloudConfig = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterMasterCloudConfig.key());
        if (masterCloudConfig == null || masterCloudConfig.isEmpty()) {
            s_logger.warn("global setting " + ApplicationClusterConfig.ApplicationClusterMasterCloudConfig.key() + " is empty." +
                    "Admin has not specified the cloud config template to be used for provisioning master VM");
            return false;
        }

        String nodeCloudConfig = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterNodeCloudConfig.key());
        if (nodeCloudConfig == null || nodeCloudConfig.isEmpty()) {
            s_logger.warn("global setting " + ApplicationClusterConfig.ApplicationClusterNodeCloudConfig.key() + " is empty." +
                    "Admin has not specified the cloud config template to be used for provisioning node VM's");
            return false;
        }


        String networkOfferingName = _globalConfigDao.getValue(ApplicationClusterConfig.ApplicationClusterNetworkOffering.key());
        if (networkOfferingName == null || networkOfferingName.isEmpty()) {
            s_logger.warn("global setting " + ApplicationClusterConfig.ApplicationClusterNetworkOffering.key()  + " is empty. " +
                    "Admin has not yet specified the network offering to be used for provisioning isolated network for the cluster.");
            return false;
        }

        NetworkOfferingVO networkOffering = _networkOfferingDao.findByUniqueName(networkOfferingName);
        if (networkOffering == null) {
            s_logger.warn("Network offering with name :" + networkOfferingName + " specified by admin is not found.");
            return false;
        }

        if (networkOffering.getState() == NetworkOffering.State.Disabled) {
            s_logger.warn("Network offering :" + networkOfferingName + "is not enabled.");
            return false;
        }

        List<String> services = _ntwkOfferingServiceMapDao.listServicesForNetworkOffering(networkOffering.getId());
        if (services == null || services.isEmpty() || !services.contains("SourceNat")) {
            s_logger.warn("Network offering :" + networkOfferingName + " does not have necessary services to provision container cluster");
            return false;
        }

        if (networkOffering.getEgressDefaultPolicy() == false) {
            s_logger.warn("Network offering :" + networkOfferingName + "has egress default policy turned off should be on to provision container cluster.");
            return false;
        }

        long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
        PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            s_logger.warn("Unable to find physical network with id: " + physicalNetworkId + " and tag: " + networkOffering.getTags());
            return false;
        }

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateApplicationClusterCmd.class);
        cmdList.add(StartApplicationClusterCmd.class);
        cmdList.add(StopApplicationClusterCmd.class);
        cmdList.add(DeleteApplicationClusterCmd.class);
        cmdList.add(ListApplicationClusterCmd.class);
        cmdList.add(ListApplicationClusterCACertCmd.class);
        return cmdList;
    }

    // Garbage collector periodically run through the container clusters marked for GC. For each container cluster
    // marked for GC, attempt is made to destroy cluster.
    public class ApplicationClusterGarbageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("ApplicationCluster.GC.Lock");
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
                List<ApplicationClusterVO> clusters = _applicationClusterDao.findClustersToGarbageCollect();
                for (ApplicationCluster cluster :clusters ) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Running application cluster garbage collector on container cluster name:" + cluster.getName());
                    }
                    try {
                        if (cleanupContainerClusterResources(cluster.getId())) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Application cluster: " + cluster.getName() + " is successfully garbage collected");
                            }
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Application cluster: " + cluster.getName() + " failed to get" +
                                        " garbage collected. Will be attempted to garbage collected in next run");
                            }
                        }
                    } catch (RuntimeException e) {
                        s_logger.debug("Faied to destroy application cluster name:" + cluster.getName() + " during GC due to " + e);
                        // proceed furhter with rest of the container cluster garbage collection
                    } catch (Exception e) {
                        s_logger.debug("Faied to destroy application cluster name:" + cluster.getName() + " during GC due to " + e);
                        // proceed furhter with rest of the container cluster garbage collection
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught exception while running application cluster gc: ", e);
            }
        }
    }

    /**
     * The ApplicationClusterStatusScanner checks if the application cluster is in the desired state. If it detects this application cluster
     * is not in the desired state, it will trigger an event and marks the application cluster to be 'Alert' state. For e.g an
     * application cluster in 'Running' state this means all the cluster's VM's of type Node should be running and
     * the number of node VM's should be the same as the cluster size, and the master node VM is running. It is possible due, to
     * out of band changes by the user or the hosts going down, we may end up one or more VM's in stopped state. in which case
     * the scanner detects these changes and marks the cluster in 'Alert' state. Similarly a cluster in 'Stopped' state means
     * all the cluster VM's are in stopped state and any mismatch in states should get picked up by the scanner and it will then
     * mark the application cluster to be in the 'Alert' state. Through recovery or reconciliation, clusters in 'Alert' will
     * be brought back to a known good or desired state.
     */
    public class ApplicationClusterStatusScanner extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("ApplicationCluster.State.Scanner.Lock");
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

                // run through the list of application clusters in 'Running' state and ensure all the VM's are Running in the cluster
                List<ApplicationClusterVO> runningClusters = _applicationClusterDao.findClustersInState(ApplicationCluster.State.Running);
                for (ApplicationCluster cluster : runningClusters ) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Running application cluster state scanner on cluster:" + cluster.getName() + " for state " + ApplicationCluster.State.Starting);
                    }
                    try {
                        if (!isClusterInDesiredState(cluster, VirtualMachine.State.Running)) {
                            stateTransitTo(cluster.getId(), ApplicationCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        s_logger.warn("Failed to run through VM states of application cluster due to " + e);
                    }
                }

                // run through container clusters in 'Stopped' state and ensure all the VM's are Stopped in the cluster
                List<ApplicationClusterVO> stoppedClusters = _applicationClusterDao.findClustersInState(ApplicationCluster.State.Stopped);
                for (ApplicationCluster cluster : stoppedClusters ) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Running application cluster state scanner on cluster:" + cluster.getName() + " for state " + ApplicationCluster.State.Stopped);
                    }
                    try {
                        if (!isClusterInDesiredState(cluster, VirtualMachine.State.Stopped)) {
                            stateTransitTo(cluster.getId(), ApplicationCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        s_logger.warn("Failed to run through VM states of container cluster due to " + e);
                    }
                }

                // run through container clusters in 'Alert' state and reconcile state as 'Running' if the VM's are running
                List<ApplicationClusterVO> clustersInAlertState = _applicationClusterDao.findClustersInState(ApplicationCluster.State.Alert);
                for (ApplicationCluster cluster : clustersInAlertState ) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Running application cluster state scanner on cluster:" + cluster.getName() + " for state " + ApplicationCluster.State.Alert);
                    }
                    try {
                        if (isClusterInDesiredState(cluster, VirtualMachine.State.Running)) {
                            // mark the cluster to be running
                            stateTransitTo(cluster.getId(), ApplicationCluster.Event.RecoveryRequested);
                            stateTransitTo(cluster.getId(), ApplicationCluster.Event.OperationSucceeded);
                        }
                    } catch (Exception e) {
                        s_logger.warn("Failed to run through VM states of application cluster status scanner due to " + e);
                    }
                }

            } catch (RuntimeException e) {
                s_logger.warn("Caught exception while running application cluster state scanner.", e);
            } catch (Exception e) {
                s_logger.warn("Caught exception while running application cluster state scanner.", e);
            }
        }
    }

    // checks if container cluster is in desired state
    boolean isClusterInDesiredState(ApplicationCluster applicationCluster, VirtualMachine.State state) {
        List<ApplicationClusterVmMapVO> clusterVMs = _applicationClusterVmMapDao.listByClusterId(applicationCluster.getId());

        // check if all the VM's are in same state
        for (ApplicationClusterVmMapVO clusterVm : clusterVMs) {
            VMInstanceVO vm = _vmInstanceDao.findByIdIncludingRemoved(clusterVm.getVmId());
            if (vm.getState() != state) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found VM in the application cluster: " + applicationCluster.getName() +
                            " in state: " + vm.getState().toString() + " while expected to be in state: " + state.toString() +
                            " So moving the cluster to Alert state for reconciliation.");
                }
                return false;
            }
        }

        // check cluster is running at desired capacity include master node as well, so count should be cluster size + 1
        // TODO size + 1 is very topology specific and needs adressing This should be an accumulation of nodetype.count for each nodetype
        if (clusterVMs.size() != (applicationCluster.getNodeCount() + 1)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found only " + clusterVMs.size() + " VM's in the container cluster: " + applicationCluster.getName() +
                        " in state: " + state.toString() + " While expected number of VM's to " +
                        " be in state: " + state.toString() + " is " + (applicationCluster.getNodeCount() + 1) +
                        " So moving the cluster to Alert state for reconciliation.");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean start() {
        _gcExecutor.scheduleWithFixedDelay(new ApplicationClusterGarbageCollector(), 300, 300, TimeUnit.SECONDS);
        _stateScanner.scheduleWithFixedDelay(new ApplicationClusterStatusScanner(), 300, 30, TimeUnit.SECONDS);

        // run the data base migration.
        Properties dbProps = DbProperties.getDbProperties();
        final String cloudUsername = dbProps.getProperty("db.cloud.username");
        final String cloudPassword = dbProps.getProperty("db.cloud.password");
        final String cloudHost = dbProps.getProperty("db.cloud.host");
        final int cloudPort = Integer.parseInt(dbProps.getProperty("db.cloud.port"));
        final String dbUrl = "jdbc:mysql://" + cloudHost + ":" + cloudPort + "/cloud";

        try {
            Flyway flyway = new Flyway();
            flyway.setDataSource(dbUrl, cloudUsername, cloudPassword);

            // name the meta table as sb_ccs_schema_version
            flyway.setTable("application_cluster_service_version");

            // make the existing cloud DB schema and data as baseline
            flyway.setBaselineOnMigrate(true);
            flyway.setBaselineVersionAsString("0");

            // apply CCS schema
            flyway.migrate();
        } catch (FlywayException fwe) {
            s_logger.error("Failed to run migration on Cloudstack Application Cluster Service database due to " + fwe);
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _gcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Application-Cluster-Scavenger"));
        _stateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Application-Cluster-State-Scanner"));

        final KeystoreVO keyStoreVO = keystoreDao.findByName(CCS_ROOTCA_KEYPAIR);
        if (keyStoreVO == null) {
                try {
                    final KeyPair keyPair = generateRandomKeyPair();
                    final String rootCACert = x509CertificateToPem(generateRootCACertificate(keyPair));
                    final String rootCAKey = rsaPrivateKeyToPem(keyPair.getPrivate());
                    keystoreDao.save(CCS_ROOTCA_KEYPAIR, rootCACert, rootCAKey, "");
                    s_logger.info("No Container Cluster CA stores found, created and saved a keypair with certificate: \n" + rootCACert);
                } catch (NoSuchProviderException | NoSuchAlgorithmException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException e) {
                    s_logger.error("Unable to create and save CCS rootCA keypair: " + e.toString());
                }
        }
        return true;
    }

    /**
     * @deprecated  this should move to {@link CertService}
     * @param keyPair
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws CertificateEncodingException
     * @throws SignatureException
     * @throws InvalidKeyException
     */
    @Deprecated
    public X509Certificate generateRootCACertificate(KeyPair keyPair) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, SignatureException, InvalidKeyException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final X500Principal dnName = new X500Principal(CCS_ROOTCA_CN);
        final X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(now.minusDays(1).toDate());
        certGen.setNotAfter(now.plusYears(50).toDate());
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        return certGen.generate(keyPair.getPrivate(), "BC");
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public X509Certificate generateClientCertificate(final PrivateKey rootCAPrivateKey, final X509Certificate rootCACert,
                                                     final KeyPair keyPair, final String publicIPAddress, final boolean isMasterNode) throws IOException, CertificateParsingException, InvalidKeyException, NoSuchAlgorithmException, CertificateEncodingException, NoSuchProviderException, SignatureException, InvalidKeySpecException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();;
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal(CCS_ROOTCA_CN));
        certGen.setSubjectDN(new X500Principal(CCS_CLUSTER_CN));
        certGen.setNotBefore(now.minusDays(1).toDate());
        certGen.setNotAfter(now.plusYears(10).toDate());
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(rootCACert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifier(keyPair.getPublic().getEncoded()));

        if (isMasterNode) {
            final List<ASN1Encodable> subjectAlternativeNames = new ArrayList<ASN1Encodable>();
            subjectAlternativeNames.add(new GeneralName(GeneralName.iPAddress, publicIPAddress));
            subjectAlternativeNames.add(new GeneralName(GeneralName.iPAddress, "10.0.0.1"));
            subjectAlternativeNames.add(new GeneralName(GeneralName.iPAddress, "10.1.1.1"));
            subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, "kubernetes"));
            subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, "kubernetes.default"));
            subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, "kubernetes.default.svc"));
            subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, "kubernetes.default.svc.cluster.local"));

            final DERSequence subjectAlternativeNamesExtension = new DERSequence(
                    subjectAlternativeNames.toArray(new ASN1Encodable[subjectAlternativeNames.size()]));
            certGen.addExtension(X509Extensions.SubjectAlternativeName, false,
                    subjectAlternativeNamesExtension);
        }

        return certGen.generate(rootCAPrivateKey, "BC");
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public KeyPair generateRandomKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public KeyFactory getKeyFactory() {
        KeyFactory keyFactory = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            keyFactory = KeyFactory.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return keyFactory;
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public X509Certificate pemToX509Cert(final String pem) throws IOException {
        return (X509Certificate) certService.parseCertificate(pem);
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public String x509CertificateToPem(final X509Certificate cert) throws IOException, CertificateEncodingException {

        // TODO convert cert to PemObject
        try (final StringWriter sw = new StringWriter();
                final PemWriter pw = new PemWriter(sw)) {
            final PemObject pemObject = new PemObject(cert.getType(), cert.getEncoded());
            pw.writeObject(pemObject);
            return sw.toString();
        }
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public PrivateKey pemToRSAPrivateKey(final String pem) throws InvalidKeySpecException, IOException {
        final PemReader pr = new PemReader(new StringReader(pem));
        final PemObject pemObject = pr.readPemObject();
        final KeyFactory keyFactory = getKeyFactory();
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pemObject.getContent()));
    }

    /**
     * @deprecated this should move to {@link CertService}
     */
    @Deprecated
    public String rsaPrivateKeyToPem(final PrivateKey key) throws IOException {
        final PemObject pemObject = new PemObject(CCS_RSA_PRIVATE_KEY, key.getEncoded());
        final StringWriter sw = new StringWriter();
        try (final PemWriter pw = new PemWriter(sw)) {
            pw.writeObject(pemObject);
        }
        return sw.toString();
    }
}
