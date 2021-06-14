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

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;
import org.springframework.stereotype.Component;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class MockVpcVirtualNetworkApplianceManager extends ManagerBase implements VpcVirtualNetworkApplianceManager, VpcVirtualNetworkApplianceService {

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#startRemoteAccessVpn(com.cloud.network.Network, com.cloud.network.RemoteAccessVpn, java.util.List)
     */
    @Override
    public boolean startRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#deleteRemoteAccessVpn(com.cloud.network.Network, com.cloud.network.RemoteAccessVpn, java.util.List)
     */
    @Override
    public boolean deleteRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#getRoutersForNetwork(long)
     */
    @Override
    public List<VirtualRouter> getRoutersForNetwork(final long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VirtualNetworkApplianceManager#stop(com.cloud.network.router.VirtualRouter, boolean, com.cloud.user.User, com.cloud.user.Account)
     */
    @Override
    public VirtualRouter stop(final VirtualRouter router, final boolean forced, final User callingUser, final Account callingAccount) throws ConcurrentOperationException {
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

    @Override
    public boolean removeDhcpSupportForSubnet(final Network network, final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#startRouter(long, boolean)
     */
    @Override
    public VirtualRouter startRouter(final long routerId, final boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#rebootRouter(long, boolean)
     */
    @Override
    public VirtualRouter rebootRouter(final long routerId, final boolean reprogramNetwork, final boolean forced) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#upgradeRouter(com.cloud.api.commands.UpgradeRouterCmd)
     */
    @Override
    public VirtualRouter upgradeRouter(final UpgradeRouterCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#stopRouter(long, boolean)
     */
    @Override
    public VirtualRouter stopRouter(final long routerId, final boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#startRouter(long)
     */
    @Override
    public VirtualRouter startRouter(final long id) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VirtualNetworkApplianceService#destroyRouter(long, com.cloud.user.Account, java.lang.Long)
     */
    @Override
    public VirtualRouter destroyRouter(final long routerId, final Account caller, final Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
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
    public boolean addVpcRouterToGuestNetwork(final VirtualRouter router, final Network network, final Map<VirtualMachineProfile.Param, Object> params)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.VpcVirtualNetworkApplianceService#removeVpcRouterFromGuestNetwork(com.cloud.network.router.VirtualRouter, com.cloud.network.Network, boolean)
     */
    @Override
    public boolean removeVpcRouterFromGuestNetwork(final VirtualRouter router, final Network network) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#destroyPrivateGateway(com.cloud.network.vpc.PrivateGateway, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean destroyPrivateGateway(final PrivateGateway gateway, final VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#startSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean startSite2SiteVpn(final Site2SiteVpnConnection conn, final VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcVirtualNetworkApplianceManager#stopSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection, com.cloud.network.router.VirtualRouter)
     */
    @Override
    public boolean stopSite2SiteVpn(final Site2SiteVpnConnection conn, final VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<DomainRouterVO> getVpcRouters(final long vpcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter findRouter(final long routerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> upgradeRouterTemplate(final UpgradeRouterTemplateCmd cmd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Pair<Boolean, String> performRouterHealthChecks(long routerId) {
        return null;
    }

    @Override
    public boolean prepareAggregatedExecution(final Network network, final List<DomainRouterVO> routers) throws AgentUnavailableException {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean completeAggregatedExecution(final Network network, final List<DomainRouterVO> routers) throws AgentUnavailableException {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean startRemoteAccessVpn(final RemoteAccessVpn vpn, final VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stopRemoteAccessVpn(final RemoteAccessVpn vpn, final VirtualRouter router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean startSite2SiteVpn(DomainRouterVO router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T extends VirtualRouter> void collectNetworkStatistics(T router, Nic nic) {
        // TODO Auto-generated method stub
        return;
    }
}
