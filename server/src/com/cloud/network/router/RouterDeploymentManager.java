package com.cloud.network.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;

public class RouterDeploymentManager {

    private static final Logger logger = Logger.getLogger(RouterDeploymentManager.class);

    @Inject
    VpcDao vpcDao;
    @Inject
    VpcOfferingDao vpcOffDao;
    @Inject
    PhysicalNetworkDao pNtwkDao;
    @Inject
    VpcManager vpcMgr;
    @Inject
    PhysicalNetworkServiceProviderDao physicalProviderDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    NetworkOrchestrationService networkMgr;
    @Inject
    NetworkModel networkModel;
    @Inject
    VirtualRouterProviderDao vrProviderDao;

    @Inject
    NetworkGeneralHelper nwHelper;
    @Inject
    VpcVirtualNetworkHelperImpl vpcHelper;


    protected ServiceOfferingVO _offering;


    ///////////////////////////////////////////////////////////////////////
    // Non-VPC behavior
    ///////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////
    // VPC Specific behavior
    ///////////////////////////////////////////////////////////////////////

    public List<DomainRouterVO> deployVirtualRouterInVpc(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {

        List<DomainRouterVO> routers = findOrDeployVirtualRouterInVpc(routerDeploymentDefinition);

        return nwHelper.startRouters(routerDeploymentDefinition.getParams(), routers);
    }

    @DB
    protected List<DomainRouterVO> findOrDeployVirtualRouterInVpc(final RouterDeploymentDefinition routerDeploymentDefinition)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        final Vpc vpc = routerDeploymentDefinition.getVpc();
        logger.debug("Deploying Virtual Router in VPC " + vpc);

        Vpc vpcLock = vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }

        //1) Get deployment plan and find out the list of routers
        Pair<DeploymentPlan, List<DomainRouterVO>> planAndRouters = getDeploymentPlanAndRouters(routerDeploymentDefinition);
        DeploymentPlan plan = planAndRouters.first();
        List<DomainRouterVO> routers = planAndRouters.second();
        try {
            //2) Return routers if exist
            if (routers.size() >= 1) {
                return routers;
            }

            Long offeringId = vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
            if (offeringId == null) {
                offeringId = _offering.getId();
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
            routers.add(router);

        } finally {
            // TODO Should we do this after the pre or after the whole??
            if (vpcLock != null) {
                vpcDao.releaseFromLockTable(vpc.getId());
            }
        }
        return routers;
    }

    protected Pair<DeploymentPlan, List<DomainRouterVO>> getDeploymentPlanAndRouters(final RouterDeploymentDefinition routerDeploymentDefinition) {
        long dcId = routerDeploymentDefinition.getDest().getDataCenter().getId();

        DeploymentPlan plan = new DataCenterDeployment(dcId);
        List<DomainRouterVO> routers = vpcHelper.getVpcRouters(routerDeploymentDefinition.getVpc().getId());

        return new Pair<DeploymentPlan, List<DomainRouterVO>>(plan, routers);
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
