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

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.network.Network;
import com.cloud.network.dao.RouterNetworkDao;
import com.cloud.network.dao.RouterNetworkDaoImpl;
import com.cloud.network.dao.RouterNetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDaoImpl;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatisticsDaoImpl;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Component
@Local(value = { DomainRouterDao.class })
@DB
public class DomainRouterDaoImpl extends GenericDaoBase<DomainRouterVO, Long> implements DomainRouterDao {

    protected SearchBuilder<DomainRouterVO> AllFieldsSearch;
    protected SearchBuilder<DomainRouterVO> IdNetworkIdStatesSearch;
    protected SearchBuilder<DomainRouterVO> HostUpSearch;
    protected SearchBuilder<DomainRouterVO> StateNetworkTypeSearch;
    protected SearchBuilder<DomainRouterVO> OutsidePodSearch;
    @Inject HostDao _hostsDao;
    @Inject RouterNetworkDao _routerNetworkDao;
    @Inject UserStatisticsDao _userStatsDao;
    @Inject NetworkOfferingDao _offDao;
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
        SearchBuilder<RouterNetworkVO> joinRouterNetwork = _routerNetworkDao.createSearchBuilder();
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
        SearchBuilder<RouterNetworkVO> joinRouterNetwork1 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork1.and("networkId", joinRouterNetwork1.entity().getNetworkId(), Op.EQ);
        IdNetworkIdStatesSearch.join("networkRouter", joinRouterNetwork1, joinRouterNetwork1.entity().getRouterId(), IdNetworkIdStatesSearch.entity().getId(), JoinType.INNER);
        IdNetworkIdStatesSearch.and("states", IdNetworkIdStatesSearch.entity().getState(), Op.IN);
        IdNetworkIdStatesSearch.done();

        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), Op.NIN);
        SearchBuilder<RouterNetworkVO> joinRouterNetwork3 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork3.and("networkId", joinRouterNetwork3.entity().getNetworkId(), Op.EQ);
        joinRouterNetwork3.and("type", joinRouterNetwork3.entity().getGuestType(), Op.EQ);
        HostUpSearch.join("networkRouter", joinRouterNetwork3, joinRouterNetwork3.entity().getRouterId(), HostUpSearch.entity().getId(), JoinType.INNER);
        HostUpSearch.done();
          
        StateNetworkTypeSearch = createSearchBuilder();
        StateNetworkTypeSearch.and("state", StateNetworkTypeSearch.entity().getState(), Op.EQ);
        SearchBuilder<RouterNetworkVO> joinRouterNetwork4 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork4.and("networkId", joinRouterNetwork4.entity().getNetworkId(), Op.EQ);
        joinRouterNetwork4.and("type", joinRouterNetwork4.entity().getGuestType(), Op.EQ);
        StateNetworkTypeSearch.join("networkRouter", joinRouterNetwork4, joinRouterNetwork4.entity().getRouterId(), StateNetworkTypeSearch.entity().getId(), JoinType.INNER);
        
        SearchBuilder<HostVO> joinHost = _hostsDao.createSearchBuilder();
        joinHost.and("mgmtServerId", joinHost.entity().getManagementServerId(), Op.EQ);
        StateNetworkTypeSearch.join("host", joinHost, joinHost.entity().getId(), 
                StateNetworkTypeSearch.entity().getHostId(), JoinType.INNER);
        StateNetworkTypeSearch.done();
        
        
        OutsidePodSearch = createSearchBuilder();
        SearchBuilder<RouterNetworkVO> joinRouterNetwork2 = _routerNetworkDao.createSearchBuilder();
        joinRouterNetwork2.and("networkId", joinRouterNetwork2.entity().getNetworkId(), Op.EQ);
        OutsidePodSearch.join("networkRouter", joinRouterNetwork2, joinRouterNetwork2.entity().getRouterId(), 
                OutsidePodSearch.entity().getId(), JoinType.INNER);
        OutsidePodSearch.and("podId", OutsidePodSearch.entity().getPodIdToDeployIn(), Op.NEQ);
        OutsidePodSearch.and("state", OutsidePodSearch.entity().getState(), Op.EQ);
        OutsidePodSearch.and("role", OutsidePodSearch.entity().getRole(), Op.EQ);
        OutsidePodSearch.done();

    }

    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        DomainRouterVO router = createForUpdate();
        router.setPublicIpAddress(null);
        UpdateBuilder ub = getUpdateBuilder(router);
        ub.set(router, "state", State.Destroyed);
        update(id, ub, router);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<DomainRouterVO> listByDataCenter(long dcId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("dc", dcId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findBy(long accountId, long dcId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", Role.VIRTUAL_ROUTER);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findBy(long accountId, long dcId, Role role) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", role);
        return listBy(sc);
    }


    @Override
    public List<DomainRouterVO> listBy(long accountId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByHostId(Long hostId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("host", hostId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByPodId(Long podId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("podId", podId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByPodIdAndStates(Long podId, State... states) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("podId", podId);
        sc.setParameters("states", (Object[]) states);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listIsolatedByHostId(Long hostId) {
        SearchCriteria<DomainRouterVO> sc = HostUpSearch.create();
        if (hostId != null) {
            sc.setParameters("host", hostId);
        }
        sc.setJoinParameters("networkRouter", "type", Network.GuestType.Isolated);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByDomain(Long domainId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findByNetwork(long networkId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByLastHostId(Long hostId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("lastHost", hostId);
        sc.setParameters("state", State.Stopped);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listActive(long networkId) {
        SearchCriteria<DomainRouterVO> sc = IdNetworkIdStatesSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("states", State.Running, State.Migrating, State.Stopping, State.Starting);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByStateAndNetworkType(State state, Network.GuestType type, long mgmtSrvrId) {
        SearchCriteria<DomainRouterVO> sc = StateNetworkTypeSearch.create();
        sc.setParameters("state", state);
        sc.setJoinParameters("networkRouter", "type", type);
        sc.setJoinParameters("host", "mgmtServerId", mgmtSrvrId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> findByNetworkOutsideThePod(long networkId, long podId, State state, Role role) {
        SearchCriteria<DomainRouterVO> sc = OutsidePodSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("podId", podId);
        sc.setParameters("state", state);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByNetworkAndPodAndRole(long networkId, long podId, Role role) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("podId", podId);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByNetworkAndRole(long networkId, Role role) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("networkRouter", "networkId", networkId);
        sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByElementId(long elementId) {
        SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("elementId", elementId);
        return listBy(sc);
    }
    
    @Override
    @DB
    public DomainRouterVO persist(DomainRouterVO router, List<Network> guestNetworks) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        // 1) create network
        DomainRouterVO newRouter = super.persist(router);
        
        if (guestNetworks != null && !guestNetworks.isEmpty()) {
            // 2) add router to the network
            for (Network guestNetwork : guestNetworks) {
                addRouterToGuestNetwork(router, guestNetwork);  
            }
        }
       
        txn.commit();
        return newRouter;
    }
    
    @Override
    @DB
    public void addRouterToGuestNetwork(VirtualRouter router, Network guestNetwork) {
        if (_routerNetworkDao.findByRouterAndNetwork(router.getId(), guestNetwork.getId()) == null) {
            NetworkOffering off = _offDao.findById(guestNetwork.getNetworkOfferingId());
            if (!(off.getName().equalsIgnoreCase(NetworkOffering.SystemPrivateGatewayNetworkOffering))) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                //1) add router to network
                RouterNetworkVO routerNtwkMap = new RouterNetworkVO(router.getId(), guestNetwork.getId(), guestNetwork.getGuestType());
                _routerNetworkDao.persist(routerNtwkMap);
                //2) create user stats entry for the network
                UserStatisticsVO stats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), 
                        guestNetwork.getId(), null, router.getId(), router.getType().toString());
                if (stats == null) {
                    stats = new UserStatisticsVO(router.getAccountId(), router.getDataCenterId(), null, router.getId(),
                            router.getType().toString(), guestNetwork.getId());
                    _userStatsDao.persist(stats);
                }
                txn.commit();
            }
        }  
    }
    
    @Override
    public void removeRouterFromGuestNetwork(long routerId, long guestNetworkId) {
        RouterNetworkVO routerNtwkMap = _routerNetworkDao.findByRouterAndNetwork(routerId, guestNetworkId);
        if (routerNtwkMap != null) {
            _routerNetworkDao.remove(routerNtwkMap.getId());
        }
    }
    
    @Override
    public List<Long> getRouterNetworks(long routerId) {
        return _routerNetworkDao.getRouterNetworks(routerId);
    }
    
    @Override
    public List<DomainRouterVO> listByVpcId(long vpcId) {
        SearchCriteria<DomainRouterVO> sc = VpcSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("role", Role.VIRTUAL_ROUTER);
        return listBy(sc);
    }

}
