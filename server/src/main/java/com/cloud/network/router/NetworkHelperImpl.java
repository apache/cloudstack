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
package com.cloud.network.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.utils.validation.ChecksumUtil;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.router.deployment.RouterDeploymentDefinition;
import org.apache.cloudstack.utils.CloudStackVersion;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.RouterHealthCheckResultDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public class NetworkHelperImpl implements NetworkHelper {

    private static final Logger s_logger = Logger.getLogger(NetworkHelperImpl.class);

    protected static Account s_systemAccount;
    protected static String s_vmInstanceName;

    @Inject
    protected NicDao _nicDao;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected DomainRouterDao _routerDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    protected IPAddressDao _ipAddressDao;
    @Inject
    private UserIpv6AddressDao _ipv6Dao;
    @Inject
    protected NetworkOrchestrationService _networkMgr;
    @Inject
    private UserDao _userDao;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected VirtualMachineManager _itMgr;
    @Inject
    protected IpAddressManager _ipAddrMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VpcVirtualNetworkApplianceManager _vpcRouterMgr;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    RouterHealthCheckResultDao _routerHealthCheckResultDao;
    @Inject
    Ipv6Service ipv6Service;

    protected final Map<HypervisorType, ConfigKey<String>> hypervisorsMap = new HashMap<>();

    @PostConstruct
    protected void setupHypervisorsMap() {
        hypervisorsMap.put(HypervisorType.XenServer, VirtualNetworkApplianceManager.RouterTemplateXen);
        hypervisorsMap.put(HypervisorType.KVM, VirtualNetworkApplianceManager.RouterTemplateKvm);
        hypervisorsMap.put(HypervisorType.VMware, VirtualNetworkApplianceManager.RouterTemplateVmware);
        hypervisorsMap.put(HypervisorType.Hyperv, VirtualNetworkApplianceManager.RouterTemplateHyperV);
        hypervisorsMap.put(HypervisorType.LXC, VirtualNetworkApplianceManager.RouterTemplateLxc);
        hypervisorsMap.put(HypervisorType.Ovm3, VirtualNetworkApplianceManager.RouterTemplateOvm3);
    }

    @Override
    public boolean sendCommandsToRouter(final VirtualRouter router, final Commands cmds) throws AgentUnavailableException, ResourceUnavailableException {
        if (!checkRouterVersion(router)) {
            s_logger.debug("Router requires upgrade. Unable to send command to router:" + router.getId() + ", router template version : " + router.getTemplateVersion()
                    + ", minimal required version : " + NetworkOrchestrationService.MinVRVersion.valueIn(router.getDataCenterId()));
            throw new ResourceUnavailableException("Unable to send command. Router requires upgrade", VirtualRouter.class, router.getId());
        }
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }

        if (answers == null || answers.length != cmds.size()) {
            return false;
        }

        // FIXME: Have to return state for individual command in the future
        boolean result = true;
        for (final Answer answer : answers) {
            if (!answer.getResult()) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public void handleSingleWorkingRedundantRouter(final List<? extends VirtualRouter> connectedRouters, final List<? extends VirtualRouter> disconnectedRouters,
            final String reason) throws ResourceUnavailableException {
        if (connectedRouters.isEmpty() || disconnectedRouters.isEmpty()) {
            return;
        }

        for (final VirtualRouter virtualRouter : connectedRouters) {
            if (!virtualRouter.getIsRedundantRouter()) {
                throw new ResourceUnavailableException("Who is calling this with non-redundant router or non-domain router?", DataCenter.class, virtualRouter.getDataCenterId());
            }
        }

        for (final VirtualRouter virtualRouter : disconnectedRouters) {
            if (!virtualRouter.getIsRedundantRouter()) {
                throw new ResourceUnavailableException("Who is calling this with non-redundant router or non-domain router?", DataCenter.class, virtualRouter.getDataCenterId());
            }
        }

        final DomainRouterVO connectedRouter = (DomainRouterVO) connectedRouters.get(0);
        DomainRouterVO disconnectedRouter = (DomainRouterVO) disconnectedRouters.get(0);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("About to stop the router " + disconnectedRouter.getInstanceName() + " due to: " + reason);
        }
        final String title = "Virtual router " + disconnectedRouter.getInstanceName() + " would be stopped after connecting back, due to " + reason;
        final String context = "Virtual router (name: " + disconnectedRouter.getInstanceName() + ", id: " + disconnectedRouter.getId()
                + ") would be stopped after connecting back, due to: " + reason;
        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, disconnectedRouter.getDataCenterId(), disconnectedRouter.getPodIdToDeployIn(), title, context);
        disconnectedRouter.setStopPending(true);
        disconnectedRouter = _routerDao.persist(disconnectedRouter);
    }

    @Override
    public NicTO getNicTO(final VirtualRouter router, final Long networkId, final String broadcastUri) {
        final NicProfile nicProfile = _networkModel.getNicProfile(router, networkId, broadcastUri);

        return _itMgr.toNicTO(nicProfile, router.getHypervisorType());
    }

    @Override
    public VirtualRouter destroyRouter(final long routerId, final Account caller, final Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy router " + routerId);
        }

        final DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            return null;
        }

        _accountMgr.checkAccess(caller, null, true, router);

        _itMgr.expunge(router.getUuid());
        _routerHealthCheckResultDao.expungeHealthChecks(router.getId());
        _routerDao.remove(router.getId());
        return router;
    }

    @Override
    public boolean checkRouterVersion(final VirtualRouter router) {
        if (!VirtualNetworkApplianceManager.RouterVersionCheckEnabled.value()) {
            // Router version check is disabled.
            return true;
        }
        if (router.getTemplateVersion() == null) {
            return false;
        }
        final long dcid = router.getDataCenterId();
        String routerVersion = CloudStackVersion.trimRouterVersion(router.getTemplateVersion());
        String routerChecksum = router.getScriptsVersion() == null ? "" : router.getScriptsVersion();
        boolean routerVersionMatch = CloudStackVersion.compare(routerVersion, NetworkOrchestrationService.MinVRVersion.valueIn(dcid)) >= 0;
        if (routerVersionMatch) {
            return true;
        }
        if (HypervisorType.Simulator.equals(router.getHypervisorType())) {
            return true;
        }
        String currentCheckSum = ChecksumUtil.calculateCurrentChecksum(router.getName(), "vms/cloud-scripts.tgz");
        boolean routerCheckSumMatch = currentCheckSum.equals(routerChecksum);
        return routerCheckSumMatch;
    }

    @Override
    public boolean checkRouterTemplateVersion(final VirtualRouter router) {
        if (!VirtualNetworkApplianceManager.RouterVersionCheckEnabled.value()) {
            // Router version check is disabled.
            return true;
        }
        if (router.getTemplateVersion() == null) {
            return false;
        }
        final long dcid = router.getDataCenterId();
        String routerVersion = CloudStackVersion.trimRouterVersion(router.getTemplateVersion());
        return CloudStackVersion.compare(routerVersion, NetworkOrchestrationService.MinVRVersion.valueIn(dcid)) >= 0;
    }

    protected DomainRouterVO start(DomainRouterVO router, final User user, final Account caller, final Map<Param, Object> params, final DeploymentPlan planToDeploy)
            throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting router " + router);
        try {
            _itMgr.advanceStart(router.getUuid(), params, planToDeploy, null);
        } catch (final OperationTimedoutException e) {
            throw new ResourceUnavailableException("Starting router " + router + " failed! " + e.toString(), DataCenter.class, router.getDataCenterId());
        }
        if (router.isStopPending()) {
            s_logger.info("Clear the stop pending flag of router " + router.getHostName() + " after start router successfully!");
            router.setStopPending(false);
            router = _routerDao.persist(router);
        }
        // We don't want the failure of VPN Connection affect the status of
        // router, so we try to make connection
        // only after router start successfully
        final Long vpcId = router.getVpcId();
        if (vpcId != null) {
            _vpcRouterMgr.startSite2SiteVpn(_routerDao.findById(router.getId()));
        }
        return _routerDao.findById(router.getId());
    }

    protected DomainRouterVO waitRouter(final DomainRouterVO router) {
        DomainRouterVO vm = _routerDao.findById(router.getId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Router " + router.getInstanceName() + " is not fully up yet, we will wait");
        }
        while (vm.getState() == State.Starting) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
            }

            // reload to get the latest state info
            vm = _routerDao.findById(router.getId());
        }

        if (vm.getState() == State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Router " + router.getInstanceName() + " is now fully up");
            }

            return router;
        }

        s_logger.warn("Router " + router.getInstanceName() + " failed to start. current state: " + vm.getState());
        return null;
    }

    @Override
    public List<DomainRouterVO> startRouters(final RouterDeploymentDefinition routerDeploymentDefinition) throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {

        final List<DomainRouterVO> runningRouters = new ArrayList<DomainRouterVO>();

        for (DomainRouterVO router : routerDeploymentDefinition.getRouters()) {
            boolean skip = false;
            final State state = router.getState();
            if (router.getHostId() != null && state != State.Running) {
                final HostVO host = _hostDao.findById(router.getHostId());
                if (host == null || host.getState() != Status.Up) {
                    skip = true;
                }
            }
            if (!skip) {
                if (state != State.Running) {
                    router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), routerDeploymentDefinition.getParams());
                }
                if (router != null) {
                    runningRouters.add(router);
                }
            }
        }
        return runningRouters;
    }

    @Override
    public DomainRouterVO startVirtualRouter(final DomainRouterVO router, final User user, final Account caller, final Map<Param, Object> params)
            throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        if (router.getRole() != Role.VIRTUAL_ROUTER || !router.getIsRedundantRouter()) {
            return start(router, user, caller, params, null);
        }

        if (router.getState() == State.Running) {
            s_logger.debug("Redundant router " + router.getInstanceName() + " is already running!");
            return router;
        }

        //
        // If another thread has already requested a VR start, there is a
        // transition period for VR to transit from
        // Starting to Running, there exist a race conditioning window here
        // We will wait until VR is up or fail
        if (router.getState() == State.Starting) {
            return waitRouter(router);
        }

        final DataCenterDeployment plan = new DataCenterDeployment(0, null, null, null, null, null);
        DomainRouterVO result = null;
        assert router.getIsRedundantRouter();
        final List<Long> networkIds = _routerDao.getRouterNetworks(router.getId());

        DomainRouterVO routerToBeAvoid = null;
        List<DomainRouterVO> routerList = null;
        if (networkIds.size() != 0) {
            routerList = _routerDao.findByNetwork(networkIds.get(0));
        } else if (router.getVpcId() != null) {
            routerList = _routerDao.listByVpcId(router.getVpcId());
        }
        if (routerList != null) {
            for (final DomainRouterVO rrouter : routerList) {
                if (rrouter.getHostId() != null && rrouter.getIsRedundantRouter() && rrouter.getState() == State.Running) {
                    if (routerToBeAvoid != null) {
                        throw new ResourceUnavailableException("Try to start router " + router.getInstanceName() + "(" + router.getId() + ")"
                                + ", but there are already two redundant routers with IP " + router.getPublicIpAddress() + ", they are " + rrouter.getInstanceName() + "("
                                + rrouter.getId() + ") and " + routerToBeAvoid.getInstanceName() + "(" + routerToBeAvoid.getId() + ")", DataCenter.class,
                                rrouter.getDataCenterId());
                    }
                    routerToBeAvoid = rrouter;
                }
            }
        }
        if (routerToBeAvoid == null) {
            return start(router, user, caller, params, null);
        }
        // We would try best to deploy the router to another place
        final int retryIndex = 5;
        final ExcludeList[] avoids = new ExcludeList[5];
        avoids[0] = new ExcludeList();
        avoids[0].addPod(routerToBeAvoid.getPodIdToDeployIn());
        avoids[1] = new ExcludeList();
        avoids[1].addCluster(_hostDao.findById(routerToBeAvoid.getHostId()).getClusterId());
        avoids[2] = new ExcludeList();
        final List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(routerToBeAvoid.getId(), Volume.Type.ROOT);
        if (volumes != null && volumes.size() != 0) {
            avoids[2].addPool(volumes.get(0).getPoolId());
        }
        avoids[2].addHost(routerToBeAvoid.getHostId());
        avoids[3] = new ExcludeList();
        avoids[3].addHost(routerToBeAvoid.getHostId());
        avoids[4] = new ExcludeList();

        for (int i = 0; i < retryIndex; i++) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Try to deploy redundant virtual router:" + router.getHostName() + ", for " + i + " time");
            }
            plan.setAvoids(avoids[i]);
            try {
                result = start(router, user, caller, params, plan);
            } catch (final InsufficientServerCapacityException ex) {
                result = null;
            }
            if (result != null) {
                break;
            }
        }
        return result;
    }

    protected String retrieveTemplateName(final HypervisorType hType, final long datacenterId) {
        String templateName = null;

        if (hType == HypervisorType.BareMetal) {
            final ConfigKey<String> hypervisorConfigKey = hypervisorsMap.get(HypervisorType.VMware);
            templateName = hypervisorConfigKey.valueIn(datacenterId);
        } else {
            // Returning NULL is fine because the simulator will need it when
            // being used instead of a real hypervisor.
            // The hypervisorsMap contains only real hypervisors.
            final ConfigKey<String> hypervisorConfigKey = hypervisorsMap.get(hType);

            if (hypervisorConfigKey != null) {
                templateName = hypervisorConfigKey.valueIn(datacenterId);
            }
        }

        return templateName;
    }

    @Override
    public DomainRouterVO deployRouter(final RouterDeploymentDefinition routerDeploymentDefinition, final boolean startRouter)
            throws InsufficientAddressCapacityException, InsufficientServerCapacityException, InsufficientCapacityException, StorageUnavailableException, ResourceUnavailableException {

        final ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(routerDeploymentDefinition.getServiceOfferingId());
        final Account owner = routerDeploymentDefinition.getOwner();

        // Router is the network element, we don't know the hypervisor type yet.
        // Try to allocate the domR twice using diff hypervisors, and when
        // failed both times, throw the exception up
        final List<HypervisorType> hypervisors = getHypervisors(routerDeploymentDefinition);

        int allocateRetry = 0;
        int startRetry = 0;
        DomainRouterVO router = null;
        for (final Iterator<HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            final HypervisorType hType = iter.next();
            try {
                final long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Allocating the VR with id=%s in datacenter %s with the hypervisor type %s", id, routerDeploymentDefinition.getDest()
                            .getDataCenter(), hType));
                }

                final String templateName = retrieveTemplateName(hType, routerDeploymentDefinition.getDest().getDataCenter().getId());
                final VMTemplateVO template = _templateDao.findRoutingTemplate(hType, templateName);

                if (template == null) {
                    s_logger.debug(hType + " won't support system vm, skip it");
                    continue;
                }

                final boolean offerHA = routerOffering.isOfferHA();

                // routerDeploymentDefinition.getVpc().getId() ==> do not use
                // VPC because it is not a VPC offering.
                final Long vpcId = routerDeploymentDefinition.getVpc() != null ? routerDeploymentDefinition.getVpc().getId() : null;

                long userId = CallContext.current().getCallingUserId();
                if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
                    final List<UserVO> userVOs = _userDao.listByAccount(owner.getAccountId());
                    if (!userVOs.isEmpty()) {
                        userId =  userVOs.get(0).getId();
                    }
                }

                router = new DomainRouterVO(id, routerOffering.getId(), routerDeploymentDefinition.getVirtualProvider().getId(), VirtualMachineName.getRouterName(id,
                        s_vmInstanceName), template.getId(), template.getHypervisorType(), template.getGuestOSId(), owner.getDomainId(), owner.getId(),
                        userId, routerDeploymentDefinition.isRedundant(), RedundantState.UNKNOWN, offerHA, false, vpcId);

                router.setDynamicallyScalable(template.isDynamicallyScalable());
                router.setRole(Role.VIRTUAL_ROUTER);
                router = _routerDao.persist(router);

                reallocateRouterNetworks(routerDeploymentDefinition, router, template, null);
                router = _routerDao.findById(router.getId());
            } catch (final InsufficientCapacityException ex) {
                if (allocateRetry < 2 && iter.hasNext()) {
                    s_logger.debug("Failed to allocate the VR with hypervisor type " + hType + ", retrying one more time");
                    continue;
                } else {
                    throw ex;
                }
            } finally {
                allocateRetry++;
            }

            if (startRouter) {
                try {
                    router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), routerDeploymentDefinition.getParams());
                    break;
                } catch (final InsufficientCapacityException ex) {
                    if (startRetry < 2 && iter.hasNext()) {
                        s_logger.debug("Failed to start the VR  " + router + " with hypervisor type " + hType + ", " + "destroying it and recreating one more time");
                        // destroy the router
                        destroyRouter(router.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM);
                        continue;
                    } else {
                        throw ex;
                    }
                } finally {
                    startRetry++;
                }
            } else {
                // return stopped router
                return router;
            }
        }

        return router;
    }

    protected void filterSupportedHypervisors(final List<HypervisorType> hypervisors) {
        // For non vpc we keep them all assuming all types in the list are
        // supported
    }

    protected String getNoHypervisorsErrMsgDetails() {
        return "";
    }

    protected List<HypervisorType> getHypervisors(final RouterDeploymentDefinition routerDeploymentDefinition) throws InsufficientServerCapacityException {
        final DeployDestination dest = routerDeploymentDefinition.getDest();
        List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();
        final HypervisorType defaults = _resourceMgr.getDefaultHypervisor(dest.getDataCenter().getId());
        if (defaults != HypervisorType.None) {
            hypervisors.add(defaults);
        }
        if (dest.getCluster() != null) {
            if (dest.getCluster().getHypervisorType() == HypervisorType.Ovm) {
                hypervisors.add(getClusterToStartDomainRouterForOvm(dest.getCluster().getPodId()));
            } else {
                hypervisors.add(dest.getCluster().getHypervisorType());
            }
        } else if (defaults == HypervisorType.None) {
            hypervisors = _resourceMgr.getSupportedHypervisorTypes(dest.getDataCenter().getId(), true, routerDeploymentDefinition.getPlan().getPodId());
        }

        filterSupportedHypervisors(hypervisors);

        if (hypervisors.isEmpty()) {
            if (routerDeploymentDefinition.getPodId() != null) {
                throw new InsufficientServerCapacityException("Unable to create virtual router, there are no clusters in the pod." + getNoHypervisorsErrMsgDetails(), Pod.class,
                        routerDeploymentDefinition.getPodId());
            }
            throw new InsufficientServerCapacityException("Unable to create virtual router, there are no clusters in the zone." + getNoHypervisorsErrMsgDetails(),
                    DataCenter.class, dest.getDataCenter().getId());
        }
        return hypervisors;
    }

    /*
     * Ovm won't support any system. So we have to choose a partner cluster in
     * the same pod to start domain router for us
     */
    protected HypervisorType getClusterToStartDomainRouterForOvm(final long podId) {
        final List<ClusterVO> clusters = _clusterDao.listByPodId(podId);
        for (final ClusterVO cv : clusters) {
            if (cv.getHypervisorType() == HypervisorType.Ovm || cv.getHypervisorType() == HypervisorType.BareMetal) {
                continue;
            }

            final List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cv.getId());
            if (hosts == null || hosts.isEmpty()) {
                continue;
            }

            for (final HostVO h : hosts) {
                if (h.getState() == Status.Up) {
                    s_logger.debug("Pick up host that has hypervisor type " + h.getHypervisorType() + " in cluster " + cv.getId() + " to start domain router for OVM");
                    return h.getHypervisorType();
                }
            }
        }

        final String errMsg = new StringBuilder("Cannot find an available cluster in Pod ").append(podId)
                .append(" to start domain router for Ovm. \n Ovm won't support any system vm including domain router, ")
                .append("please make sure you have a cluster with hypervisor type of any of xenserver/KVM/Vmware in the same pod")
                .append(" with Ovm cluster. And there is at least one host in UP status in that cluster.").toString();
        throw new CloudRuntimeException(errMsg);
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> configureControlNic(final RouterDeploymentDefinition routerDeploymentDefinition) {
        final LinkedHashMap<Network, List<? extends NicProfile>> controlConfig = new LinkedHashMap<Network, List<? extends NicProfile>>(3);

        s_logger.debug("Adding nic for Virtual Router in Control network ");
        final List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        final NetworkOffering controlOffering = offerings.get(0);
        final Network controlNic = _networkMgr.setupNetwork(s_systemAccount, controlOffering, routerDeploymentDefinition.getPlan(), null, null, false).get(0);

        controlConfig.put(controlNic, new ArrayList<NicProfile>());

        return controlConfig;
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> configurePublicNic(final RouterDeploymentDefinition routerDeploymentDefinition, final boolean hasGuestNic) throws InsufficientAddressCapacityException {
        final LinkedHashMap<Network, List<? extends NicProfile>> publicConfig = new LinkedHashMap<Network, List<? extends NicProfile>>(3);

        if (routerDeploymentDefinition.isPublicNetwork()) {
            s_logger.debug("Adding nic for Virtual Router in Public network ");
            // if source nat service is supported by the network, get the source
            // nat ip address
            final NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            final PublicIp sourceNatIp = routerDeploymentDefinition.getSourceNatIP();
            defaultNic.setIPv4Address(sourceNatIp.getAddress().addr());
            defaultNic.setIPv4Gateway(sourceNatIp.getGateway());
            defaultNic.setIPv4Netmask(sourceNatIp.getNetmask());
            defaultNic.setMacAddress(sourceNatIp.getMacAddress());
            // get broadcast from public network
            final Network pubNet = _networkDao.findById(sourceNatIp.getNetworkId());
            if (pubNet.getBroadcastDomainType() == BroadcastDomainType.Vxlan) {
                defaultNic.setBroadcastType(BroadcastDomainType.Vxlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vxlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(BroadcastDomainType.Vxlan.toUri(sourceNatIp.getVlanTag()));
            } else {
                defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(IsolationType.Vlan.toUri(sourceNatIp.getVlanTag()));
            }

            //If guest nic has already been added we will have 2 devices in the list.
            if (hasGuestNic) {
                defaultNic.setDeviceId(2);
            }

            final NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
            final List<? extends Network> publicNetworks = _networkMgr.setupNetwork(s_systemAccount, publicOffering, routerDeploymentDefinition.getPlan(), null, null, false);
            final String publicIp = defaultNic.getIPv4Address();
            // We want to use the identical MAC address for RvR on public
            // interface if possible
            final NicVO peerNic = _nicDao.findByIp4AddressAndNetworkId(publicIp, publicNetworks.get(0).getId());
            if (peerNic != null) {
                s_logger.info("Use same MAC as previous RvR, the MAC is " + peerNic.getMacAddress());
                defaultNic.setMacAddress(peerNic.getMacAddress());
            }
            if (routerDeploymentDefinition.getGuestNetwork() != null) {
                ipv6Service.updateNicIpv6(defaultNic, routerDeploymentDefinition.getDest().getDataCenter(), routerDeploymentDefinition.getGuestNetwork());
            }
            publicConfig.put(publicNetworks.get(0), new ArrayList<NicProfile>(Arrays.asList(defaultNic)));
        }

        return publicConfig;
    }

    @Override
    public LinkedHashMap<Network, List<? extends NicProfile>> configureDefaultNics(final RouterDeploymentDefinition routerDeploymentDefinition) throws ConcurrentOperationException, InsufficientAddressCapacityException {

        final LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);

        // 1) Guest Network
        final LinkedHashMap<Network, List<? extends NicProfile>> guestNic = configureGuestNic(routerDeploymentDefinition);
        networks.putAll(guestNic);

        // 2) Control network
        final LinkedHashMap<Network, List<? extends NicProfile>> controlNic = configureControlNic(routerDeploymentDefinition);
        networks.putAll(controlNic);

        // 3) Public network
        final LinkedHashMap<Network, List<? extends NicProfile>> publicNic = configurePublicNic(routerDeploymentDefinition, networks.size() > 1);
        networks.putAll(publicNic);

        return networks;
    }

    @Override
    public LinkedHashMap<Network, List<? extends NicProfile>> configureGuestNic(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws ConcurrentOperationException, InsufficientAddressCapacityException {

        // Form networks
        final LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);
        // 1) Guest network
        final Network guestNetwork = routerDeploymentDefinition.getGuestNetwork();

        if (guestNetwork != null) {
            s_logger.debug("Adding nic for Virtual Router in Guest network " + guestNetwork);
            String defaultNetworkStartIp = null, defaultNetworkStartIpv6 = null;
            final Nic placeholder = _networkModel.getPlaceholderNicForRouter(guestNetwork, routerDeploymentDefinition.getPodId());
            if (!routerDeploymentDefinition.isPublicNetwork()) {
                if (guestNetwork.getCidr() != null) {
                    if (placeholder != null && placeholder.getIPv4Address() != null) {
                        s_logger.debug("Requesting ipv4 address " + placeholder.getIPv4Address() + " stored in placeholder nic for the network "
                                + guestNetwork);
                        defaultNetworkStartIp = placeholder.getIPv4Address();
                    } else {
                        NetworkDetailVO routerIpDetail = networkDetailsDao.findDetail(guestNetwork.getId(), ApiConstants.ROUTER_IP);
                        String routerIp = routerIpDetail != null ? routerIpDetail.getValue() : null;
                        if (routerIp != null) {
                            defaultNetworkStartIp = routerIp;
                        } else {
                            final String startIp = _networkModel.getStartIpAddress(guestNetwork.getId());
                            if (startIp != null
                                    && _ipAddressDao.findByIpAndSourceNetworkId(guestNetwork.getId(), startIp).getAllocatedTime() == null) {
                                defaultNetworkStartIp = startIp;
                            } else if (s_logger.isDebugEnabled()) {
                                s_logger.debug("First ipv4 " + startIp + " in network id=" + guestNetwork.getId()
                                        + " is already allocated, can't use it for domain router; will get random ip address from the range");
                            }
                        }
                    }
                }

                if (guestNetwork.getIp6Cidr() != null) {
                    if (placeholder != null && placeholder.getIPv6Address() != null) {
                        s_logger.debug("Requesting ipv6 address " + placeholder.getIPv6Address() + " stored in placeholder nic for the network "
                                + guestNetwork);
                        defaultNetworkStartIpv6 = placeholder.getIPv6Address();
                    } else {
                        NetworkDetailVO routerIpDetail = networkDetailsDao.findDetail(guestNetwork.getId(), ApiConstants.ROUTER_IPV6);
                        String routerIpv6 = routerIpDetail != null ? routerIpDetail.getValue() : null;
                        if (routerIpv6 != null) {
                            defaultNetworkStartIpv6 = routerIpv6;
                        } else {
                            final String startIpv6 = _networkModel.getStartIpv6Address(guestNetwork.getId());
                            if (startIpv6 != null && _ipv6Dao.findByNetworkIdAndIp(guestNetwork.getId(), startIpv6) == null) {
                                defaultNetworkStartIpv6 = startIpv6;
                            } else if (s_logger.isDebugEnabled()) {
                                s_logger.debug("First ipv6 " + startIpv6 + " in network id=" + guestNetwork.getId()
                                        + " is already allocated, can't use it for domain router; will get random ipv6 address from the range");
                            }
                        }
                    }
                }
            } else if (placeholder != null) {
                // Remove placeholder nic if router has public network
                _nicDao.remove(placeholder.getId());
            }

            final NicProfile gatewayNic = new NicProfile(defaultNetworkStartIp, defaultNetworkStartIpv6);
            if (routerDeploymentDefinition.isPublicNetwork()) {
                if (routerDeploymentDefinition.isRedundant()) {
                    gatewayNic.setIPv4Address(this.acquireGuestIpAddressForVrouterRedundant(guestNetwork));
                } else {
                    gatewayNic.setIPv4Address(guestNetwork.getGateway());
                }
                gatewayNic.setBroadcastUri(guestNetwork.getBroadcastUri());
                gatewayNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
                gatewayNic.setIsolationUri(guestNetwork.getBroadcastUri());
                gatewayNic.setMode(guestNetwork.getMode());
                final String gatewayCidr = _networkModel.getValidNetworkCidr(guestNetwork);
                gatewayNic.setIPv4Netmask(NetUtils.getCidrNetmask(gatewayCidr));
            } else {
                gatewayNic.setDefaultNic(true);
            }

            networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(gatewayNic)));
        }
        return networks;
    }

    @Override
    public void reallocateRouterNetworks(final RouterDeploymentDefinition routerDeploymentDefinition, final VirtualRouter router, final VMTemplateVO template, final HypervisorType hType)
            throws ConcurrentOperationException, InsufficientCapacityException {
        final ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(routerDeploymentDefinition.getServiceOfferingId());

        final LinkedHashMap<Network, List<? extends NicProfile>> networks = configureDefaultNics(routerDeploymentDefinition);

        _itMgr.allocate(router.getInstanceName(), template, routerOffering, networks, routerDeploymentDefinition.getPlan(), hType);
    }

    public static void setSystemAccount(final Account systemAccount) {
        s_systemAccount = systemAccount;
    }

    public static void setVMInstanceName(final String vmInstanceName) {
        s_vmInstanceName = vmInstanceName;
    }
    @Override
    public boolean validateHAProxyLBRule(final LoadBalancingRule rule) {
        final String timeEndChar = "dhms";
        int haproxy_stats_port = Integer.parseInt(_configDao.getValue(Config.NetworkLBHaproxyStatsPort.key()));
        if (rule.getSourcePortStart() == haproxy_stats_port) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Can't create LB on port "+ haproxy_stats_port +", haproxy is listening for  LB stats on this port");
            }
            return false;
        }
        String lbProtocol = rule.getLbProtocol();
        if (lbProtocol != null && lbProtocol.toLowerCase().equals(NetUtils.UDP_PROTO)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Can't create LB rule as haproxy does not support udp");
            }
            return false;
        }

        List<String> lbProtocols = Arrays.asList("tcp", "udp", "tcp-proxy", "ssl");
        if (rule.getLbProtocol() != null && ! lbProtocols.contains(rule.getLbProtocol())) {
            throw new InvalidParameterValueException("protocol " + rule.getLbProtocol() + " is not in valid protocols " + lbProtocols);
        }

        for (final LoadBalancingRule.LbStickinessPolicy stickinessPolicy : rule.getStickinessPolicies()) {
            final List<Pair<String, String>> paramsList = stickinessPolicy.getParams();

            if (LbStickinessMethod.StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {

            } else if (LbStickinessMethod.StickinessMethodType.SourceBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String tablesize = "200k"; // optional
                String expire = "30m"; // optional

                /* overwrite default values with the stick parameters */
                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("tablesize".equalsIgnoreCase(key)) {
                        tablesize = value;
                    }
                    if ("expire".equalsIgnoreCase(key)) {
                        expire = value;
                    }
                }
                if (expire != null && !containsOnlyNumbers(expire, timeEndChar)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: expire is not in timeformat: " + expire);
                }
                if (tablesize != null && !containsOnlyNumbers(tablesize, "kmg")) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: tablesize is not in size format: " + tablesize);

                }
            } else if (LbStickinessMethod.StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String length = null; // optional
                String holdTime = null; // optional

                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("length".equalsIgnoreCase(key)) {
                        length = value;
                    }
                    if ("holdtime".equalsIgnoreCase(key)) {
                        holdTime = value;
                    }
                }

                if (length != null && !containsOnlyNumbers(length, null)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: length is not a number: " + length);
                }
                if (holdTime != null && !containsOnlyNumbers(holdTime, timeEndChar) && !containsOnlyNumbers(holdTime, null)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: holdtime is not in timeformat: " + holdTime);
                }
            }
        }
        return true;
    }

    /*
     * This function detects numbers like 12 ,32h ,42m .. etc,. 1) plain number
     * like 12 2) time or tablesize like 12h, 34m, 45k, 54m , here last
     * character is non-digit but from known characters .
     */
    private static boolean containsOnlyNumbers(final String str, final String endChar) {
        if (str == null) {
            return false;
        }

        String number = str;
        if (endChar != null) {
            boolean matchedEndChar = false;
            if (str.length() < 2) {
                return false; // at least one numeric and one char. example:
            }
            // 3h
            final char strEnd = str.toCharArray()[str.length() - 1];
            for (final char c : endChar.toCharArray()) {
                if (strEnd == c) {
                    number = str.substring(0, str.length() - 1);
                    matchedEndChar = true;
                    break;
                }
            }
            if (!matchedEndChar) {
                return false;
            }
        }
        try {
            Integer.parseInt(number);
        } catch (final NumberFormatException e) {
            return false;
        }
        return true;
    }

    public String acquireGuestIpAddressForVrouterRedundant(Network network) {
        return _ipAddrMgr.acquireGuestIpAddressByPlacement(network, null);
    }
}
