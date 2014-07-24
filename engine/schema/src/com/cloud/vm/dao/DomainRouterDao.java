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
package com.cloud.vm.dao;

import java.util.List;

import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine.State;

/**
 *
 *  DomainRouterDao implements
 */
public interface DomainRouterDao extends GenericDao<DomainRouterVO, Long> {
    /**
     * gets the DomainRouterVO by user id and data center
     * @Param dcId data center Id.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listByDataCenter(long dcId);

    /**
     * gets the DomainRouterVO by account id and data center
     * @param account id of the user.
     * @Param dcId data center Id.
     * @return DomainRouterVO
     */
    public List<DomainRouterVO> findBy(long accountId, long dcId);

    /**
     * gets the DomainRouterVO by user id.
     * @param userId id of the user.
     * @Param dcId data center Id.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listBy(long userId);

    /**
     * list virtual machine routers by host id.  pass in null to get all
     * virtual machine routers.
     * @param hostId id of the host.  null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listByHostId(Long hostId);

    public List<DomainRouterVO> listByLastHostId(Long hostId);

    /**
     * list virtual machine routers by pod id.  pass in null to get all
     * virtual machine routers.
     * @param podId id of the pod.  null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listRunningByPodId(Long podId);

    /**
     * list virtual machine routers by pod id.  pass in null to get all
     * virtual machine routers.
     * @param podId id of the pod.  null if to get all.
     * @param state state of the domain router. null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listByPodIdAndStates(Long podId, State... states);

    /**
     * list virtual machine routers by host id.
     * pass in null to get all
     * virtual machine routers.
     * @param hostId id of the host.  null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listIsolatedByHostId(Long hostId);

    /**
     * Find the list of domain routers for a domain
     * @param id
     * @return
     */
    public List<DomainRouterVO> listRunningByDomain(Long id);

    List<DomainRouterVO> findBy(long accountId, long dcId, Role role);

    List<DomainRouterVO> findByNetwork(long networkId);

    List<DomainRouterVO> listActive(long networkId);

    /**
     * List domain routers by state and network type which reside on Host managed by the specified management server
     * @return
     */
    List<DomainRouterVO> listByStateAndNetworkType(State state, Network.GuestType type, long mgmtSrvrId);

    List<DomainRouterVO> listByStateAndManagementServer(State state, long mgmtSrvrId);

    List<DomainRouterVO> findByNetworkOutsideThePod(long networkId, long podId, State state, Role role);

    List<DomainRouterVO> listByNetworkAndPodAndRole(long networkId, long podId, Role role);

    List<DomainRouterVO> listByNetworkAndRole(long networkId, Role role);

    List<DomainRouterVO> listByElementId(long elementId);

    /**
     * Persists the domain router instance + creates the reference to the guest network (if not null)
     * @param guestNetworks TODO
     * @return
     */
    DomainRouterVO persist(DomainRouterVO router, List<Network> guestNetworks);

    /**
     * @param routerId
     * @return
     */
    List<Long> getRouterNetworks(long routerId);

    /**
     * @param vpcId
     * @return
     */
    List<DomainRouterVO> listByVpcId(long vpcId);

    /**
     * @param routerId
     * @param guestNetwork
     */
    void addRouterToGuestNetwork(VirtualRouter router, Network guestNetwork);

    /**
     * @param routerId
     * @param guestNetworkId
     */
    void removeRouterFromGuestNetwork(long routerId, long guestNetworkId);

    List<DomainRouterVO> listRunningByClusterId(Long clusterId);

    List<DomainRouterVO> listRunningByAccountId(long accountId);

    List<DomainRouterVO> listRunningByDataCenter(long dcId);

    List<DomainRouterVO> listStopped(long networkId);
}
