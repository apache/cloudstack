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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.dao.RouterNetworkDao;
import com.cloud.network.dao.RouterNetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine.State;

@Component
@Local(value = {DomainRouterDao.class})
@DB
public class DomainRouterDaoImpl extends GenericDaoBase<DomainRouterVO, Long> implements DomainRouterDao {

    protected SearchBuilder<DomainRouterVO> AllFieldsSearch;
    protected SearchBuilder<DomainRouterVO> RunningSearch;
    protected SearchBuilder<DomainRouterVO> IdNetworkIdStatesSearch;
    protected SearchBuilder<DomainRouterVO> HostUpSearch;
    protected SearchBuilder<DomainRouterVO> StateNetworkTypeSearch;
    protected SearchBuilder<DomainRouterVO> OutsidePodSearch;
    protected SearchBuilder<DomainRouterVO> clusterSearch;
    protected SearchBuilder<DomainRouterVO> SearchByStateAndManagementServerId;
    @Inject
    HostDao _hostsDao;
    @Inject
    RouterNetworkDao _routerNetworkDao;
    @Inject
    UserStatisticsDao _userStatsDao;
    @Inject
    NetworkOfferingDao _offDao;
    protected SearchBuilder<DomainRouterVO> VpcSearch;

    public DomainRouterDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("dc", AllFieldsSearch.entity().getDataCenterId(), Op.EQ);
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("role", AllFieldsSearch.entity().getRole(), Op.EQ);
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), Op.EQ);
        AllFieldsSearch.and("host", AllFieldsSearch.entity().getHostId(), Op.EQ);
        AllFieldsSearch.and("lastHost", AllFieldsSearch.entity().getLastHostId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("states", AllFieldsSearch.entity().getState(), Op.IN);
        final SearchBuilder<RouterNetworkVO> joinRouterNetwork = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork.and("networkId", joinRouterNetwork.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.join("networkRouter", joinRouterNetwork, joinRouterNetwork.entity().getRouterId(), AllFieldsSearch.entity().getId(), JoinType.INNER);
        AllFieldsSearch.and("podId", AllFieldsSearch.entity().getPodIdToDeployIn(), Op.EQ);
        AllFieldsSearch.and("elementId", AllFieldsSearch.entity().getElementId(), Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), Op.EQ);
        AllFieldsSearch.done();

        VpcSearch = createSearchBuilder();
        VpcSearch.and("role", VpcSearch.entity().getRole(), Op.EQ);
        VpcSearch.and("vpcId", VpcSearch.entity().getVpcId(), Op.EQ);
        VpcSearch.done();

        IdNetworkIdStatesSearch = createSearchBuilder();
        IdNetworkIdStatesSearch.and("id", IdNetworkIdStatesSearch.entity().getId(), Op.EQ);
        final SearchBuilder<RouterNetworkVO> joinRouterNetwork1 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork1.and("networkId", joinRouterNetwork1.entity().getNetworkId(), Op.EQ);
        IdNetworkIdStatesSearch.join("networkRouter", joinRouterNetwork1, joinRouterNetwork1.entity().getRouterId(), IdNetworkIdStatesSearch.entity().getId(),
                JoinType.INNER);
        IdNetworkIdStatesSearch.and("states", IdNetworkIdStatesSearch.entity().getState(), Op.IN);
        IdNetworkIdStatesSearch.done();

        HostUpSearch = createSearchBuilder();
        HostUpSearch.select(null, Func.DISTINCT, HostUpSearch.entity().getId());
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), Op.NIN);
        final SearchBuilder<RouterNetworkVO> joinRouterNetwork3 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork3.and("networkId", joinRouterNetwork3.entity().getNetworkId(), Op.EQ);
        joinRouterNetwork3.and("type", joinRouterNetwork3.entity().getGuestType(), Op.EQ);
        HostUpSearch.join("networkRouter", joinRouterNetwork3, joinRouterNetwork3.entity().getRouterId(), HostUpSearch.entity().getId(), JoinType.INNER);
        HostUpSearch.done();

        StateNetworkTypeSearch = createSearchBuilder();
        StateNetworkTypeSearch.select(null, Func.DISTINCT, StateNetworkTypeSearch.entity().getId());
        StateNetworkTypeSearch.and("state", StateNetworkTypeSearch.entity().getState(), Op.EQ);
        final SearchBuilder<RouterNetworkVO> joinRouterNetwork4 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork4.and("networkId", joinRouterNetwork4.entity().getNetworkId(), Op.EQ);
        joinRouterNetwork4.and("type", joinRouterNetwork4.entity().getGuestType(), Op.EQ);
        StateNetworkTypeSearch.join("networkRouter", joinRouterNetwork4, joinRouterNetwork4.entity().getRouterId(), StateNetworkTypeSearch.entity().getId(),
                JoinType.INNER);

        final SearchBuilder<HostVO> joinHost = _hostsDao.createSearchBuilder();
        joinHost.and("mgmtServerId", joinHost.entity().getManagementServerId(), Op.EQ);
        StateNetworkTypeSearch.join("host", joinHost, joinHost.entity().getId(), StateNetworkTypeSearch.entity().getHostId(), JoinType.INNER);
        StateNetworkTypeSearch.done();

        SearchByStateAndManagementServerId = createSearchBuilder();
        SearchByStateAndManagementServerId.and("state", SearchByStateAndManagementServerId.entity().getState(), Op.EQ);

        final SearchBuilder<HostVO> joinHost2 = _hostsDao.createSearchBuilder();
        joinHost2.and("mgmtServerId", joinHost2.entity().getManagementServerId(), Op.EQ);
        SearchByStateAndManagementServerId.join("host", joinHost2, joinHost2.entity().getId(), SearchByStateAndManagementServerId.entity().getHostId(), JoinType.INNER);
        SearchByStateAndManagementServerId.done();

        OutsidePodSearch = createSearchBuilder();
        final SearchBuilder<RouterNetworkVO> joinRouterNetwork2 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork2.and("networkId", joinRouterNetwork2.entity().getNetworkId(), Op.EQ);
        OutsidePodSearch.join("networkRouter", joinRouterNetwork2, joinRouterNetwork2.entity().getRouterId(), OutsidePodSearch.entity().getId(), JoinType.INNER);
        OutsidePodSearch.and("podId", OutsidePodSearch.entity().getPodIdToDeployIn(), Op.NEQ);
        OutsidePodSearch.and("state", OutsidePodSearch.entity().getState(), Op.EQ);
        OutsidePodSearch.and("role", OutsidePodSearch.entity().getRole(), Op.EQ);
        OutsidePodSearch.done();

        clusterSearch = createSearchBuilder();
        clusterSearch.and("state", clusterSearch.entity().getState(), Op.EQ);
        final SearchBuilder<HostVO> clusterHost = _hostsDao.createSearchBuilder();
        clusterHost.and("clusterId", clusterHost.entity().getClusterId(), Op.EQ);
        clusterSearch.join("host", clusterHost, clusterSearch.entity().getHostId(), clusterHost.entity().getId(), JoinType.INNER);
        clusterSearch.done();

        RunningSearch = createSearchBuilder();
        RunningSearch.and("dc", RunningSearch.entity().getDataCenterId(), Op.EQ);
        RunningSearch.and("account", RunningSearch.entity().getAccountId(), Op.EQ);
        RunningSearch.and("domainId", RunningSearch.entity().getDomainId(), Op.EQ);
        RunningSearch.and("state", RunningSearch.entity().getState(), Op.EQ);
        RunningSearch.and("podId", RunningSearch.entity().getPodIdToDeployIn(), Op.EQ);
        RunningSearch.done();
    }

    @Override
    public boolean remove(final Long id) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        final DomainRouterVO router = createForUpdate();
        router.setPublicIpAddress(null);
        final UpdateBuilder ub = getUpdateBuilder(router);
        ub.set(router, "state", State.Destroyed);
        update(id, ub, router);

        final boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<DomainRouterVO> listByDataCenter(final long dcId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("dc", dcId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findBy(final long accountId, final long dcId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", Role.VIRTUAL_ROUTER);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findBy(final long accountId, final long dcId, final Role role) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listBy(final long accountId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByHostId(final Long hostId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("host", hostId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listRunningByPodId(final Long podId) {
        final SearchCriteria<DomainRouterVO> sc = RunningSearch.create();
        sc.setParameters("state", State.Running);
        sc.setParameters("podId", podId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listRunningByClusterId(final Long clusterId) {
        final SearchCriteria<DomainRouterVO> sc = clusterSearch.create();
        sc.setParameters("state", State.Running);
        sc.setJoinParameters("host", "clusterId", clusterId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByPodIdAndStates(final Long podId, final State... states) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("podId", podId);
        sc.setParameters("states", (Object[])states);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listIsolatedByHostId(final Long hostId) {
        final SearchCriteria<DomainRouterVO> sc = HostUpSearch.create();
        if (hostId != null) {
            sc.setParameters("host", hostId);
        }
        sc.setJoinParameters("networkRouter", "type", Network.GuestType.Isolated);
        final List<DomainRouterVO> routerIds = listBy(sc);
        final List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        for (final DomainRouterVO router : routerIds) {
            CollectionUtils.addIgnoreNull(routers, findById(router.getId()));
        }
        return routers;
    }

    @Override
    public List<DomainRouterVO> listRunningByDomain(final Long domainId) {
        final SearchCriteria<DomainRouterVO> sc = RunningSearch.create();
        sc.setParameters("state", State.Running);
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findByNetwork(final long networkId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByLastHostId(final Long hostId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("lastHost", hostId);
        sc.setParameters("state", State.Stopped);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listActive(final long networkId) {
        final SearchCriteria<DomainRouterVO> sc = IdNetworkIdStatesSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("states", State.Running, State.Migrating, State.Stopping, State.Starting);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByStateAndNetworkType(final State state, final Network.GuestType type, final long mgmtSrvrId) {
        final SearchCriteria<DomainRouterVO> sc = StateNetworkTypeSearch.create();
        sc.setParameters("state", state);
        sc.setJoinParameters("networkRouter", "type", type);
        sc.setJoinParameters("host", "mgmtServerId", mgmtSrvrId);
        final List<DomainRouterVO> routerIds = listBy(sc);
        final List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        for (final DomainRouterVO router : routerIds) {
            routers.add(findById(router.getId()));
        }
        return routers;
    }

    @Override
    public List<DomainRouterVO> listByStateAndManagementServer(final State state, final long mgmtSrvrId) {
        final SearchCriteria<DomainRouterVO> sc = SearchByStateAndManagementServerId.create();
        sc.setParameters("state", state);
        sc.setJoinParameters("host", "mgmtServerId", mgmtSrvrId);

        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findByNetworkOutsideThePod(final long networkId, final long podId, final State state, final Role role) {
        final SearchCriteria<DomainRouterVO> sc = OutsidePodSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("podId", podId);
        sc.setParameters("state", state);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByNetworkAndPodAndRole(final long networkId, final long podId, final Role role) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("podId", podId);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByNetworkAndRole(final long networkId, final Role role) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByElementId(final long elementId) {
        final SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("elementId", elementId);
        return listBy(sc);
    }

    @Override
    @DB
    public DomainRouterVO persist(final DomainRouterVO router, final List<Network> guestNetworks) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        // 1) create network
        final DomainRouterVO newRouter = super.persist(router);

        if (guestNetworks != null && !guestNetworks.isEmpty()) {
            // 2) add router to the network
            for (final Network guestNetwork : guestNetworks) {
                addRouterToGuestNetwork(router, guestNetwork);
            }
        }

        txn.commit();
        return newRouter;
    }

    @Override
    @DB
    public void addRouterToGuestNetwork(final VirtualRouter router, final Network guestNetwork) {
        if (_routerNetworkDao.findByRouterAndNetwork(router.getId(), guestNetwork.getId()) == null) {
            final NetworkOffering off = _offDao.findById(guestNetwork.getNetworkOfferingId());
            if (!off.getName().equalsIgnoreCase(NetworkOffering.SystemPrivateGatewayNetworkOffering)) {
                final TransactionLegacy txn = TransactionLegacy.currentTxn();
                txn.start();
                //1) add router to network
                final RouterNetworkVO routerNtwkMap = new RouterNetworkVO(router.getId(), guestNetwork.getId(), guestNetwork.getGuestType());
                _routerNetworkDao.persist(routerNtwkMap);
                //2) create user stats entry for the network
                UserStatisticsVO stats =
                        _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), guestNetwork.getId(), null, router.getId(), router.getType().toString());
                if (stats == null) {
                    stats =
                            new UserStatisticsVO(router.getAccountId(), router.getDataCenterId(), null, router.getId(), router.getType().toString(), guestNetwork.getId());
                    _userStatsDao.persist(stats);
                }
                txn.commit();
            }
        }
    }

    @Override
    public void removeRouterFromGuestNetwork(final long routerId, final long guestNetworkId) {
        final RouterNetworkVO routerNtwkMap = _routerNetworkDao.findByRouterAndNetwork(routerId, guestNetworkId);
        if (routerNtwkMap != null) {
            _routerNetworkDao.remove(routerNtwkMap.getId());
        }
    }

    @Override
    public List<Long> getRouterNetworks(final long routerId) {
        return _routerNetworkDao.getRouterNetworks(routerId);
    }

    @Override
    public List<DomainRouterVO> listByVpcId(final long vpcId) {
        final SearchCriteria<DomainRouterVO> sc = VpcSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("role", Role.VIRTUAL_ROUTER);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listRunningByAccountId(final long accountId) {
        final SearchCriteria<DomainRouterVO> sc = RunningSearch.create();
        sc.setParameters("state", State.Running);
        sc.setParameters("account", accountId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listRunningByDataCenter(final long dcId) {
        final SearchCriteria<DomainRouterVO> sc = RunningSearch.create();
        sc.setParameters("state", State.Running);
        sc.setParameters("dc", dcId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listStopped(final long networkId) {
        final SearchCriteria<DomainRouterVO> sc = IdNetworkIdStatesSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("states", State.Stopped);
        return listBy(sc);
    }
}
