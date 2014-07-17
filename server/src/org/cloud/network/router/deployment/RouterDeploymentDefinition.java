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
    protected ServiceOfferingVO offering;




    protected RouterDeploymentDefinition(final Network guestNetwork, final DeployDestination dest,
            final Account owner, final Map<Param, Object> params, final boolean isRedundant) {

        this.guestNetwork = guestNetwork;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public void setOffering(ServiceOfferingVO offering) {
        this.offering = offering;
    }
    public Vpc getVpc() {
        return null;
    }
    public Network getGuestNetwork() {
        return guestNetwork;
    }
    public void setGuestNetwork(final Network guestNetwork) {
        this.guestNetwork = guestNetwork;
    }
    public DeployDestination getDest() {
        return dest;
    }
    public void setDest(final DeployDestination dest) {
        this.dest = dest;
    }
    public Account getOwner() {
        return owner;
    }
    public void setOwner(final Account owner) {
        this.owner = owner;
    }
    public Map<Param, Object> getParams() {
        return params;
    }
    public void setParams(final Map<Param, Object> params) {
        this.params = params;
    }
    public boolean isRedundant() {
        return isRedundant;
    }
    public void setRedundant(final boolean isRedundant) {
        this.isRedundant = isRedundant;
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
    public void setRouters(List<DomainRouterVO> routers) {
        this.routers = routers;
    }

    public boolean isBasic() {
        return this.dest.getDataCenter().getNetworkType() == NetworkType.Basic;
    }

    public Long getOfferingId() {
        return offering == null ? null : offering.getId();
    }

    public void generateDeploymentPlan() {
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

        findOrDeployVirtualRouter();

        return nwHelper.startRouters(this);
    }

    @DB
    protected void findOrDeployVirtualRouter()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        final Network lock = networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
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
            if (this.isBasic() && dest.getPod() == null) {
                // Find all pods in the data center with running or starting user vms
                final long dcId = dest.getDataCenter().getId();
                final List<HostPodVO> pods = listByDataCenterIdVMTypeAndStates(dcId, VirtualMachine.Type.User, VirtualMachine.State.Starting, VirtualMachine.State.Running);

                // Loop through all the pods skip those with running or starting VRs
                for (final HostPodVO pod : pods) {
                    // Get list of VRs in starting or running state
                    final long podId = pod.getId();
                    final List<DomainRouterVO> virtualRouters = routerDao.listByPodIdAndStates(podId, VirtualMachine.State.Starting, VirtualMachine.State.Running);

                    assert (virtualRouters.size() <= 1) : "Pod can have utmost one VR in Basic Zone, please check!";

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

            // Except for Basic Zone, the for loop will iterate only once
            for (final DeployDestination destination : destinations) {
                this.dest = destination;
                planDeploymentRouters();
                this.generateDeploymentPlan();

                // 2) Figure out required routers count
                int routerCount = 1;
                if (this.isRedundant) {
                    routerCount = 2;
                    //Check current redundant routers, if possible(all routers are stopped), reset the priority
                    if (this.routers.size() != 0) {
                        checkAndResetPriorityOfRedundantRouter(this.routers);
                    }
                }

                // If old network is redundant but new is single router, then routers.size() = 2 but routerCount = 1
                if (this.routers.size() >= routerCount) {
                    return;
                }

                if (this.routers.size() >= 5) {
                    logger.error("Too much redundant routers!");
                }

                // Check if providers are supported in the physical networks
                final Type type = Type.VirtualRouter;
                final Long physicalNetworkId = networkModel.getPhysicalNetworkId(guestNetwork);
                final PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + type.toString() + " in physical network " + physicalNetworkId);
                }
                final VirtualRouterProvider vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), type);
                if (vrProvider == null) {
                    throw new CloudRuntimeException("Cannot find virtual router provider " + type.toString() + " as service provider " + provider.getId());
                }

                if (networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
                    this.owner = accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
                }

                // Check if public network has to be set on VR
                boolean publicNetwork = false;
                if (networkModel.isProviderSupportServiceInNetwork(guestNetwork.getId(), Service.SourceNat, Provider.VirtualRouter)) {
                    publicNetwork = true;
                }
                if (this.isRedundant && !publicNetwork) {
                    logger.error("Didn't support redundant virtual router without public network!");
                    this.routers = null;
                    return;
                }

                Long offeringId = networkOfferingDao.findById(guestNetwork.getNetworkOfferingId()).getServiceOfferingId();
                if (offeringId == null) {
                    offeringId = getOfferingId();
                }

                PublicIp sourceNatIp = null;
                if (publicNetwork) {
                    sourceNatIp = ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                            this.owner, guestNetwork);
                }

                // 3) deploy virtual router(s)
                final int count = routerCount - this.routers.size();
                for (int i = 0; i < count; i++) {
                    LinkedHashMap<Network, List<? extends NicProfile>> networks =
                            createRouterNetworks(new Pair<Boolean, PublicIp>(
                            publicNetwork, sourceNatIp));
                    //don't start the router as we are holding the network lock that needs to be released at the end of router allocation
                    DomainRouterVO router = nwHelper.deployRouter(this, vrProvider, offeringId, networks, false, null);

                    if (router != null) {
                        routerDao.addRouterToGuestNetwork(router, guestNetwork);
                        this.routers.add(router);
                    }
                }
            }
        } finally {
            if (lock != null) {
                networkDao.releaseFromLockTable(lock.getId());
                if (logger.isDebugEnabled()) {
                    logger.debug("Lock is released for network id " + lock.getId() + " as a part of router startup in " + dest);
                }
            }
        }
    }


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
            routerDao.update(router.getId(), router);
        }
    }
    protected LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks(
            final Pair<Boolean, PublicIp> publicNetwork)
                    throws ConcurrentOperationException, InsufficientAddressCapacityException {

        final Network guestNetwork = this.guestNetwork;
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
                final Nic placeholder = networkModel.getPlaceholderNicForRouter(guestNetwork, this.getPodId());
                if (guestNetwork.getCidr() != null) {
                    if (placeholder != null && placeholder.getIp4Address() != null) {
                        logger.debug("Requesting ipv4 address " + placeholder.getIp4Address() + " stored in placeholder nic for the network " + guestNetwork);
                        defaultNetworkStartIp = placeholder.getIp4Address();
                    } else {
                        final String startIp = networkModel.getStartIpAddress(guestNetwork.getId());
                        if (startIp != null && ipAddressDao.findByIpAndSourceNetworkId(guestNetwork.getId(), startIp).getAllocatedTime() == null) {
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
                        final String startIpv6 = networkModel.getStartIpv6Address(guestNetwork.getId());
                        if (startIpv6 != null && ipv6Dao.findByNetworkIdAndIp(guestNetwork.getId(), startIpv6) == null) {
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
                if (this.isRedundant) {
                    gatewayNic.setIp4Address(ipAddrMgr.acquireGuestIpAddress(guestNetwork, null));
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
        List<? extends NetworkOffering> offerings = networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = offerings.get(0);
        Network controlConfig = networkMgr.setupNetwork(VirtualNwStatus.account, controlOffering, this.plan,
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
            final Network pubNet = networkDao.findById(sourceNatIp.getNetworkId());
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