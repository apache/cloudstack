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

package com.cloud.vpc;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.VpnUser;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

@Component
@Local(value = {VpcVirtualNetworkApplianceManager.class, VpcVirtualNetworkApplianceService.class})
public class MockVpcVirtualNetworkApplianceManager extends ManagerBase implements VpcVirtualNetworkApplianceManager, VpcVirtualNetworkApplianceService {

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#sendSshKeysToHost(java.lang.Long, java.lang.String, java.lang.String)
     */
    @Override
    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#savePasswordToRouter(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, java.util.List)
     */
    @Override
    public boolean savePasswordToRouter(Network network, NicProfile nic, VirtualMachineProfile profile, List<? extends VirtualRouter> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean saveSSHPublicKeyToRouter(Network network, NicProfile nic, VirtualMachineProfile profile, List<? extends VirtualRouter> routers, String SSHPublicKey)
        throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#saveUserDataToRouter(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, java.util.List)
     */
    @Override
    public boolean saveUserDataToRouter(Network network, NicProfile nic, VirtualMachineProfile profile, List<? extends VirtualRouter> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#deployVirtualRouterInGuestNetwork(com.cloud.network.Network, com.cloud.deploy.DeployDestination, com.cloud.user.Account, java.util.Map, boolean)
     */
    @Override
    public List<DomainRouterVO> deployVirtualRouterInGuestNetwork(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params,
        boolean isRedundant) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#startRemoteAccessVpn(com.cloud.network.Network, com.cloud.network.RemoteAccessVpn, java.util.List)
     */
    @Override
    public boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#deleteRemoteAccessVpn(com.cloud.network.Network, com.cloud.network.RemoteAccessVpn, java.util.List)
     */
    @Override
    public boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#associatePublicIP(com.cloud.network.Network, java.util.List, java.util.List)
     */
    @Override
    public boolean associatePublicIP(Network network, List<? extends PublicIpAddress> ipAddress, List<? extends VirtualRouter> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#applyFirewallRules(com.cloud.network.Network, java.util.List, java.util.List)
     */
    @Override
    public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#getRoutersForNetwork(long)
     */
    @Override
    public List<VirtualRouter> getRoutersForNetwork(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#applyVpnUsers(com.cloud.network.Network, java.util.List, java.util.List)
     */
    @Override
    public String[] applyVpnUsers(Network network, List<? extends VpnUser> users, List<DomainRouterVO> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#stop(com.cloud.network.router.VirtualRouter, boolean, com.cloud.user.User, com.cloud.user.Account)
     */
    @Override
    public VirtualRouter stop(VirtualRouter router, boolean forced, User callingUser, Account callingAccount) throws ConcurrentOperationException,
        ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#getDnsBasicZoneUpdate()
     */
    @Override
    public String getDnsBasicZoneUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#applyStaticNats(com.cloud.network.Network, java.util.List, java.util.List)
     */
    @Override
    public boolean applyStaticNats(Network network, List<? extends StaticNat> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#applyDhcpEntry(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DeployDestination, java.util.List)
     */
    @Override
    public boolean applyDhcpEntry(Network config, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, List<DomainRouterVO> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#applyUserData(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DeployDestination, java.util.List)
     */
    @Override
    public boolean applyUserData(Network config, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, List<DomainRouterVO> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean configDhcpForSubnet(Network network, NicProfile nic, VirtualMachineProfile uservm, DeployDestination dest, List<DomainRouterVO> routers)
        throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network, List<DomainRouterVO> routers) throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#startRouter(long, boolean)
     */
    @Override
    public VirtualRouter startRouter(long routerId, boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#rebootRouter(long, boolean)
     */
    @Override
    public VirtualRouter rebootRouter(long routerId, boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#upgradeRouter(com.cloud.api.commands.UpgradeRouterCmd)
     */
    @Override
    public VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#stopRouter(long, boolean)
     */
    @Override
    public VirtualRouter stopRouter(long routerId, boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#startRouter(long)
     */
    @Override
    public VirtualRouter startRouter(long id) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#destroyRouter(long, com.cloud.user.Account, java.lang.Long)
     */
    @Override
    public VirtualRouter destroyRouter(long routerId, Account caller, Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;

    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        return true;

    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VpcVirtualNetworkApplianceService#addVpcRouterToGuestNetwork(com.cloud.network.router.VirtualRouter, com.cloud.network.Network, boolean)
     */
    @Override
    public boolean addVpcRouterToGuestNetwork(VirtualRouter router, Network network, boolean isRedundant) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VpcVirtualNetworkApplianceService#removeVpcRouterFromGuestNetwork(com.cloud.network.router.VirtualRouter, com.cloud.network.Network, boolean)
     */
    @Override
    public boolean removeVpcRouterFromGuestNetwork(VirtualRouter router, Network network, boolean isRedundant) throws ConcurrentOperationException,
        ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#deployVirtualRouterInVpc(com.cloud.network.vpc.Vpc, com.cloud.deploy.DeployDestination, com.cloud.user.Account, java.util.Map)
     */
    @Override
    public List<DomainRouterVO> deployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner, Map<Param, Object> params) throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyNetworkACLs(Network network, List<? extends NetworkACLItem> rules, List<? extends VirtualRouter> routers, boolean privateGateway)
        throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#setupPrivateGateway(com.cloud.network.vpc.PrivateGateway, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean setupPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#destroyPrivateGateway(com.cloud.network.vpc.PrivateGateway, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean destroyPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#applyStaticRoutes(java.util.List, java.util.List)
     */
    @Override
    public boolean applyStaticRoutes(List<StaticRouteProfile> routes, List<DomainRouterVO> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#startSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean startSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#stopSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean stopSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<DomainRouterVO> getVpcRouters(long vpcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyLoadBalancingRules(Network network, List<? extends LoadBalancingRule> rules, List<? extends VirtualRouter> routers)
        throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public VirtualRouter findRouter(long routerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> upgradeRouterTemplate(UpgradeRouterTemplateCmd cmd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean setupDhcpForPvlan(boolean add, DomainRouterVO router, Long hostId, NicProfile nic) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean startRemoteAccessVpn(RemoteAccessVpn vpn, VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stopRemoteAccessVpn(RemoteAccessVpn vpn, VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String[] applyVpnUsers(RemoteAccessVpn vpn, List<? extends VpnUser> users, VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }
}
