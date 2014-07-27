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
package org.cloud.network.router.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.router.NetworkGeneralHelper;
import com.cloud.network.router.VirtualNwStatus;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.router.VpcVirtualNetworkHelperImpl;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
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
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class RouterDeploymentDefinition {
    private static final Logger logger = Logger.getLogger(RouterDeploymentDefinition.class);

    protected NetworkDao networkDao;
    protected DomainRouterDao routerDao;
    protected PhysicalNetworkServiceProviderDao physicalProviderDao;
    protected NetworkModel networkModel;
    protected VirtualRouterProviderDao vrProviderDao;
    protected NetworkOfferingDao networkOfferingDao;
    protected IpAddressManager ipAddrMgr;
    protected VMInstanceDao vmDao;
    protected HostPodDao podDao;
    protected AccountManager accountMgr;
    protected NetworkOrchestrationService networkMgr;
    protected NicDao nicDao;
    protected UserIpv6AddressDao ipv6Dao;
    protected IPAddressDao ipAddressDao;
    protected VirtualRouterProvider vrProvider;


    @Inject
    protected NetworkGeneralHelper nwHelper;
    @Inject
    protected VpcVirtualNetworkHelperImpl vpcHelper;


    protected Network guestNetwork;
    protected DeployDestination dest;
    protected Account owner;
    protected Map<Param, Object> params;
    protected boolean isRedundant;
    protected DeploymentPlan plan;
    protected List<DomainRouterVO> routers = new ArrayList<>();
    protected Long offeringId;
    protected Long tableLockId;
    protected boolean publicNetwork;
    protected PublicIp sourceNatIp;

    protected RouterDeploymentDefinition(final Network guestNetwork, final DeployDestination dest,
            final Account owner, final Map<Param, Object> params, final boolean isRedundant) {

        this.guestNetwork = guestNetwork;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public Long getOfferingId() {
        return this.offeringId;
    }

    public Vpc getVpc() {
        return null;
    }
    public Network getGuestNetwork() {
        return guestNetwork;
    }
    public DeployDestination getDest() {
        return dest;
    }
    public Account getOwner() {
        return owner;
    }
    public Map<Param, Object> getParams() {
        return params;
    }
    public boolean isRedundant() {
        return isRedundant;
    }
    public DeploymentPlan getPlan() {
        return plan;
    }
    public boolean isVpcRouter() {
        return false;
    }
    public Pod getPod() {
        return dest.getPod();
    }
    public Long getPodId() {
        return dest.getPod() == null ? null : dest.getPod().getId();
    }
    public List<DomainRouterVO> getRouters() {
        return routers;
    }

    public VirtualRouterProvider getVirtualProvider() {
        return this.vrProvider;
    }

    public boolean isBasic() {
        return this.dest.getDataCenter().getNetworkType() == NetworkType.Basic;
    }

    protected void generateDeploymentPlan() {
        final long dcId = this.dest.getDataCenter().getId();
        Long podId = null;
        if (this.isBasic()) {
            if (this.dest.getPod() == null) {
                throw new CloudRuntimeException("Pod id is expected in deployment destination");
            }
            podId = this.dest.getPod().getId();
        }
        this.plan = new DataCenterDeployment(dcId, podId, null, null, null, null);
    }

    public List<DomainRouterVO> deployVirtualRouter()
            throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {

        this.findOrDeployVirtualRouter();

        return nwHelper.startRouters(this);
    }

    @DB
    protected void findOrDeployVirtualRouter()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        try {
            this.lock();
            this.checkPreconditions();
            // dest has pod=null, for Basic Zone findOrDeployVRs for all Pods
            final List<DeployDestination> destinations = findDestinations();

            for (final DeployDestination destination : destinations) {
                this.dest = destination;
                this.planDeploymentRouters();
                this.generateDeploymentPlan();
                this.executeDeployment();
            }
        } finally {
            this.unlock();
        }
    }

    protected void lock() {
        final Network lock = networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }
        this.tableLockId = lock.getId();
    }

    protected void unlock() {
        if (this.tableLockId != null) {
            networkDao.releaseFromLockTable(this.tableLockId);
            if (logger.isDebugEnabled()) {
                logger.debug("Lock is released for network id " + this.tableLockId
                        + " as a part of router startup in " + dest);
            }
        }
    }

    protected void checkPreconditions() throws ResourceUnavailableException {
        if (guestNetwork.getState() != Network.State.Implemented &&
                guestNetwork.getState() != Network.State.Setup &&
                guestNetwork.getState() != Network.State.Implementing) {
            throw new ResourceUnavailableException("Network is not yet fully implemented: " + guestNetwork,
                    Network.class, this.guestNetwork.getId());
        }

        if (guestNetwork.getTrafficType() != TrafficType.Guest) {
            throw new ResourceUnavailableException("Network is not type Guest as expected: " + guestNetwork,
                    Network.class, this.guestNetwork.getId());
        }
    }

    protected List<DeployDestination> findDestinations() {
        // dest has pod=null, for Basic Zone findOrDeployVRs for all Pods
        final List<DeployDestination> destinations = new ArrayList<DeployDestination>();

        // for basic zone, if 'dest' has pod set to null then this is network restart scenario otherwise it is a vm deployment scenario
        if (this.isBasic() && dest.getPod() == null) {
            // Find all pods in the data center with running or starting user vms
            final long dcId = dest.getDataCenter().getId();
            final List<HostPodVO> pods = listByDataCenterIdVMTypeAndStates(dcId, VirtualMachine.Type.User, VirtualMachine.State.Starting, VirtualMachine.State.Running);

            // Loop through all the pods skip those with running or starting VRs
            for (final HostPodVO pod : pods) {
                // Get list of VRs in starting or running state
                final long podId = pod.getId();
                final List<DomainRouterVO> virtualRouters = routerDao.listByPodIdAndStates(podId, VirtualMachine.State.Starting, VirtualMachine.State.Running);

                if (virtualRouters.size() > 1) {
                    // FIXME Find or create a better and more specific exception for this
                    throw new CloudRuntimeException("Pod can have utmost one VR in Basic Zone, please check!");
                }

                // Add virtualRouters to the routers, this avoids the situation when
                // all routers are skipped and VirtualRouterElement throws exception
                this.routers.addAll(virtualRouters);

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
        return destinations;
    }

    protected int getNumberOfRoutersToDeploy() {
        // TODO Are we sure this makes sense? Somebody said 5 was too many?
        if (this.routers.size() >= 5) {
            logger.error("Too many redundant routers!");
        }

        // If old network is redundant but new is single router, then routers.size() = 2 but routerCount = 1
        int routersExpected = 1;
        if (this.isRedundant) {
            routersExpected = 2;
        }
        return routersExpected < this.routers.size() ?
                0 : routersExpected - this.routers.size();
    }

    /**
     * Executes preparation and deployment of the routers. After this method ends, {@link this#routers}
     * should have all of the deployed routers ready for start, and no more.
     *
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     */
    protected void executeDeployment()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        //Check current redundant routers, if possible(all routers are stopped), reset the priority
        // TODO Why shouldn't we call this method also for VPC
        setupPriorityOfRedundantRouter();

        if (this.getNumberOfRoutersToDeploy() > 0) {
            if (networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
                this.owner = accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
            }

            // Check if public network has to be set on VR
            this.publicNetwork = networkModel.isProviderSupportServiceInNetwork(
                            guestNetwork.getId(), Service.SourceNat, Provider.VirtualRouter);

            if (this.isRedundant && !this.publicNetwork) {
                // TODO Shouldn't be this throw an exception instead of log error and empty list of routers
                logger.error("Didn't support redundant virtual router without public network!");
                this.routers = new ArrayList<>();
            } else {
                this.findVirtualProvider();
                this.findOfferingId();
                this.findSourceNatIP();
                this.deployAllVirtualRouters();
            }
        }
    }

    protected void findSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        this.sourceNatIp = null;
        if (this.publicNetwork) {
            this.sourceNatIp = this.ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                    this.owner,this.guestNetwork);
        }
    }

    protected void findOfferingId() {
        Long networkOfferingId = networkOfferingDao.findById(guestNetwork.getNetworkOfferingId()).getServiceOfferingId();
        if (networkOfferingId != null) {
            this.offeringId = networkOfferingId;
        }
    }

    protected void findVirtualProvider() {
        // Check if providers are supported in the physical networks
        final Type type = Type.VirtualRouter;
        final Long physicalNetworkId = networkModel.getPhysicalNetworkId(guestNetwork);
        final PhysicalNetworkServiceProvider provider =
                physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());

        if (provider == null) {
            throw new CloudRuntimeException(
                    String.format("Cannot find service provider %s  in physical network %s",
                            type.toString(), physicalNetworkId));
        }

        this.vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), type);
        if (this.vrProvider == null) {
            throw new CloudRuntimeException(
                    String.format("Cannot find virtual router provider %s as service provider %s",
                            type.toString(), provider.getId()));
        }
    }

    protected void deployAllVirtualRouters()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        int routersToDeploy = this.getNumberOfRoutersToDeploy();
        for(int i = 0; i < routersToDeploy; i++) {
            LinkedHashMap<Network, List<? extends NicProfile>> networks =
                    createRouterNetworks();
            //don't start the router as we are holding the network lock that needs to be released at the end of router allocation
            DomainRouterVO router = nwHelper.deployRouter(this, networks, false, null);

            if (router != null) {
                this.routerDao.addRouterToGuestNetwork(router, this.guestNetwork);
                this.routers.add(router);
            }
        }
    }

    /**
     * Lists all pods given a Data Center Id, a {@link VirtualMachine.Type} and a list of
     * {@link VirtualMachine.State}
     *
     * @param id
     * @param type
     * @param states
     * @return
     */
    protected List<HostPodVO> listByDataCenterIdVMTypeAndStates(final long id, final VirtualMachine.Type type, final VirtualMachine.State... states) {
        final SearchBuilder<VMInstanceVO> vmInstanceSearch = vmDao.createSearchBuilder();
        vmInstanceSearch.and("type", vmInstanceSearch.entity().getType(), SearchCriteria.Op.EQ);
        vmInstanceSearch.and("states", vmInstanceSearch.entity().getState(), SearchCriteria.Op.IN);

        final SearchBuilder<HostPodVO> podIdSearch = podDao.createSearchBuilder();
        podIdSearch.and("dc", podIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        podIdSearch.select(null, SearchCriteria.Func.DISTINCT, podIdSearch.entity().getId());
        podIdSearch.join("vmInstanceSearch", vmInstanceSearch, podIdSearch.entity().getId(), vmInstanceSearch.entity().getPodIdToDeployIn(), JoinBuilder.JoinType.INNER);
        podIdSearch.done();

        final SearchCriteria<HostPodVO> sc = podIdSearch.create();
        sc.setParameters("dc", id);
        sc.setJoinParameters("vmInstanceSearch", "type", type);
        sc.setJoinParameters("vmInstanceSearch", "states", (Object[])states);
        return podDao.search(sc, null);
    }

    protected void planDeploymentRouters() {
        if (this.isBasic()) {
            this.routers = routerDao.listByNetworkAndPodAndRole(this.guestNetwork.getId(),
                    this.getPodId(), Role.VIRTUAL_ROUTER);
        } else {
            this.routers = routerDao.listByNetworkAndRole(this.guestNetwork.getId(),
                    Role.VIRTUAL_ROUTER);
        }
    }

    /**
     * Routers need reset if at least one of the routers is not redundant or stopped.
     *
     * @return
     */
    protected boolean routersNeedReset() {
        boolean needReset = true;
        for (final DomainRouterVO router : this.routers) {
            if (!router.getIsRedundantRouter() || router.getState() != VirtualMachine.State.Stopped) {
                needReset = false;
                break;
            }
        }

        return needReset;
    }

    /**
     * Only for redundant deployment and if any routers needed reset, we shall reset all
     * routers priorities
     */
    protected void setupPriorityOfRedundantRouter() {
            if (this.isRedundant && this.routersNeedReset()) {
                for (final DomainRouterVO router : this.routers) {
                    // getUpdatedPriority() would update the value later
                    router.setPriority(0);
                    router.setIsPriorityBumpUp(false);
                    routerDao.update(router.getId(), router);
                }
            }
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks()
                    throws ConcurrentOperationException, InsufficientAddressCapacityException {

        //Form networks
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);
        //1) Guest network
        boolean hasGuestNetwork = false;
        if (this.guestNetwork != null) {
            logger.debug("Adding nic for Virtual Router in Guest network " + this.guestNetwork);
            String defaultNetworkStartIp = null, defaultNetworkStartIpv6 = null;
            if (!this.publicNetwork) {
                final Nic placeholder = networkModel.getPlaceholderNicForRouter(this.guestNetwork, this.getPodId());
                if (this.guestNetwork.getCidr() != null) {
                    if (placeholder != null && placeholder.getIp4Address() != null) {
                        logger.debug("Requesting ipv4 address " + placeholder.getIp4Address() + " stored in placeholder nic for the network " + this.guestNetwork);
                        defaultNetworkStartIp = placeholder.getIp4Address();
                    } else {
                        final String startIp = networkModel.getStartIpAddress(this.guestNetwork.getId());
                        if (startIp != null && ipAddressDao.findByIpAndSourceNetworkId(this.guestNetwork.getId(), startIp).getAllocatedTime() == null) {
                            defaultNetworkStartIp = startIp;
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("First ipv4 " + startIp + " in network id=" + this.guestNetwork.getId() +
                                    " is already allocated, can't use it for domain router; will get random ip address from the range");
                        }
                    }
                }

                if (this.guestNetwork.getIp6Cidr() != null) {
                    if (placeholder != null && placeholder.getIp6Address() != null) {
                        logger.debug("Requesting ipv6 address " + placeholder.getIp6Address() + " stored in placeholder nic for the network " + this.guestNetwork);
                        defaultNetworkStartIpv6 = placeholder.getIp6Address();
                    } else {
                        final String startIpv6 = networkModel.getStartIpv6Address(this.guestNetwork.getId());
                        if (startIpv6 != null && ipv6Dao.findByNetworkIdAndIp(this.guestNetwork.getId(), startIpv6) == null) {
                            defaultNetworkStartIpv6 = startIpv6;
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("First ipv6 " + startIpv6 + " in network id=" + this.guestNetwork.getId() +
                                    " is already allocated, can't use it for domain router; will get random ipv6 address from the range");
                        }
                    }
                }
            }

            final NicProfile gatewayNic = new NicProfile(defaultNetworkStartIp, defaultNetworkStartIpv6);
            if (this.publicNetwork) {
                if (this.isRedundant) {
                    gatewayNic.setIp4Address(ipAddrMgr.acquireGuestIpAddress(this.guestNetwork, null));
                } else {
                    gatewayNic.setIp4Address(this.guestNetwork.getGateway());
                }
                gatewayNic.setBroadcastUri(this.guestNetwork.getBroadcastUri());
                gatewayNic.setBroadcastType(this.guestNetwork.getBroadcastDomainType());
                gatewayNic.setIsolationUri(this.guestNetwork.getBroadcastUri());
                gatewayNic.setMode(this.guestNetwork.getMode());
                final String gatewayCidr = this.guestNetwork.getCidr();
                gatewayNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
            } else {
                gatewayNic.setDefaultNic(true);
            }

            networks.put(this.guestNetwork, new ArrayList<NicProfile>(Arrays.asList(gatewayNic)));
            hasGuestNetwork = true;
        }

        //2) Control network
        logger.debug("Adding nic for Virtual Router in Control network ");
        List<? extends NetworkOffering> offerings = networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = offerings.get(0);
        Network controlConfig = networkMgr.setupNetwork(VirtualNwStatus.account, controlOffering, this.plan,
                null, null, false).get(0);
        networks.put(controlConfig, new ArrayList<NicProfile>());
        //3) Public network
        if (publicNetwork) {
            logger.debug("Adding nic for Virtual Router in Public network ");
            //if source nat service is supported by the network, get the source nat ip address
            final NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            defaultNic.setIp4Address(this.sourceNatIp.getAddress().addr());
            defaultNic.setGateway(this.sourceNatIp.getGateway());
            defaultNic.setNetmask(this.sourceNatIp.getNetmask());
            defaultNic.setMacAddress(this.sourceNatIp.getMacAddress());
            // get broadcast from public network
            final Network pubNet = networkDao.findById(sourceNatIp.getNetworkId());
            if (pubNet.getBroadcastDomainType() == BroadcastDomainType.Vxlan) {
                defaultNic.setBroadcastType(BroadcastDomainType.Vxlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vxlan.toUri(this.sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(BroadcastDomainType.Vxlan.toUri(this.sourceNatIp.getVlanTag()));
            } else {
                defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(this.sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(IsolationType.Vlan.toUri(this.sourceNatIp.getVlanTag()));
            }
            if (hasGuestNetwork) {
                defaultNic.setDeviceId(2);
            }
            final NetworkOffering publicOffering = networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
            final List<? extends Network> publicNetworks = networkMgr.setupNetwork(VirtualNwStatus.account, publicOffering,
                    this.plan, null, null, false);
            final String publicIp = defaultNic.getIp4Address();
            // We want to use the identical MAC address for RvR on public interface if possible
            final NicVO peerNic = nicDao.findByIp4AddressAndNetworkId(publicIp, publicNetworks.get(0).getId());
            if (peerNic != null) {
                logger.info("Use same MAC as previous RvR, the MAC is " + peerNic.getMacAddress());
                defaultNic.setMacAddress(peerNic.getMacAddress());
            }
            networks.put(publicNetworks.get(0), new ArrayList<NicProfile>(Arrays.asList(defaultNic)));
        }

        return networks;
    }

}