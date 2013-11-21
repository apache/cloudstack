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

import java.util.List;
import java.util.Map;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.VpnUser;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineProfile.Param;

public interface VpcVirtualNetworkApplianceManager extends VirtualNetworkApplianceManager, VpcVirtualNetworkApplianceService {

    /**
     * @param vpc
     * @param dest
     * @param owner
     * @param params
     * @return
     * @throws InsufficientCapacityException
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    List<DomainRouterVO> deployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner, Map<Param, Object> params) throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException;

    /**
     *
     * @param network
     * @param rules
     * @param routers
     * @param privateGateway
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyNetworkACLs(Network network, List<? extends NetworkACLItem> rules, List<? extends VirtualRouter> routers, boolean privateGateway)
        throws ResourceUnavailableException;

    /**
     * @param gateway
     * @param router TODO
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean setupPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param gateway
     * @param router
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean destroyPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param routes
     * @param routers
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyStaticRoutes(List<StaticRouteProfile> routes, List<DomainRouterVO> routers) throws ResourceUnavailableException;

    /**
     * @param conn
     * @param routers
     * @return
     * @throws ResourceUnavailableException
     */
    boolean startSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException;

    /**
     * @param conn
     * @param routers
     * @return
     * @throws ResourceUnavailableException
     */
    boolean stopSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException;

    /**
     * @param vpcId
     * @return
     */
    List<DomainRouterVO> getVpcRouters(long vpcId);

    /**
     * @param vpn
     * @param router
     * @return
     * @throws ResourceUnavailableException
     */
    boolean startRemoteAccessVpn(RemoteAccessVpn vpn, VirtualRouter router) throws ResourceUnavailableException;

    /**
     * @param vpn
     * @param router
     * @return
     * @throws ResourceUnavailableException
     */
    boolean stopRemoteAccessVpn(RemoteAccessVpn vpn, VirtualRouter router) throws ResourceUnavailableException;

    /**
     * @param vpn
     * @param users
     * @param routers
     * @return
     * @throws ResourceUnavailableException
     */
    String[] applyVpnUsers(RemoteAccessVpn vpn, List<? extends VpnUser> users, VirtualRouter router) throws ResourceUnavailableException;
}
