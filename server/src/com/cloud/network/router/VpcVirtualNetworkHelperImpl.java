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


import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
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
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.server.ConfigurationServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;


@Component
// This will not be a public service anymore, but a helper for the only public service
@Local(value = {VpcVirtualNetworkHelper.class})
public class VpcVirtualNetworkHelperImpl implements VpcVirtualNetworkHelper {
//implements VpcVirtualNetworkApplianceManager {

    private static final Logger s_logger = Logger.getLogger(VpcVirtualNetworkHelperImpl.class);
    String _name;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    PhysicalNetworkDao _pNtwkDao;
    @Inject
    NetworkService _ntwkService;
    @Inject
    NetworkACLManager _networkACLMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    EntityManager _entityMgr;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    FirewallRulesDao _rulesDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;

    protected ServiceOfferingVO _offering;

    protected NetworkGeneralHelper nwHelper = new NetworkGeneralHelper();


    @Override
    public List<DomainRouterVO> deployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner,
            Map<Param, Object> params, boolean isRedundant)
            throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException {

        List<DomainRouterVO> routers = findOrDeployVirtualRouterInVpc(vpc, dest, owner, params, isRedundant);

        return this.nwHelper.startRouters(params, routers);
    }

    @DB
    protected List<DomainRouterVO> findOrDeployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner,
            Map<Param, Object> params, boolean isRedundant)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        s_logger.debug("Deploying Virtual Router in VPC " + vpc);
        Vpc vpcLock = _vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }

        //1) Get deployment plan and find out the list of routers
        Pair<DeploymentPlan, List<DomainRouterVO>> planAndRouters = getDeploymentPlanAndRouters(vpc.getId(), dest);
        DeploymentPlan plan = planAndRouters.first();
        List<DomainRouterVO> routers = planAndRouters.second();
        try {
            //2) Return routers if exist
            if (routers.size() >= 1) {
                return routers;
            }

            Long offeringId = _vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
            if (offeringId == null) {
                offeringId = _offering.getId();
            }
            //3) Deploy Virtual Router
            List<? extends PhysicalNetwork> pNtwks = _pNtwkDao.listByZone(vpc.getZoneId());

            VirtualRouterProvider vpcVrProvider = null;

            for (PhysicalNetwork pNtwk : pNtwks) {
                PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(pNtwk.getId(), Type.VPCVirtualRouter.toString());
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + Type.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
                }
                vpcVrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), Type.VPCVirtualRouter);
                if (vpcVrProvider != null) {
                    break;
                }
            }

            PublicIp sourceNatIp = _vpcMgr.assignSourceNatIpAddressToVpc(owner, vpc);

            DomainRouterVO router = deployVpcRouter(owner, dest, plan, params, isRedundant, vpcVrProvider, offeringId, vpc.getId(), sourceNatIp);
            routers.add(router);

        } finally {
            // TODO Should we do this after the pre or after the whole??
            if (vpcLock != null) {
                _vpcDao.releaseFromLockTable(vpc.getId());
            }
        }
        return routers;
    }

    protected Pair<DeploymentPlan, List<DomainRouterVO>> getDeploymentPlanAndRouters(long vpcId, DeployDestination dest) {
        long dcId = dest.getDataCenter().getId();

        DeploymentPlan plan = new DataCenterDeployment(dcId);
        List<DomainRouterVO> routers = getVpcRouters(vpcId);

        return new Pair<DeploymentPlan, List<DomainRouterVO>>(plan, routers);
    }


    protected DomainRouterVO deployVpcRouter(Account owner, DeployDestination dest, DeploymentPlan plan, Map<Param, Object> params, boolean isRedundant,
        VirtualRouterProvider vrProvider, long svcOffId, Long vpcId, PublicIp sourceNatIp) throws ConcurrentOperationException, InsufficientAddressCapacityException,
        InsufficientServerCapacityException, InsufficientCapacityException, StorageUnavailableException, ResourceUnavailableException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = createVpcRouterNetworks(owner, isRedundant, plan, new Pair<Boolean, PublicIp>(true, sourceNatIp),vpcId);

        DomainRouterVO router =
            this.nwHelper.deployRouter(owner, dest, plan, params, isRedundant, vrProvider, svcOffId, vpcId, networks, true, _vpcMgr.getSupportedVpcHypervisors());

        return router;
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> createVpcRouterNetworks(Account owner, boolean isRedundant, DeploymentPlan plan, Pair<Boolean, PublicIp> sourceNatIp,
        long vpcId) throws ConcurrentOperationException, InsufficientAddressCapacityException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(4);

        TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(sourceNatIp.second().getVlanTag());

        //1) allocate nic for control and source nat public ip
        networks = this.nwHelper.createRouterNetworks(owner, isRedundant, plan, null, sourceNatIp);


        //2) allocate nic for private gateways if needed
        List<PrivateGateway> privateGateways = _vpcMgr.getVpcPrivateGateways(vpcId);
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (PrivateGateway privateGateway : privateGateways) {
                NicProfile privateNic = createPrivateNicProfileForGateway(privateGateway);
                Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
                networks.put(privateNetwork, new ArrayList<NicProfile>(Arrays.asList(privateNic)));
            }
        }

        //3) allocate nic for guest gateway if needed
        List<? extends Network> guestNetworks = _vpcMgr.getVpcNetworks(vpcId);
        for (Network guestNetwork : guestNetworks) {
            if (_networkModel.isPrivateGateway(guestNetwork.getId())) {
                continue;
            }
            if (guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup) {
                NicProfile guestNic = createGuestNicProfileForVpcRouter(guestNetwork);
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
            }
        }

        //4) allocate nic for additional public network(s)
        List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(vpcId, false);
        List<NicProfile> publicNics = new ArrayList<NicProfile>();
        Network publicNetwork = null;
        for (IPAddressVO ip : ips) {
            PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
            if ((ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) && _vpcMgr.isIpAllocatedToVpc(ip) &&
                !publicVlans.contains(publicIp.getVlanTag())) {
                s_logger.debug("Allocating nic for router in vlan " + publicIp.getVlanTag());
                NicProfile publicNic = new NicProfile();
                publicNic.setDefaultNic(false);
                publicNic.setIp4Address(publicIp.getAddress().addr());
                publicNic.setGateway(publicIp.getGateway());
                publicNic.setNetmask(publicIp.getNetmask());
                publicNic.setMacAddress(publicIp.getMacAddress());
                publicNic.setBroadcastType(BroadcastDomainType.Vlan);
                publicNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(publicIp.getVlanTag()));
                publicNic.setIsolationUri(IsolationType.Vlan.toUri(publicIp.getVlanTag()));
                NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
                if (publicNetwork == null) {
                    List<? extends Network> publicNetworks = _networkMgr.setupNetwork(VirtualNwStatus.account,
                            publicOffering, plan, null, null, false);
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

    protected NicProfile createGuestNicProfileForVpcRouter(Network guestNetwork) {
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

    @Override
    public List<DomainRouterVO> getVpcRouters(long vpcId) {
        return _routerDao.listByVpcId(vpcId);
    }

    @Override
    @DB
    public NicProfile createPrivateNicProfileForGateway(VpcGateway privateGateway) {
        Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
        PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(privateNetwork.getDataCenterId(), privateNetwork.getId(), privateGateway.getIp4Address());
        Nic privateNic = _nicDao.findByIp4AddressAndNetworkId(ipVO.getIpAddress(), privateNetwork.getId());

        NicProfile privateNicProfile = new NicProfile();

        if (privateNic != null) {
            VirtualMachine vm = _vmDao.findById(privateNic.getInstanceId());
            privateNicProfile =
                new NicProfile(privateNic, privateNetwork, privateNic.getBroadcastUri(), privateNic.getIsolationUri(), _networkModel.getNetworkRate(
                    privateNetwork.getId(), vm.getId()), _networkModel.isSecurityGroupSupportedInNetwork(privateNetwork), _networkModel.getNetworkTag(
                    vm.getHypervisorType(), privateNetwork));
        } else {
            String netmask = NetUtils.getCidrNetmask(privateNetwork.getCidr());
            PrivateIpAddress ip =
                new PrivateIpAddress(ipVO, privateNetwork.getBroadcastUri().toString(), privateNetwork.getGateway(), netmask,
                    NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress())));

            URI netUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
            privateNicProfile.setIp4Address(ip.getIpAddress());
            privateNicProfile.setGateway(ip.getGateway());
            privateNicProfile.setNetmask(ip.getNetmask());
            privateNicProfile.setIsolationUri(netUri);
            privateNicProfile.setBroadcastUri(netUri);
            // can we solve this in setBroadcastUri()???
            // or more plugable construct is desirable
            privateNicProfile.setBroadcastType(BroadcastDomainType.getSchemeValue(netUri));
            privateNicProfile.setFormat(AddressFormat.Ip4);
            privateNicProfile.setReservationId(String.valueOf(ip.getBroadcastUri()));
            privateNicProfile.setMacAddress(ip.getMacAddress());
        }

        return privateNicProfile;
    }

}
