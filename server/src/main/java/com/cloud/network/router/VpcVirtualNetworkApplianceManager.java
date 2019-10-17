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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.vm.DomainRouterVO;

public interface VpcVirtualNetworkApplianceManager extends VirtualNetworkApplianceManager, VpcVirtualNetworkApplianceService {

    /**
     * @param gateway
     * @param router
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean destroyPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException;

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
}