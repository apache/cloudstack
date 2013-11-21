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
package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.VpnUser;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = {NetworkElement.class, FirewallServiceProvider.class, DhcpServiceProvider.class, UserDataServiceProvider.class, StaticNatServiceProvider.class,
    LoadBalancingServiceProvider.class, PortForwardingServiceProvider.class, IpDeployer.class, VpcProvider.class, Site2SiteVpnServiceProvider.class,
    NetworkACLServiceProvider.class})
public class VpcVirtualRouterElement extends VirtualRouterElement implements VpcProvider, Site2SiteVpnServiceProvider, NetworkACLServiceProvider {
    private static final Logger s_logger = Logger.getLogger(VpcVirtualRouterElement.class);
    @Inject
    VpcManager _vpcMgr;
    @Inject
    VpcVirtualNetworkApplianceManager _vpcRouterMgr;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    NetworkDao _networkDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    EntityManager _entityMgr;

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Override
    protected boolean canHandle(Network network, Service service) {
        Long physicalNetworkId = _networkMgr.getPhysicalNetworkId(network);
        if (physicalNetworkId == null) {
            return false;
        }

        if (network.getVpcId() == null) {
            return false;
        }

        if (!_networkMgr.isProviderEnabledInPhysicalNetwork(physicalNetworkId, Network.Provider.VPCVirtualRouter.getName())) {
            return false;
        }

        if (service == null) {
            if (!_networkMgr.isProviderForNetwork(getProvider(), network.getId())) {
                s_logger.trace("Element " + getProvider().getName() + " is not a provider for the network " + network);
                return false;
            }
        } else {
            if (!_networkMgr.isProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + service.getName() + " in the network " + network);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException {

        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);

        _vpcRouterMgr.deployVirtualRouterInVpc(vpc, dest, _accountMgr.getAccount(vpc.getAccountId()), params);

        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> routers = _routerDao.listByVpcId(vpc.getId());
        if (routers == null || routers.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (DomainRouterVO router : routers) {
            result = result && (_routerMgr.destroyRouter(router.getId(), context.getAccount(), context.getCaller().getId()) != null);
        }
        return result;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException,
        ConcurrentOperationException, InsufficientCapacityException {

        Long vpcId = network.getVpcId();
        if (vpcId == null) {
            s_logger.trace("Network " + network + " is not associated with any VPC");
            return false;
        }

        Vpc vpc = _vpcMgr.getActiveVpc(vpcId);
        if (vpc == null) {
            s_logger.warn("Unable to find Enabled VPC by id " + vpcId);
            return false;
        }

        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);

        List<DomainRouterVO> routers = _vpcRouterMgr.deployVirtualRouterInVpc(vpc, dest, _accountMgr.getAccount(vpc.getAccountId()), params);
        if ((routers == null) || (routers.size() == 0)) {
            throw new ResourceUnavailableException("Can't find at least one running router!", DataCenter.class, network.getDataCenterId());
        }

        if (routers.size() > 1) {
            throw new CloudRuntimeException("Found more than one router in vpc " + vpc);
        }

        DomainRouterVO router = routers.get(0);
        //Add router to guest network if needed
        if (!_networkMgr.isVmPartOfNetwork(router.getId(), network.getId())) {
            if (!_vpcRouterMgr.addVpcRouterToGuestNetwork(router, network, false)) {
                throw new CloudRuntimeException("Failed to add VPC router " + router + " to guest network " + network);
            } else {
                s_logger.debug("Successfully added VPC router " + router + " to guest network " + network);
            }
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        Long vpcId = network.getVpcId();
        if (vpcId == null) {
            s_logger.trace("Network " + network + " is not associated with any VPC");
            return false;
        }

        Vpc vpc = _vpcMgr.getActiveVpc(vpcId);
        if (vpc == null) {
            s_logger.warn("Unable to find Enabled VPC by id " + vpcId);
            return false;
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
            params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);
            List<DomainRouterVO> routers = _vpcRouterMgr.deployVirtualRouterInVpc(vpc, dest, _accountMgr.getAccount(vpc.getAccountId()), params);
            if ((routers == null) || (routers.size() == 0)) {
                throw new ResourceUnavailableException("Can't find at least one running router!", DataCenter.class, network.getDataCenterId());
            }

            if (routers.size() > 1) {
                throw new CloudRuntimeException("Found more than one router in vpc " + vpc);
            }

            DomainRouterVO router = routers.get(0);
            //Add router to guest network if needed
            if (!_networkMgr.isVmPartOfNetwork(router.getId(), network.getId())) {
                if (!_vpcRouterMgr.addVpcRouterToGuestNetwork(router, network, false)) {
                    throw new CloudRuntimeException("Failed to add VPC router " + router + " to guest network " + network);
                } else {
                    s_logger.debug("Successfully added VPC router " + router + " to guest network " + network);
                }
            }
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        boolean success = true;
        Long vpcId = network.getVpcId();
        if (vpcId == null) {
            s_logger.debug("Network " + network + " doesn't belong to any vpc, so skipping unplug nic part");
            return success;
        }

        List<? extends VirtualRouter> routers = _routerDao.listByVpcId(vpcId);
        for (VirtualRouter router : routers) {
            //1) Check if router is already a part of the network
            if (!_networkMgr.isVmPartOfNetwork(router.getId(), network.getId())) {
                s_logger.debug("Router " + router + " is not a part the network " + network);
                continue;
            }
            //2) Call unplugNics in the network service
            success = success && _vpcRouterMgr.removeVpcRouterFromGuestNetwork(router, network, false);
            if (!success) {
                s_logger.warn("Failed to unplug nic in network " + network + " for virtual router " + router);
            } else {
                s_logger.debug("Successfully unplugged nic in network " + network + " for virtual router " + router);
            }
        }

        return success;
    }

    @Override
    public boolean destroy(Network config, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        boolean success = true;
        Long vpcId = config.getVpcId();
        if (vpcId == null) {
            s_logger.debug("Network " + config + " doesn't belong to any vpc, so skipping unplug nic part");
            return success;
        }

        List<? extends VirtualRouter> routers = _routerDao.listByVpcId(vpcId);
        for (VirtualRouter router : routers) {
            //1) Check if router is already a part of the network
            if (!_networkMgr.isVmPartOfNetwork(router.getId(), config.getId())) {
                s_logger.debug("Router " + router + " is not a part the network " + config);
                continue;
            }
            //2) Call unplugNics in the network service
            success = success && _vpcRouterMgr.removeVpcRouterFromGuestNetwork(router, config, false);
            if (!success) {
                s_logger.warn("Failed to unplug nic in network " + config + " for virtual router " + router);
            } else {
                s_logger.debug("Successfully unplugged nic in network " + config + " for virtual router " + router);
            }
        }

        return success;
    }

    @Override
    public Provider getProvider() {
        return Provider.VPCVirtualRouter;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.putAll(VirtualRouterElement.capabilities);

        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.putAll(capabilities.get(Service.SourceNat));
        sourceNatCapabilities.put(Capability.RedundantRouter, "false");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        Map<Capability, String> vpnCapabilities = new HashMap<Capability, String>();
        vpnCapabilities.putAll(capabilities.get(Service.Vpn));
        vpnCapabilities.put(Capability.VpnTypes, "s2svpn");
        capabilities.put(Service.Vpn, vpnCapabilities);

        //remove firewall capability
        capabilities.remove(Service.Firewall);

        //add network ACL capability
        Map<Capability, String> networkACLCapabilities = new HashMap<Capability, String>();
        networkACLCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        capabilities.put(Service.NetworkACL, networkACLCapabilities);

        return capabilities;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway) throws ConcurrentOperationException, ResourceUnavailableException {
        if (gateway.getType() != VpcGateway.Type.Private) {
            s_logger.warn("Type of vpc gateway is not " + VpcGateway.Type.Private);
            return false;
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(gateway.getVpcId());
        if (routers == null || routers.isEmpty()) {
            s_logger.debug(getName() + " element doesn't need to create Private gateway on the backend; VPC virtual " + "router doesn't exist in the vpc id=" +
                gateway.getVpcId());
            return true;
        }

        if (routers.size() > 1) {
            throw new CloudRuntimeException("Found more than one router in vpc " + gateway.getVpcId());
        }

        VirtualRouter router = routers.get(0);

        if (_vpcRouterMgr.setupPrivateGateway(gateway, router)) {
            try {
                List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(gateway.getNetworkACLId());
                if (!applyACLItemsToPrivateGw(gateway, rules)) {
                    s_logger.debug("Failed to apply network acl id  " + gateway.getNetworkACLId() + "  on gateway ");
                    return false;
                }
            } catch (Exception ex) {
                s_logger.debug("Failed to apply network acl id  " + gateway.getNetworkACLId() + "  on gateway ");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deletePrivateGateway(PrivateGateway gateway) throws ConcurrentOperationException, ResourceUnavailableException {
        if (gateway.getType() != VpcGateway.Type.Private) {
            s_logger.warn("Type of vpc gateway is not " + VpcGateway.Type.Private);
            return false;
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(gateway.getVpcId());
        if (routers == null || routers.isEmpty()) {
            s_logger.debug(getName() + " element doesn't need to delete Private gateway on the backend; VPC virtual " + "router doesn't exist in the vpc id=" +
                gateway.getVpcId());
            return true;
        }

        if (routers.size() > 1) {
            throw new CloudRuntimeException("Found more than one router in vpc " + gateway.getVpcId());
        }

        VirtualRouter router = routers.get(0);

        return _vpcRouterMgr.destroyPrivateGateway(gateway, router);
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException {
        boolean canHandle = true;
        for (Service service : services) {
            if (!canHandle(network, service)) {
                canHandle = false;
                break;
            }
        }
        if (canHandle) {
            List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug(getName() + " element doesn't need to associate ip addresses on the backend; VPC virtual " + "router doesn't exist in the network " +
                    network.getId());
                return true;
            }

            return _vpcRouterMgr.associatePublicIP(network, ipAddress, routers);
        } else {
            return false;
        }
    }

    @Override
    public boolean applyNetworkACLs(Network config, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        if (canHandle(config, Service.NetworkACL)) {
            List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(config.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply firewall rules on the backend; virtual " + "router doesn't exist in the network " +
                    config.getId());
                return true;
            }

            if (!_vpcRouterMgr.applyNetworkACLs(config, rules, routers, false)) {
                throw new CloudRuntimeException("Failed to apply network acl rules in network " + config.getId());
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    protected Type getVirtualRouterProvider() {
        return Type.VPCVirtualRouter;
    }

    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes) throws ResourceUnavailableException {
        List<DomainRouterVO> routers = _routerDao.listByVpcId(vpc.getId());
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Virtual router elemnt doesn't need to static routes on the backend; virtual " + "router doesn't exist in the vpc " + vpc);
            return true;
        }

        if (!_vpcRouterMgr.applyStaticRoutes(routes, routers)) {
            throw new CloudRuntimeException("Failed to apply static routes in vpc " + vpc);
        } else {
            s_logger.debug("Applied static routes on vpc " + vpc);
            return true;
        }
    }

    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        Network config = _networkDao.findById(gateway.getNetworkId());
        boolean isPrivateGateway = true;

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(gateway.getVpcId());
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Virtual router element doesn't need to apply network acl rules on the backend; virtual " + "router doesn't exist in the network " +
                config.getId());
            return true;
        }

        if (!_vpcRouterMgr.applyNetworkACLs(config, rules, routers, isPrivateGateway)) {
            throw new CloudRuntimeException("Failed to apply network acl in network " + config.getId());
        } else {
            return true;
        }
    }

    @Override
    public boolean startSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Site2SiteVpnGateway vpnGw = _vpnGatewayDao.findById(conn.getVpnGatewayId());
        IpAddress ip = _ipAddressDao.findById(vpnGw.getAddrId());

        Map<Capability, String> vpnCapabilities = capabilities.get(Service.Vpn);
        if (!vpnCapabilities.get(Capability.VpnTypes).contains("s2svpn")) {
            s_logger.error("try to start site 2 site vpn on unsupported network element?");
            return false;
        }

        Long vpcId = ip.getVpcId();
        Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);

        if (!_ntwkModel.isProviderEnabledInZone(vpc.getZoneId(), Provider.VPCVirtualRouter.getName())) {
            throw new ResourceUnavailableException("VPC provider is not enabled in zone " + vpc.getZoneId(), DataCenter.class, vpc.getZoneId());
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(ip.getVpcId());
        if (routers == null || routers.size() != 1) {
            throw new ResourceUnavailableException("Cannot enable site-to-site VPN on the backend; virtual router doesn't exist in the vpc " + ip.getVpcId(),
                DataCenter.class, vpc.getZoneId());
        }

        return _vpcRouterMgr.startSite2SiteVpn(conn, routers.get(0));
    }

    @Override
    public boolean stopSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Site2SiteVpnGateway vpnGw = _vpnGatewayDao.findById(conn.getVpnGatewayId());
        IpAddress ip = _ipAddressDao.findById(vpnGw.getAddrId());

        Map<Capability, String> vpnCapabilities = capabilities.get(Service.Vpn);
        if (!vpnCapabilities.get(Capability.VpnTypes).contains("s2svpn")) {
            s_logger.error("try to stop site 2 site vpn on unsupported network element?");
            return false;
        }

        Long vpcId = ip.getVpcId();
        Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);

        if (!_ntwkModel.isProviderEnabledInZone(vpc.getZoneId(), Provider.VPCVirtualRouter.getName())) {
            throw new ResourceUnavailableException("VPC provider is not enabled in zone " + vpc.getZoneId(), DataCenter.class, vpc.getZoneId());
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(ip.getVpcId());
        if (routers == null || routers.size() != 1) {
            throw new ResourceUnavailableException("Cannot enable site-to-site VPN on the backend; virtual router doesn't exist in the vpc " + ip.getVpcId(),
                DataCenter.class, vpc.getZoneId());
        }

        return _vpcRouterMgr.stopSite2SiteVpn(conn, routers.get(0));
    }

    @Override
    public String[] applyVpnUsers(RemoteAccessVpn vpn, List<? extends VpnUser> users) throws ResourceUnavailableException {
        if (vpn.getVpcId() == null) {
            return null;
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(vpn.getVpcId());
        if (routers == null || routers.size() != 1) {
            s_logger.debug("Cannot apply vpn users on the backend; virtual router doesn't exist in the network " + vpn.getVpcId());
            return null;
        }
        return _vpcRouterMgr.applyVpnUsers(vpn, users, routers.get(0));
    }

    @Override
    public boolean startVpn(RemoteAccessVpn vpn) throws ResourceUnavailableException {
        if (vpn.getVpcId() == null) {
            return false;
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(vpn.getVpcId());
        if (routers == null || routers.size() != 1) {
            s_logger.debug("Cannot apply vpn users on the backend; virtual router doesn't exist in the network " + vpn.getVpcId());
            return true;
        }
        return _vpcRouterMgr.startRemoteAccessVpn(vpn, routers.get(0));
    }

    @Override
    public boolean stopVpn(RemoteAccessVpn vpn) throws ResourceUnavailableException {
        if (vpn.getVpcId() == null) {
            return false;
        }

        List<DomainRouterVO> routers = _vpcRouterMgr.getVpcRouters(vpn.getVpcId());
        if (routers == null || routers.size() != 1) {
            s_logger.debug("Cannot apply vpn users on the backend; virtual router doesn't exist in the network " + vpn.getVpcId());
            return true;
        }
        return _vpcRouterMgr.stopRemoteAccessVpn(vpn, routers.get(0));
    }

}
