package org.cloud.network.router.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.router.VirtualNwStatus;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

public class VpcRouterDeploymentDefinition extends RouterDeploymentDefinition {
    private static final Logger logger = Logger.getLogger(VpcRouterDeploymentDefinition.class);

    protected VpcDao vpcDao;
    protected VpcOfferingDao vpcOffDao;
    protected PhysicalNetworkDao pNtwkDao;
    protected VpcManager vpcMgr;
    protected VlanDao vlanDao;

    protected Vpc vpc;


    protected VpcRouterDeploymentDefinition(final Vpc vpc, final DeployDestination dest, final Account owner,
            final Map<Param, Object> params, final boolean isRedundant) {

        super(null, dest, owner, params, isRedundant);

        this.vpc = vpc;
    }

    @Override
    public Vpc getVpc() {
        return this.vpc;
    }

    @Override
    public boolean isVpcRouter() {
        return true;
    }

    @Override
    protected void lock() {
        Vpc vpcLock = vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }
        this.tableLockId = vpcLock.getId();
    }

    @Override
    protected void unlock() {
        if (this.tableLockId != null) {
            vpcDao.releaseFromLockTable(this.tableLockId);
            if (logger.isDebugEnabled()) {
                logger.debug("Lock is released for vpc id " + this.tableLockId
                        + " as a part of router startup in " + dest);
            }
        }
    }

    @Override
    protected void checkPreconditions() {
        // No preconditions for Vpc
    }

    @Override
    protected List<DeployDestination> findDestinations() {
        final List<DeployDestination> destinations = new ArrayList<>();
        destinations.add(this.dest);
        return destinations;
    }

    @Override
    protected int getNumberOfRoutersToDeploy() {
        // TODO Should we make our changes here in order to enable Redundant Router for VPC?
        return 1 - this.routers.size();
    }

    /**
     * @see RouterDeploymentDefinition#executeDeployment()
     */
    @Override
    protected void executeDeployment()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        //2) Return routers if exist, otherwise...
        if (getNumberOfRoutersToDeploy() > 0) {
            this.findVirtualProvider();
            this.findOfferingId();
            this.findSourceNatIP();

            //3) Deploy Virtual Router
            DomainRouterVO router = deployVpcRouter(sourceNatIp);
            this.routers.add(router);
        }
    }

    @Override
    protected void findSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        this.sourceNatIp = vpcMgr.assignSourceNatIpAddressToVpc(this.owner, vpc);
    }

    @Override
    protected void findVirtualProvider() {
        List<? extends PhysicalNetwork> pNtwks = pNtwkDao.listByZone(vpc.getZoneId());

        this.vrProvider = null;

        for (PhysicalNetwork pNtwk : pNtwks) {
            PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(pNtwk.getId(), Type.VPCVirtualRouter.toString());
            if (provider == null) {
                throw new CloudRuntimeException("Cannot find service provider " + Type.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
            }
            this.vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), Type.VPCVirtualRouter);
            if (this.vrProvider != null) {
                break;
            }
        }
    }

    @Override
    protected void findOfferingId() {
        Long vpcOfferingId = vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
        if (vpcOfferingId != null) {
            this.offeringId = vpcOfferingId;
        }
    }

    protected DomainRouterVO deployVpcRouter(final PublicIp sourceNatIp)
            throws ConcurrentOperationException, InsufficientAddressCapacityException,
            InsufficientServerCapacityException, InsufficientCapacityException, StorageUnavailableException, ResourceUnavailableException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = createRouterNetworks();

        DomainRouterVO router =
                nwHelper.deployRouter(this, networks, true, vpcMgr.getSupportedVpcHypervisors());

        return router;
    }

    @Override
    protected LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks()
                    throws ConcurrentOperationException, InsufficientAddressCapacityException {

        TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(this.sourceNatIp.getVlanTag());

        //1) allocate nic for control and source nat public ip
        LinkedHashMap<Network, List<? extends NicProfile>> networks = super.createRouterNetworks();


        //2) allocate nic for private gateways if needed
        List<PrivateGateway> privateGateways = vpcMgr.getVpcPrivateGateways(this.vpc.getId());
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (PrivateGateway privateGateway : privateGateways) {
                NicProfile privateNic = vpcHelper.createPrivateNicProfileForGateway(privateGateway);
                Network privateNetwork = networkModel.getNetwork(privateGateway.getNetworkId());
                networks.put(privateNetwork, new ArrayList<NicProfile>(Arrays.asList(privateNic)));
            }
        }

        //3) allocate nic for guest gateway if needed
        List<? extends Network> guestNetworks = vpcMgr.getVpcNetworks(this.vpc.getId());
        for (Network guestNetwork : guestNetworks) {
            if (networkModel.isPrivateGateway(guestNetwork.getId())) {
                continue;
            }
            if (guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup) {
                NicProfile guestNic = vpcHelper.createGuestNicProfileForVpcRouter(guestNetwork);
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
            }
        }

        //4) allocate nic for additional public network(s)
        List<IPAddressVO> ips = ipAddressDao.listByAssociatedVpc(this.vpc.getId(), false);
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
                            publicOffering, this.plan, null, null, false);
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

    @Override
    protected void planDeploymentRouters() {
        this.routers = vpcHelper.getVpcRouters(this.vpc.getId());
    }

    @Override
    protected void generateDeploymentPlan() {
        final long dcId = this.dest.getDataCenter().getId();
        this.plan = new DataCenterDeployment(dcId);
    }
}
