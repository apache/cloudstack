package com.cloud.network.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
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
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.maint.Version;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
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

public class NetworkGeneralHelper {

    private static final Logger s_logger = Logger.getLogger(NetworkGeneralHelper.class);


    @Inject
    NicDao _nicDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    NetworkOrchestrationService _networkMgr;

    public String getRouterControlIp(final long routerId) {
        String routerControlIpAddress = null;
        final List<NicVO> nics = _nicDao.listByVmId(routerId);
        for (final NicVO n : nics) {
            final NetworkVO nc = _networkDao.findById(n.getNetworkId());
            if (nc != null && nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
                // router will have only one control ip
                break;
            }
        }

        if (routerControlIpAddress == null) {
            s_logger.warn("Unable to find router's control ip in its attached NICs!. routerId: " + routerId);
            final DomainRouterVO router = _routerDao.findById(routerId);
            return router.getPrivateIpAddress();
        }

        return routerControlIpAddress;
    }

    public String getRouterIpInNetwork(final long networkId, final long instanceId) {
        return _nicDao.getIpAddress(networkId, instanceId);
    }


//    @Override
    public boolean sendCommandsToRouter(final VirtualRouter router, final Commands cmds) throws AgentUnavailableException {
        if(!checkRouterVersion(router)){
            s_logger.debug("Router requires upgrade. Unable to send command to router:" + router.getId() + ", router template version : " + router.getTemplateVersion()
                    + ", minimal required version : " + VirtualNetworkApplianceService.MinVRVersion);
            throw new CloudRuntimeException("Unable to send command. Upgrade in progress. Please contact administrator.");
        }
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }

        if (answers == null) {
            return false;
        }

        if (answers.length != cmds.size()) {
            return false;
        }

        // FIXME: Have to return state for individual command in the future
        boolean result = true;
        if (answers.length > 0) {
            for (final Answer answer : answers) {
                if (!answer.getResult()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }


//    @Override
    public NicTO getNicTO(final VirtualRouter router, Long networkId, String broadcastUri) {
        NicProfile nicProfile = _networkModel.getNicProfile(router, networkId, broadcastUri);

        return _itMgr.toNicTO(nicProfile, router.getHypervisorType());
    }

//    @Override
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
        _routerDao.remove(router.getId());
        return router;
    }

    /**
     * Checks if the router is at the required version. Compares MS version and router version.
     *
     * @param router
     * @return
     */
//    @Override
    public boolean checkRouterVersion(final VirtualRouter router) {
        if(!VirtualNetworkApplianceManagerImpl.routerVersionCheckEnabled.value()){
            //Router version check is disabled.
            return true;
        }
        if(router.getTemplateVersion() == null){
            return false;
        }
        final String trimmedVersion = Version.trimRouterVersion(router.getTemplateVersion());
        return (Version.compare(trimmedVersion, VirtualNetworkApplianceService.MinVRVersion) >= 0);
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
        // We don't want the failure of VPN Connection affect the status of router, so we try to make connection
        // only after router start successfully
        final Long vpcId = router.getVpcId();
        if (vpcId != null) {
            _s2sVpnMgr.reconnectDisconnectedVpnByVpc(vpcId);
        }
        return _routerDao.findById(router.getId());
    }

    protected DomainRouterVO waitRouter(DomainRouterVO router) {
        DomainRouterVO vm = _routerDao.findById(router.getId());

        if (s_logger.isDebugEnabled())
            s_logger.debug("Router " + router.getInstanceName() + " is not fully up yet, we will wait");
        while (vm.getState() == State.Starting) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            // reload to get the latest state info
            vm = _routerDao.findById(router.getId());
        }

        if (vm.getState() == State.Running) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Router " + router.getInstanceName() + " is now fully up");

            return router;
        }

        s_logger.warn("Router " + router.getInstanceName() + " failed to start. current state: " + vm.getState());
        return null;
    }


//    @Override
    public List<DomainRouterVO> startRouters(final Map<Param, Object> params, final List<DomainRouterVO> routers) throws StorageUnavailableException,
    InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> runningRouters = null;

        if (routers != null) {
            runningRouters = new ArrayList<DomainRouterVO>();
        }

        for (DomainRouterVO router : routers) {
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
                    router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), params);
                }
                if (router != null) {
                    runningRouters.add(router);
                }
            }
        }
        return runningRouters;
    }

//    @Override
    public DomainRouterVO startVirtualRouter(DomainRouterVO router, User user, Account caller, Map<Param, Object> params)
            throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {

        if (router.getRole() != Role.VIRTUAL_ROUTER || !router.getIsRedundantRouter()) {
            return this.start(router, user, caller, params, null);
        }

        if (router.getState() == State.Running) {
            s_logger.debug("Redundant router " + router.getInstanceName() + " is already running!");
            return router;
        }

        //
        // If another thread has already requested a VR start, there is a transition period for VR to transit from
        // Starting to Running, there exist a race conditioning window here
        // We will wait until VR is up or fail
        if (router.getState() == State.Starting) {
            return waitRouter(router);
        }

        DataCenterDeployment plan = new DataCenterDeployment(0, null, null, null, null, null);
        DomainRouterVO result = null;
        assert router.getIsRedundantRouter();
        final List<Long> networkIds = _routerDao.getRouterNetworks(router.getId());
        //Not support VPC now
        if (networkIds.size() > 1) {
            throw new ResourceUnavailableException("Unable to support more than one guest network for redundant router now!", DataCenter.class, router.getDataCenterId());
        }
        DomainRouterVO routerToBeAvoid = null;
        if (networkIds.size() != 0) {
            final List<DomainRouterVO> routerList = _routerDao.findByNetwork(networkIds.get(0));
            for (final DomainRouterVO rrouter : routerList) {
                if (rrouter.getHostId() != null && rrouter.getIsRedundantRouter() && rrouter.getState() == State.Running) {
                    if (routerToBeAvoid != null) {
                        throw new ResourceUnavailableException("Try to start router " + router.getInstanceName() + "(" + router.getId() + ")" +
                                ", but there are already two redundant routers with IP " + router.getPublicIpAddress() + ", they are " + rrouter.getInstanceName() + "(" +
                                rrouter.getId() + ") and " + routerToBeAvoid.getInstanceName() + "(" + routerToBeAvoid.getId() + ")", DataCenter.class,
                                rrouter.getDataCenterId());
                    }
                    routerToBeAvoid = rrouter;
                }
            }
        }
        if (routerToBeAvoid == null) {
            return this.start(router, user, caller, params, null);
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
                result = this.start(router, user, caller, params, plan);
            } catch (final InsufficientServerCapacityException ex) {
                result = null;
            }
            if (result != null) {
                break;
            }
        }
        return result;
    }


//    @Override
    public DomainRouterVO deployRouter(Account owner,
            DeployDestination dest, DeploymentPlan plan,
            Map<Param, Object> params, boolean isRedundant,
            VirtualRouterProvider vrProvider, long svcOffId, Long vpcId,
            LinkedHashMap<Network, List<? extends NicProfile>> networks,
            boolean startRouter, List<HypervisorType> supportedHypervisors)
            throws InsufficientAddressCapacityException,
            InsufficientServerCapacityException, InsufficientCapacityException,
            StorageUnavailableException, ResourceUnavailableException {

        final ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(svcOffId);

        // Router is the network element, we don't know the hypervisor type yet.
        // Try to allocate the domR twice using diff hypervisors, and when failed both times, throw the exception up
        final List<HypervisorType> hypervisors = getHypervisors(dest, plan, supportedHypervisors);

        int allocateRetry = 0;
        int startRetry = 0;
        DomainRouterVO router = null;
        for (final Iterator<HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            final HypervisorType hType = iter.next();
            try {
                final long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Allocating the VR i=" + id + " in datacenter " + dest.getDataCenter() + "with the hypervisor type " + hType);
                }

                String templateName = null;
                switch (hType) {
                case XenServer:
                    templateName = VirtualNetworkApplianceManager.RouterTemplateXen.valueIn(dest.getDataCenter().getId());
                    break;
                case KVM:
                    templateName = VirtualNetworkApplianceManager.RouterTemplateKvm.valueIn(dest.getDataCenter().getId());
                    break;
                case VMware:
                    templateName = VirtualNetworkApplianceManager.RouterTemplateVmware.valueIn(dest.getDataCenter().getId());
                    break;
                case Hyperv:
                    templateName = VirtualNetworkApplianceManager.RouterTemplateHyperV.valueIn(dest.getDataCenter().getId());
                    break;
                case LXC:
                    templateName = VirtualNetworkApplianceManager.RouterTemplateLxc.valueIn(dest.getDataCenter().getId());
                    break;
                default:
                    break;
                }
                final VMTemplateVO template = _templateDao.findRoutingTemplate(hType, templateName);

                if (template == null) {
                    s_logger.debug(hType + " won't support system vm, skip it");
                    continue;
                }

                boolean offerHA = routerOffering.getOfferHA();
                /* We don't provide HA to redundant router VMs, admin should own it all, and redundant router themselves are HA */
                if (isRedundant) {
                    offerHA = false;
                }

                router =
                        new DomainRouterVO(id, routerOffering.getId(), vrProvider.getId(),
                                VirtualMachineName.getRouterName(id, VirtualNwStatus.instance), template.getId(), template.getHypervisorType(),
                                template.getGuestOSId(), owner.getDomainId(), owner.getId(), isRedundant, 0, false, RedundantState.UNKNOWN,
                                offerHA, false, vpcId);
                router.setDynamicallyScalable(template.isDynamicallyScalable());
                router.setRole(Role.VIRTUAL_ROUTER);
                router = _routerDao.persist(router);
                _itMgr.allocate(router.getInstanceName(), template, routerOffering, networks, plan, null);
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
                    router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), params);
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
                //return stopped router
                return router;
            }
        }

        return router;
    }

    protected List<HypervisorType> getHypervisors(final DeployDestination dest, final DeploymentPlan plan, final List<HypervisorType> supportedHypervisors)
            throws InsufficientServerCapacityException {
        List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();

        if (dest.getCluster() != null) {
            if (dest.getCluster().getHypervisorType() == HypervisorType.Ovm) {
                hypervisors.add(getClusterToStartDomainRouterForOvm(dest.getCluster().getPodId()));
            } else {
                hypervisors.add(dest.getCluster().getHypervisorType());
            }
        } else {
            final HypervisorType defaults = _resourceMgr.getDefaultHypervisor(dest.getDataCenter().getId());
            if (defaults != HypervisorType.None) {
                hypervisors.add(defaults);
            } else {
                //if there is no default hypervisor, get it from the cluster
                hypervisors = _resourceMgr.getSupportedHypervisorTypes(dest.getDataCenter().getId(), true, plan.getPodId());
            }
        }

        //keep only elements defined in supported hypervisors
        final StringBuilder hTypesStr = new StringBuilder();
        if (supportedHypervisors != null && !supportedHypervisors.isEmpty()) {
            hypervisors.retainAll(supportedHypervisors);
            for (final HypervisorType hType : supportedHypervisors) {
                hTypesStr.append(hType).append(" ");
            }
        }

        if (hypervisors.isEmpty()) {
            final String errMsg = (hTypesStr.capacity() > 0) ? "supporting hypervisors " + hTypesStr.toString() : "";
            if (plan.getPodId() != null) {
                throw new InsufficientServerCapacityException("Unable to create virtual router, " + "there are no clusters in the pod " + errMsg, Pod.class,
                        plan.getPodId());
            }
            throw new InsufficientServerCapacityException("Unable to create virtual router, " + "there are no clusters in the zone " + errMsg, DataCenter.class,
                    dest.getDataCenter().getId());
        }
        return hypervisors;
    }

    /*
     * Ovm won't support any system. So we have to choose a partner cluster in the same pod to start domain router for us
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

        final String errMsg =
                "Cannot find an available cluster in Pod " + podId + " to start domain router for Ovm. \n Ovm won't support any system vm including domain router, " +
                        "please make sure you have a cluster with hypervisor type of any of xenserver/KVM/Vmware in the same pod" +
                        " with Ovm cluster. And there is at least one host in UP status in that cluster.";
        throw new CloudRuntimeException(errMsg);
    }


//    @Override
    public LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks(
            Account owner, boolean isRedundant, DeploymentPlan plan,
            Network guestNetwork, Pair<Boolean, PublicIp> publicNetwork)
            throws ConcurrentOperationException,
            InsufficientAddressCapacityException {

        boolean setupPublicNetwork = false;
        if (publicNetwork != null) {
            setupPublicNetwork = publicNetwork.first();
        }

        // Form networks
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(
                3);
        // 1) Guest network
        boolean hasGuestNetwork = false;
        if (guestNetwork != null) {
            s_logger.debug("Adding nic for Virtual Router in Guest network "
                    + guestNetwork);
            String defaultNetworkStartIp = null, defaultNetworkStartIpv6 = null;
            if (!setupPublicNetwork) {
                final Nic placeholder = _networkModel
                        .getPlaceholderNicForRouter(guestNetwork,
                                plan.getPodId());
                if (guestNetwork.getCidr() != null) {
                    if (placeholder != null
                            && placeholder.getIp4Address() != null) {
                        s_logger.debug("Requesting ipv4 address "
                                + placeholder.getIp4Address()
                                + " stored in placeholder nic for the network "
                                + guestNetwork);
                        defaultNetworkStartIp = placeholder.getIp4Address();
                    } else {
                        final String startIp = _networkModel
                                .getStartIpAddress(guestNetwork.getId());
                        if (startIp != null
                                && _ipAddressDao.findByIpAndSourceNetworkId(
                                        guestNetwork.getId(), startIp)
                                        .getAllocatedTime() == null) {
                            defaultNetworkStartIp = startIp;
                        } else if (s_logger.isDebugEnabled()) {
                            s_logger.debug("First ipv4 "
                                    + startIp
                                    + " in network id="
                                    + guestNetwork.getId()
                                    + " is already allocated, can't use it for domain router; will get random ip address from the range");
                        }
                    }
                }

                if (guestNetwork.getIp6Cidr() != null) {
                    if (placeholder != null
                            && placeholder.getIp6Address() != null) {
                        s_logger.debug("Requesting ipv6 address "
                                + placeholder.getIp6Address()
                                + " stored in placeholder nic for the network "
                                + guestNetwork);
                        defaultNetworkStartIpv6 = placeholder.getIp6Address();
                    } else {
                        final String startIpv6 = _networkModel
                                .getStartIpv6Address(guestNetwork.getId());
                        if (startIpv6 != null
                                && _ipv6Dao.findByNetworkIdAndIp(
                                        guestNetwork.getId(), startIpv6) == null) {
                            defaultNetworkStartIpv6 = startIpv6;
                        } else if (s_logger.isDebugEnabled()) {
                            s_logger.debug("First ipv6 "
                                    + startIpv6
                                    + " in network id="
                                    + guestNetwork.getId()
                                    + " is already allocated, can't use it for domain router; will get random ipv6 address from the range");
                        }
                    }
                }
            }

            final NicProfile gatewayNic = new NicProfile(defaultNetworkStartIp,
                    defaultNetworkStartIpv6);
            if (setupPublicNetwork) {
                if (isRedundant) {
                    gatewayNic.setIp4Address(_ipAddrMgr.acquireGuestIpAddress(
                            guestNetwork, null));
                } else {
                    gatewayNic.setIp4Address(guestNetwork.getGateway());
                }
                gatewayNic.setBroadcastUri(guestNetwork.getBroadcastUri());
                gatewayNic.setBroadcastType(guestNetwork
                        .getBroadcastDomainType());
                gatewayNic.setIsolationUri(guestNetwork.getBroadcastUri());
                gatewayNic.setMode(guestNetwork.getMode());
                final String gatewayCidr = guestNetwork.getCidr();
                gatewayNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
            } else {
                gatewayNic.setDefaultNic(true);
            }

            networks.put(guestNetwork,
                    new ArrayList<NicProfile>(Arrays.asList(gatewayNic)));
            hasGuestNetwork = true;
        }

        // 2) Control network
        s_logger.debug("Adding nic for Virtual Router in Control network ");
        List<? extends NetworkOffering> offerings = _networkModel
                .getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = offerings.get(0);
        Network controlConfig = _networkMgr.setupNetwork(VirtualNwStatus.account,
                controlOffering, plan, null, null, false).get(0);
        networks.put(controlConfig, new ArrayList<NicProfile>());
        // 3) Public network
        if (setupPublicNetwork) {
            final PublicIp sourceNatIp = publicNetwork.second();
            s_logger.debug("Adding nic for Virtual Router in Public network ");
            // if source nat service is supported by the network, get the source
            // nat ip address
            final NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            defaultNic.setIp4Address(sourceNatIp.getAddress().addr());
            defaultNic.setGateway(sourceNatIp.getGateway());
            defaultNic.setNetmask(sourceNatIp.getNetmask());
            defaultNic.setMacAddress(sourceNatIp.getMacAddress());
            // get broadcast from public network
            final Network pubNet = _networkDao.findById(sourceNatIp
                    .getNetworkId());
            if (pubNet.getBroadcastDomainType() == BroadcastDomainType.Vxlan) {
                defaultNic.setBroadcastType(BroadcastDomainType.Vxlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vxlan
                        .toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(BroadcastDomainType.Vxlan
                        .toUri(sourceNatIp.getVlanTag()));
            } else {
                defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vlan
                        .toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(IsolationType.Vlan.toUri(sourceNatIp
                        .getVlanTag()));
            }
            if (hasGuestNetwork) {
                defaultNic.setDeviceId(2);
            }
            final NetworkOffering publicOffering = _networkModel
                    .getSystemAccountNetworkOfferings(
                            NetworkOffering.SystemPublicNetwork).get(0);
            final List<? extends Network> publicNetworks = _networkMgr
                    .setupNetwork(VirtualNwStatus.account, publicOffering, plan, null,
                            null, false);
            final String publicIp = defaultNic.getIp4Address();
            // We want to use the identical MAC address for RvR on public
            // interface if possible
            final NicVO peerNic = _nicDao.findByIp4AddressAndNetworkId(
                    publicIp, publicNetworks.get(0).getId());
            if (peerNic != null) {
                s_logger.info("Use same MAC as previous RvR, the MAC is "
                        + peerNic.getMacAddress());
                defaultNic.setMacAddress(peerNic.getMacAddress());
            }
            networks.put(publicNetworks.get(0), new ArrayList<NicProfile>(
                    Arrays.asList(defaultNic)));
        }

        return networks;
    }

}