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
package org.apache.cloudstack.network.router.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationManagerImpl;
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
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class RouterDeploymentDefinition {
    private static final Logger logger = Logger.getLogger(RouterDeploymentDefinition.class);

    protected static final int LIMIT_NUMBER_OF_ROUTERS = 5;
    protected static final int MAX_NUMBER_OF_ROUTERS = 2;

    protected NetworkDao networkDao;
    protected DomainRouterDao routerDao;
    protected PhysicalNetworkServiceProviderDao physicalProviderDao;
    protected NetworkModel networkModel;
    protected VirtualRouterProviderDao vrProviderDao;
    protected NetworkOfferingDao networkOfferingDao;
    protected ServiceOfferingDao serviceOfferingDao;
    protected DiskOfferingDao diskOfferingDao;
    protected IpAddressManager ipAddrMgr;
    protected VMInstanceDao vmDao;
    protected HostPodDao podDao;
    protected AccountManager accountMgr;
    protected NetworkOrchestrationService networkMgr;
    protected NicDao nicDao;
    protected UserIpv6AddressDao ipv6Dao;
    protected IPAddressDao ipAddressDao;
    protected VirtualRouterProvider vrProvider;
    protected NetworkHelper nwHelper;

    protected Network guestNetwork;
    protected DeployDestination dest;
    protected Account owner;
    protected Map<Param, Object> params;
    protected DeploymentPlan plan;
    protected List<DomainRouterVO> routers = new ArrayList<>();
    protected Long serviceOfferingId;
    protected Long tableLockId;
    protected boolean isPublicNetwork;
    protected PublicIp sourceNatIp;
    protected NetworkDetailsDao networkDetailsDao;

    protected RouterDeploymentDefinition(final Network guestNetwork, final DeployDestination dest,
            final Account owner, final Map<Param, Object> params) {

        this.guestNetwork = guestNetwork;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
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
        return guestNetwork.isRedundant();
    }

    public boolean isRollingRestart() {
        return guestNetwork.isRollingRestart();
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
        return vrProvider;
    }

    public boolean isBasic() {
        return dest.getDataCenter().getNetworkType() == NetworkType.Basic;
    }

    public boolean isPublicNetwork() {
        return isPublicNetwork;
    }

    public PublicIp getSourceNatIP() {
        return sourceNatIp;
    }

    protected void generateDeploymentPlan() {
        final long dcId = dest.getDataCenter().getId();
        Long podId = null;
        if (isBasic()) {
            if (dest.getPod() == null) {
                throw new CloudRuntimeException("Pod id is expected in deployment destination");
            }
            podId = dest.getPod().getId();
        }
        plan = new DataCenterDeployment(dcId, podId, null, null, null, null);
    }

    public List<DomainRouterVO> deployVirtualRouter() throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        findOrDeployVirtualRouter();
        return nwHelper.startRouters(this);
    }

    private boolean isRouterDeployed() throws ResourceUnavailableException {
        boolean isDeployed = true;
        checkPreconditions();
        final List<DeployDestination> destinations = findDestinations();
        for (final DeployDestination destination : destinations) {
            dest = destination;
            generateDeploymentPlan();
            planDeploymentRouters();
            if (getNumberOfRoutersToDeploy() > 0 && prepareDeployment()) {
                isDeployed = false;
                break;
            }
        }
        return isDeployed;
    }

    @DB
    protected void findOrDeployVirtualRouter() throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!isRouterDeployed()) {
            try {
                lock();
                // reset router list that got populated during isRouterDeployed()
                routers.clear();
                checkPreconditions();

                // dest has pod=null, for Basic Zone findOrDeployVRs for all Pods
                final List<DeployDestination> destinations = findDestinations();
                for (final DeployDestination destination : destinations) {
                    dest = destination;
                    generateDeploymentPlan();
                    executeDeployment();
                }
            } finally {
                unlock();
            }
        }
    }

    protected void lock() {
        final Network lock = networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }
        tableLockId = lock.getId();
    }

    protected void unlock() {
        if (tableLockId != null) {
            networkDao.releaseFromLockTable(tableLockId);
            if (logger.isDebugEnabled()) {
                logger.debug("Lock is released for network id " + tableLockId + " as a part of router startup in " + dest);
            }
        }
    }

    protected void checkPreconditions() throws ResourceUnavailableException {
        if (guestNetwork.getState() != Network.State.Implemented && guestNetwork.getState() != Network.State.Setup && guestNetwork.getState() != Network.State.Implementing) {
            throw new ResourceUnavailableException("Network is not yet fully implemented: " + guestNetwork, Network.class, guestNetwork.getId());
        }

        if (guestNetwork.getTrafficType() != TrafficType.Guest) {
            throw new ResourceUnavailableException("Network is not type Guest as expected: " + guestNetwork, Network.class, guestNetwork.getId());
        }
    }

    protected List<DeployDestination> findDestinations() {
        // dest has pod=null, for Basic Zone findOrDeployVRs for all Pods
        final List<DeployDestination> destinations = new ArrayList<DeployDestination>();

        // for basic zone, if 'dest' has pod set to null then this is network
        // restart scenario otherwise it is a vm deployment scenario
        if (isBasic() && dest.getPod() == null) {
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
        return destinations;
    }

    protected int getNumberOfRoutersToDeploy() {
        // TODO Are we sure this makes sense? Somebody said 5 was too many?
        if (routers.size() >= LIMIT_NUMBER_OF_ROUTERS) {
            logger.error("Too many redundant routers!");
        }

        // If old network is redundant but new is single router, then
        // routers.size() = 2 but routerCount = 1
        int routersExpected = 1;
        if (isRedundant() || isRollingRestart()) {
            routersExpected = 2;
        }
        return routersExpected < routers.size() ? 0 : routersExpected - routers.size();
    }

    protected void setupAccountOwner() {
        if (networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
            owner = accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
        }
    }

    /**
     * It executes last pending tasks to prepare the deployment and checks the
     * deployment can proceed. If it can't it return false
     *
     * @return if the deployment can proceed
     */
    protected boolean prepareDeployment() {
        setupAccountOwner();

        // Check if public network has to be set on VR
        isPublicNetwork = networkModel.isProviderSupportServiceInNetwork(guestNetwork.getId(), Service.SourceNat, Provider.VirtualRouter);

        boolean canProceed = true;
        if (isRedundant() && !isPublicNetwork) {
            // TODO Shouldn't be this throw an exception instead of log error and empty list of routers
            logger.error("Didn't support redundant virtual router without public network!");
            routers = new ArrayList<DomainRouterVO>();
            canProceed = false;
        }

        return canProceed;
    }

    /**
     * Executes preparation and deployment of the routers. After this method
     * ends, {@link this#routers} should have all of the deployed routers ready
     * for start, and no more.
     *
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     */
    protected void executeDeployment()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        //Check current redundant routers, if possible(all routers are stopped), reset the priority
        planDeploymentRouters();

        if (getNumberOfRoutersToDeploy() > 0 && prepareDeployment()) {
            findVirtualProvider();
            findServiceOfferingId();
            findSourceNatIP();
            deployAllVirtualRouters();
        }
    }

    protected void findSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        sourceNatIp = null;
        if (isPublicNetwork) {
            sourceNatIp = ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(owner, guestNetwork);
        }
    }

    protected void findDefaultServiceOfferingId() {
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findDefaultSystemOffering(ServiceOffering.routerDefaultOffUniqueName, ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dest.getDataCenter().getId()));
        serviceOfferingId = serviceOffering.getId();
    }

    protected void findAccountServiceOfferingId(long accountId) {
        String accountRouterOffering = VirtualNetworkApplianceManager.VirtualRouterServiceOffering.valueIn(accountId);
        String globalRouterOffering = VirtualNetworkApplianceManager.VirtualRouterServiceOffering.value();
        if (accountRouterOffering != null) {
            verifyServiceOfferingByUuid(accountRouterOffering);
        }
        if (serviceOfferingId == null && globalRouterOffering != accountRouterOffering) {
            verifyServiceOfferingByUuid(globalRouterOffering);
        }
    }

    private void verifyServiceOfferingByUuid(String offeringUuid) {
        logger.debug("Verifying router service offering with uuid : " + offeringUuid);
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findByUuid(offeringUuid);
        if (serviceOffering != null && serviceOffering.isSystemUse() && ServiceOffering.State.Active.equals(serviceOffering.getState())) {
            DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());
            boolean isLocalStorage = ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dest.getDataCenter().getId());
            if (isLocalStorage == diskOffering.isUseLocalStorage()) {
                logger.debug(String.format("Service offering %s (uuid: %s) will be used on virtual router", serviceOffering.getName(), serviceOffering.getUuid()));
                serviceOfferingId = serviceOffering.getId();
            }
        }
    }

    protected void findNetworkServiceOfferingId() {
        String networkRouterOffering = VirtualNetworkApplianceManager.NetworkVirtualRouterServiceOffering.valueIn(guestNetwork.getId());
        if (networkRouterOffering != null) {
            ServiceOfferingVO serviceOffering = serviceOfferingDao.findByUuid(networkRouterOffering);
            if (serviceOffering != null && "domainrouter".equals(serviceOffering.getSystemVmType())) {
                boolean isLocalStorage = ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dest.getDataCenter().getId());
                DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());
                if (isLocalStorage == diskOffering.isUseLocalStorage()) {
                    serviceOfferingId = serviceOffering.getId();
                }
            }
        }
    }

    protected void findServiceOfferingId() {
        serviceOfferingId = networkOfferingDao.findById(guestNetwork.getNetworkOfferingId()).getServiceOfferingId();
        if (serviceOfferingId == null) {
            findNetworkServiceOfferingId();
        }
        if (serviceOfferingId == null) {
            findAccountServiceOfferingId(guestNetwork.getAccountId());
        }
        if (serviceOfferingId == null) {
            findDefaultServiceOfferingId();
        }
    }

    protected void findVirtualProvider() {
        // Check if providers are supported in the physical networks
        final Type type = Type.VirtualRouter;
        final Long physicalNetworkId = networkModel.getPhysicalNetworkId(guestNetwork);
        final PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());

        if (provider == null) {
            throw new CloudRuntimeException(String.format("Cannot find service provider %s in physical network %s", type.toString(), physicalNetworkId));
        }

        vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), type);
        if (vrProvider == null) {
            throw new CloudRuntimeException(String.format("Cannot find virtual router provider %s as service provider %s", type.toString(), provider.getId()));
        }
    }

    protected void deployAllVirtualRouters() throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        final int routersToDeploy = getNumberOfRoutersToDeploy();
        for (int i = 0; i < routersToDeploy; i++) {
            // Don't start the router as we are holding the network lock that
            // needs to be released at the end of router allocation
            final DomainRouterVO router = nwHelper.deployRouter(this, false);
            //check if the network update is in progress.
            //if update is in progress add the update_pending flag to DomainRouterVO.
            NetworkDetailVO detail = networkDetailsDao.findDetail(guestNetwork.getId(),Network.updatingInSequence);
            if("true".equalsIgnoreCase(detail!=null ? detail.getValue() : null)) {
                router.setUpdateState(VirtualRouter.UpdateState.UPDATE_IN_PROGRESS);
                routerDao.persist(router);
            }
            if (router != null) {
                routerDao.addRouterToGuestNetwork(router, guestNetwork);
                //Fix according to changes by Sheng Yang in commit ID cb4513379996b262ae378daf00c6388c6b7313cf
                routers.add(router);
            }
        }
    }

    /**
     * Lists all pods given a Data Center Id, a {@link VirtualMachine.Type} and
     * a list of {@link VirtualMachine.State}
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
        sc.setJoinParameters("vmInstanceSearch", "states", (Object[]) states);
        return podDao.search(sc, null);
    }

    protected void planDeploymentRouters() {
        if (isBasic()) {
            routers.addAll(routerDao.listByNetworkAndPodAndRole(guestNetwork.getId(), getPodId(), Role.VIRTUAL_ROUTER));
        } else {
            routers.addAll(routerDao.listByNetworkAndRole(guestNetwork.getId(), Role.VIRTUAL_ROUTER));
        }
    }

    /**
     * Routers need reset if at least one of the routers is not redundant or
     * stopped.
     */
    protected boolean routersNeedReset() {
        boolean needReset = true;
        for (final DomainRouterVO router : routers) {
            if (!router.getIsRedundantRouter() || router.getState() != VirtualMachine.State.Stopped) {
                needReset = false;
                break;
            }
        }

        return needReset;
    }
}
