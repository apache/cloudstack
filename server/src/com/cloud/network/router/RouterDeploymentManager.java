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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class RouterDeploymentManager {

    private static final Logger logger = Logger.getLogger(RouterDeploymentManager.class);

    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcOfferingDao vpcOffDao;
    @Inject
    private PhysicalNetworkDao pNtwkDao;
    @Inject
    private VpcManager vpcMgr;
    @Inject
    private PhysicalNetworkServiceProviderDao physicalProviderDao;
    @Inject
    private VlanDao vlanDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private NetworkOrchestrationService networkMgr;
    @Inject
    private NetworkModel networkModel;
    @Inject
    private VirtualRouterProviderDao vrProviderDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private DomainRouterDao _routerDao = null;
    @Inject
    private PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    private NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    private VirtualRouterProviderDao _vrProviderDao;
    @Inject
    private IPAddressDao _ipAddressDao = null;
    @Inject
    private UserIpv6AddressDao _ipv6Dao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private HostPodDao _podDao = null;
    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private NicDao _nicDao;

    @Inject
    private NetworkGeneralHelper nwHelper;
    @Inject
    private VpcVirtualNetworkHelperImpl vpcHelper;


    protected ServiceOfferingVO offering;



    public void setOffering(ServiceOfferingVO offering) {
        this.offering = offering;
    }

    public Long getOfferingId() {
        return offering == null ? null : offering.getId();
    }

    public List<DomainRouterVO> deployVirtualRouter(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        if (routerDeploymentDefinition.getVpc() != null) {
            return deployVirtualRouterInVpc(routerDeploymentDefinition);
        } else {
            return deployVirtualRouterInGuestNetwork(routerDeploymentDefinition);
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // Non-VPC behavior
    ///////////////////////////////////////////////////////////////////////
    protected List<DomainRouterVO> deployVirtualRouterInGuestNetwork(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        findOrDeployVirtualRouterInGuestNetwork(routerDeploymentDefinition);

        return nwHelper.startRouters(routerDeploymentDefinition);
    }


    @DB
    protected void findOrDeployVirtualRouterInGuestNetwork(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        final Network guestNetwork = routerDeploymentDefinition.getGuestNetwork();
        final DeployDestination dest = routerDeploymentDefinition.getDest();

        List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        final Network lock = _networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Lock is acquired for network id " + lock.getId() + " as a part of router startup in " + dest);
        }

        try {

            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup ||
                    guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: " + guestNetwork;
            assert guestNetwork.getTrafficType() == TrafficType.Guest;

            // 1) Get deployment plan and find out the list of routers

            // dest has pod=null, for Basic Zone findOrDeployVRs for all Pods
            final List<DeployDestination> destinations = new ArrayList<DeployDestination>();

            // for basic zone, if 'dest' has pod set to null then this is network restart scenario otherwise it is a vm deployment scenario
            if (routerDeploymentDefinition.isBasic() && dest.getPod() == null) {
                // Find all pods in the data center with running or starting user vms
                final long dcId = dest.getDataCenter().getId();
                final List<HostPodVO> pods = listByDataCenterIdVMTypeAndStates(dcId, VirtualMachine.Type.User, VirtualMachine.State.Starting, VirtualMachine.State.Running);

                // Loop through all the pods skip those with running or starting VRs
                for (final HostPodVO pod : pods) {
                    // Get list of VRs in starting or running state
                    final long podId = pod.getId();
                    final List<DomainRouterVO> virtualRouters = _routerDao.listByPodIdAndStates(podId, VirtualMachine.State.Starting, VirtualMachine.State.Running);

                    assert (virtualRouters.size() <= 1) : "Pod can have utmost one VR in Basic Zone, please check!";

                    // Add virtualRouters to the routers, this avoids the situation when
                    // all routers are skipped and VirtualRouterElement throws exception
                    routers.addAll(virtualRouters);

                    // If List size is one, we already have a starting or running VR, skip deployment
                    if (virtualRouters.size() == 1) {
                        logger.debug("Skipping VR deployment: Found a running or starting VR in Pod " + pod.getName() + " id=" + podId);
                        continue;
                    }
                    // Add new DeployDestination for this pod
                    destinations.add(new DeployDestination(dest.getDataCenter(), pod, null, null));
                }
            } else {
                // Else, just add the supplied dest
                destinations.add(dest);
            }

            // Except for Basic Zone, the for loop will iterate only once
            for (final DeployDestination destination : destinations) {
                routerDeploymentDefinition.setDest(destination);
                planDeploymentRouters(routerDeploymentDefinition);
                routers = routerDeploymentDefinition.getRouters();

                // 2) Figure out required routers count
                int routerCount = 1;
                if (routerDeploymentDefinition.isRedundant()) {
                    routerCount = 2;
                    //Check current redundant routers, if possible(all routers are stopped), reset the priority
                    if (routers.size() != 0) {
                        checkAndResetPriorityOfRedundantRouter(routers);
                    }
                }

                // If old network is redundant but new is single router, then routers.size() = 2 but routerCount = 1
                if (routers.size() >= routerCount) {
                    return;
                }

                if (routers.size() >= 5) {
                    logger.error("Too much redundant routers!");
                }

                // Check if providers are supported in the physical networks
                final Type type = Type.VirtualRouter;
                final Long physicalNetworkId = _networkModel.getPhysicalNetworkId(guestNetwork);
                final PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + type.toString() + " in physical network " + physicalNetworkId);
                }
                final VirtualRouterProvider vrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), type);
                if (vrProvider == null) {
                    throw new CloudRuntimeException("Cannot find virtual router provider " + type.toString() + " as service provider " + provider.getId());
                }

                if (_networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
                    routerDeploymentDefinition.setOwner(_accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM));
                }

                // Check if public network has to be set on VR
                boolean publicNetwork = false;
                if (_networkModel.isProviderSupportServiceInNetwork(guestNetwork.getId(), Service.SourceNat, Provider.VirtualRouter)) {
                    publicNetwork = true;
                }
                if (routerDeploymentDefinition.isRedundant() && !publicNetwork) {
                    logger.error("Didn't support redundant virtual router without public network!");
                    routerDeploymentDefinition.setRouters(null);
                    return;
                }

                Long offeringId = _networkOfferingDao.findById(guestNetwork.getNetworkOfferingId()).getServiceOfferingId();
                if (offeringId == null) {
                    offeringId = getOfferingId();
                }

                PublicIp sourceNatIp = null;
                if (publicNetwork) {
                    sourceNatIp = _ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                            routerDeploymentDefinition.getOwner(), guestNetwork);
                }

                // 3) deploy virtual router(s)
                final int count = routerCount - routers.size();
                for (int i = 0; i < count; i++) {
                    LinkedHashMap<Network, List<? extends NicProfile>> networks =
                            createRouterNetworks(routerDeploymentDefinition, new Pair<Boolean, PublicIp>(
                            publicNetwork, sourceNatIp));
                    //don't start the router as we are holding the network lock that needs to be released at the end of router allocation
                    DomainRouterVO router = nwHelper.deployRouter(routerDeploymentDefinition, vrProvider, offeringId, networks, false, null);

                    if (router != null) {
                        _routerDao.addRouterToGuestNetwork(router, guestNetwork);
                        routers.add(router);
                    }
                }
            }
        } finally {
            if (lock != null) {
                _networkDao.releaseFromLockTable(lock.getId());
                if (logger.isDebugEnabled()) {
                    logger.debug("Lock is released for network id " + lock.getId() + " as a part of router startup in " + dest);
                }
            }
        }
    }

    protected List<HostPodVO> listByDataCenterIdVMTypeAndStates(final long id, final VirtualMachine.Type type, final VirtualMachine.State... states) {
        final SearchBuilder<VMInstanceVO> vmInstanceSearch = _vmDao.createSearchBuilder();
        vmInstanceSearch.and("type", vmInstanceSearch.entity().getType(), SearchCriteria.Op.EQ);
        vmInstanceSearch.and("states", vmInstanceSearch.entity().getState(), SearchCriteria.Op.IN);

        final SearchBuilder<HostPodVO> podIdSearch = _podDao.createSearchBuilder();
        podIdSearch.and("dc", podIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        podIdSearch.select(null, SearchCriteria.Func.DISTINCT, podIdSearch.entity().getId());
        podIdSearch.join("vmInstanceSearch", vmInstanceSearch, podIdSearch.entity().getId(), vmInstanceSearch.entity().getPodIdToDeployIn(), JoinBuilder.JoinType.INNER);
        podIdSearch.done();

        final SearchCriteria<HostPodVO> sc = podIdSearch.create();
        sc.setParameters("dc", id);
        sc.setJoinParameters("vmInstanceSearch", "type", type);
        sc.setJoinParameters("vmInstanceSearch", "states", (Object[])states);
        return _podDao.search(sc, null);
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks(final RouterDeploymentDefinition routerDeploymentDefinition,
            final Pair<Boolean, PublicIp> publicNetwork) throws ConcurrentOperationException, InsufficientAddressCapacityException {

        final Network guestNetwork = routerDeploymentDefinition.getGuestNetwork();
        boolean setupPublicNetwork = false;
        if (publicNetwork != null) {
            setupPublicNetwork = publicNetwork.first();
        }

        //Form networks
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);
        //1) Guest network
        boolean hasGuestNetwork = false;
        if (guestNetwork != null) {
            logger.debug("Adding nic for Virtual Router in Guest network " + guestNetwork);
            String defaultNetworkStartIp = null, defaultNetworkStartIpv6 = null;
            if (!setupPublicNetwork) {
                final Nic placeholder = _networkModel.getPlaceholderNicForRouter(guestNetwork, routerDeploymentDefinition.getPodId());
                if (guestNetwork.getCidr() != null) {
                    if (placeholder != null && placeholder.getIp4Address() != null) {
                        logger.debug("Requesting ipv4 address " + placeholder.getIp4Address() + " stored in placeholder nic for the network " + guestNetwork);
                        defaultNetworkStartIp = placeholder.getIp4Address();
                    } else {
                        final String startIp = _networkModel.getStartIpAddress(guestNetwork.getId());
                        if (startIp != null && _ipAddressDao.findByIpAndSourceNetworkId(guestNetwork.getId(), startIp).getAllocatedTime() == null) {
                            defaultNetworkStartIp = startIp;
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("First ipv4 " + startIp + " in network id=" + guestNetwork.getId() +
                                    " is already allocated, can't use it for domain router; will get random ip address from the range");
                        }
                    }
                }

                if (guestNetwork.getIp6Cidr() != null) {
                    if (placeholder != null && placeholder.getIp6Address() != null) {
                        logger.debug("Requesting ipv6 address " + placeholder.getIp6Address() + " stored in placeholder nic for the network " + guestNetwork);
                        defaultNetworkStartIpv6 = placeholder.getIp6Address();
                    } else {
                        final String startIpv6 = _networkModel.getStartIpv6Address(guestNetwork.getId());
                        if (startIpv6 != null && _ipv6Dao.findByNetworkIdAndIp(guestNetwork.getId(), startIpv6) == null) {
                            defaultNetworkStartIpv6 = startIpv6;
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("First ipv6 " + startIpv6 + " in network id=" + guestNetwork.getId() +
                                    " is already allocated, can't use it for domain router; will get random ipv6 address from the range");
                        }
                    }
                }
            }

            final NicProfile gatewayNic = new NicProfile(defaultNetworkStartIp, defaultNetworkStartIpv6);
            if (setupPublicNetwork) {
                if (routerDeploymentDefinition.isRedundant()) {
                    gatewayNic.setIp4Address(_ipAddrMgr.acquireGuestIpAddress(guestNetwork, null));
                } else {
                    gatewayNic.setIp4Address(guestNetwork.getGateway());
                }
                gatewayNic.setBroadcastUri(guestNetwork.getBroadcastUri());
                gatewayNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
                gatewayNic.setIsolationUri(guestNetwork.getBroadcastUri());
                gatewayNic.setMode(guestNetwork.getMode());
                final String gatewayCidr = guestNetwork.getCidr();
                gatewayNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
            } else {
                gatewayNic.setDefaultNic(true);
            }

            networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(gatewayNic)));
            hasGuestNetwork = true;
        }

        //2) Control network
        logger.debug("Adding nic for Virtual Router in Control network ");
        List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = offerings.get(0);
        Network controlConfig = _networkMgr.setupNetwork(VirtualNwStatus.account, controlOffering, routerDeploymentDefinition.getPlan(),
                null, null, false).get(0);
        networks.put(controlConfig, new ArrayList<NicProfile>());
        //3) Public network
        if (setupPublicNetwork) {
            final PublicIp sourceNatIp = publicNetwork.second();
            logger.debug("Adding nic for Virtual Router in Public network ");
            //if source nat service is supported by the network, get the source nat ip address
            final NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            defaultNic.setIp4Address(sourceNatIp.getAddress().addr());
            defaultNic.setGateway(sourceNatIp.getGateway());
            defaultNic.setNetmask(sourceNatIp.getNetmask());
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
            if (hasGuestNetwork) {
                defaultNic.setDeviceId(2);
            }
            final NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
            final List<? extends Network> publicNetworks = _networkMgr.setupNetwork(VirtualNwStatus.account, publicOffering,
                    routerDeploymentDefinition.getPlan(), null, null, false);
            final String publicIp = defaultNic.getIp4Address();
            // We want to use the identical MAC address for RvR on public interface if possible
            final NicVO peerNic = _nicDao.findByIp4AddressAndNetworkId(publicIp, publicNetworks.get(0).getId());
            if (peerNic != null) {
                logger.info("Use same MAC as previous RvR, the MAC is " + peerNic.getMacAddress());
                defaultNic.setMacAddress(peerNic.getMacAddress());
            }
            networks.put(publicNetworks.get(0), new ArrayList<NicProfile>(Arrays.asList(defaultNic)));
        }

        return networks;
    }

    /**
     * Originally a NON Vpc specific method
     *
     * @param isPodBased
     * @param dest
     * @param guestNetworkId
     * @return
     */
    protected void planDeploymentRouters(final RouterDeploymentDefinition routerDeploymentDefinition) {
        List<DomainRouterVO> routers = null;
        if (routerDeploymentDefinition.getVpc() != null) {
            routers = vpcHelper.getVpcRouters(routerDeploymentDefinition.getVpc().getId());
        } else if (routerDeploymentDefinition.isBasic()) {
            routers = _routerDao.listByNetworkAndPodAndRole(routerDeploymentDefinition.getGuestNetwork().getId(),
                    routerDeploymentDefinition.getPodId(), Role.VIRTUAL_ROUTER);
        } else {
            routers = _routerDao.listByNetworkAndRole(routerDeploymentDefinition.getGuestNetwork().getId(),
                    Role.VIRTUAL_ROUTER);
        }

        routerDeploymentDefinition.setRouters(routers);
        routerDeploymentDefinition.planDeployment();
    }

    private void checkAndResetPriorityOfRedundantRouter(final List<DomainRouterVO> routers) {
        boolean allStopped = true;
        for (final DomainRouterVO router : routers) {
            if (!router.getIsRedundantRouter() || router.getState() != VirtualMachine.State.Stopped) {
                allStopped = false;
                break;
            }
        }
        if (!allStopped) {
            return;
        }

        for (final DomainRouterVO router : routers) {
            // getUpdatedPriority() would update the value later
            router.setPriority(0);
            router.setIsPriorityBumpUp(false);
            _routerDao.update(router.getId(), router);
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // VPC Specific behavior
    ///////////////////////////////////////////////////////////////////////

    protected List<DomainRouterVO> deployVirtualRouterInVpc(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {

        findOrDeployVirtualRouterInVpc(routerDeploymentDefinition);

        return nwHelper.startRouters(routerDeploymentDefinition);
    }

    @DB
    protected void findOrDeployVirtualRouterInVpc(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        final Vpc vpc = routerDeploymentDefinition.getVpc();
        logger.debug("Deploying Virtual Router in VPC " + vpc);

        Vpc vpcLock = vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }

        //1) Get deployment plan and find out the list of routers
        planDeploymentRouters(routerDeploymentDefinition);

        // The plan & router should have been injected into the routerDeplymentDefinition in the previous method
        //DeploymentPlan plan = planAndRouters.first();
        // List<DomainRouterVO> routers = planAndRouters.second();

        //2) Return routers if exist, otherwise...
        if (routerDeploymentDefinition.getRouters().size() < 1) {
            try {

                Long offeringId = vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
                if (offeringId == null) {
                    offeringId = offering.getId();
                }
                //3) Deploy Virtual Router
                List<? extends PhysicalNetwork> pNtwks = pNtwkDao.listByZone(vpc.getZoneId());

                VirtualRouterProvider vpcVrProvider = null;

                for (PhysicalNetwork pNtwk : pNtwks) {
                    PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(pNtwk.getId(), Type.VPCVirtualRouter.toString());
                    if (provider == null) {
                        throw new CloudRuntimeException("Cannot find service provider " + Type.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
                    }
                    vpcVrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), Type.VPCVirtualRouter);
                    if (vpcVrProvider != null) {
                        break;
                    }
                }

                PublicIp sourceNatIp = vpcMgr.assignSourceNatIpAddressToVpc(routerDeploymentDefinition.getOwner(), vpc);

                DomainRouterVO router = deployVpcRouter(routerDeploymentDefinition, vpcVrProvider, offeringId, sourceNatIp);
                routerDeploymentDefinition.getRouters().add(router);

            } finally {
                // TODO Should we do this after the pre or after the whole??
                if (vpcLock != null) {
                    vpcDao.releaseFromLockTable(vpc.getId());
                }
            }
        }
    }

    protected DomainRouterVO deployVpcRouter(final RouterDeploymentDefinition routerDeploymentDefinition, final VirtualRouterProvider vrProvider,
            final long svcOffId, final PublicIp sourceNatIp) throws ConcurrentOperationException, InsufficientAddressCapacityException,
            InsufficientServerCapacityException, InsufficientCapacityException, StorageUnavailableException, ResourceUnavailableException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = createVpcRouterNetworks(routerDeploymentDefinition,
                new Pair<Boolean, PublicIp>(true, sourceNatIp), routerDeploymentDefinition.getVpc().getId());

        DomainRouterVO router =
                nwHelper.deployRouter(routerDeploymentDefinition, vrProvider, svcOffId, networks, true, vpcMgr.getSupportedVpcHypervisors());

        return router;
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> createVpcRouterNetworks(final RouterDeploymentDefinition routerDeploymentDefinition,
            final Pair<Boolean, PublicIp> sourceNatIp, final long vpcId)
                    throws ConcurrentOperationException, InsufficientAddressCapacityException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(4);

        TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(sourceNatIp.second().getVlanTag());

        //1) allocate nic for control and source nat public ip
        networks = nwHelper.createRouterNetworks(routerDeploymentDefinition, null, sourceNatIp);


        //2) allocate nic for private gateways if needed
        List<PrivateGateway> privateGateways = vpcMgr.getVpcPrivateGateways(vpcId);
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (PrivateGateway privateGateway : privateGateways) {
                NicProfile privateNic = vpcHelper.createPrivateNicProfileForGateway(privateGateway);
                Network privateNetwork = networkModel.getNetwork(privateGateway.getNetworkId());
                networks.put(privateNetwork, new ArrayList<NicProfile>(Arrays.asList(privateNic)));
            }
        }

        //3) allocate nic for guest gateway if needed
        List<? extends Network> guestNetworks = vpcMgr.getVpcNetworks(vpcId);
        for (Network guestNetwork : guestNetworks) {
            if (networkModel.isPrivateGateway(guestNetwork.getId())) {
                continue;
            }
            if (guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup) {
                NicProfile guestNic = createGuestNicProfileForVpcRouter(guestNetwork);
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
            }
        }

        //4) allocate nic for additional public network(s)
        List<IPAddressVO> ips = ipAddressDao.listByAssociatedVpc(vpcId, false);
        List<NicProfile> publicNics = new ArrayList<NicProfile>();
        Network publicNetwork = null;
        for (IPAddressVO ip : ips) {
            PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, vlanDao.findById(ip.getVlanId()));
            if ((ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) && vpcMgr.isIpAllocatedToVpc(ip) &&
                    !publicVlans.contains(publicIp.getVlanTag())) {
                logger.debug("Allocating nic for router in vlan " + publicIp.getVlanTag());
                NicProfile publicNic = new NicProfile();
                publicNic.setDefaultNic(false);
                publicNic.setIp4Address(publicIp.getAddress().addr());
                publicNic.setGateway(publicIp.getGateway());
                publicNic.setNetmask(publicIp.getNetmask());
                publicNic.setMacAddress(publicIp.getMacAddress());
                publicNic.setBroadcastType(BroadcastDomainType.Vlan);
                publicNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(publicIp.getVlanTag()));
                publicNic.setIsolationUri(IsolationType.Vlan.toUri(publicIp.getVlanTag()));
                NetworkOffering publicOffering = networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
                if (publicNetwork == null) {
                    List<? extends Network> publicNetworks = networkMgr.setupNetwork(VirtualNwStatus.account,
                            publicOffering, routerDeploymentDefinition.getPlan(), null, null, false);
                    publicNetwork = publicNetworks.get(0);
                }
                publicNics.add(publicNic);
                publicVlans.add(publicIp.getVlanTag());
            }
        }
        if (publicNetwork != null) {
            if (networks.get(publicNetwork) != null) {
                List<NicProfile> publicNicProfiles = (List<NicProfile>)networks.get(publicNetwork);
                publicNicProfiles.addAll(publicNics);
                networks.put(publicNetwork, publicNicProfiles);
            } else {
                networks.put(publicNetwork, publicNics);
            }
        }

        return networks;
    }

    protected NicProfile createGuestNicProfileForVpcRouter(final Network guestNetwork) {
        NicProfile guestNic = new NicProfile();
        guestNic.setIp4Address(guestNetwork.getGateway());
        guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
        guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
        guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
        guestNic.setMode(guestNetwork.getMode());
        String gatewayCidr = guestNetwork.getCidr();
        guestNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));

        return guestNic;
    }

}
