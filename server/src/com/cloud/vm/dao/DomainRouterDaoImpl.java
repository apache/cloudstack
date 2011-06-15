/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.Network.GuestIpType;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.NetworkDaoImpl;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine.State;

@Local(value = { DomainRouterDao.class })
public class DomainRouterDaoImpl extends GenericDaoBase<DomainRouterVO, Long> implements DomainRouterDao {
    private static final Logger s_logger = Logger.getLogger(DomainRouterDaoImpl.class);

    protected final SearchBuilder<DomainRouterVO> AllFieldsSearch;
    protected final SearchBuilder<DomainRouterVO> IdNetworkIdStatesSearch;
    protected final SearchBuilder<DomainRouterVO> HostUpSearch;
    protected final SearchBuilder<DomainRouterVO> StateNetworkTypeSearch;
    NetworkDaoImpl _networksDao = ComponentLocator.inject(NetworkDaoImpl.class);

    protected DomainRouterDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("dc", AllFieldsSearch.entity().getDataCenterIdToDeployIn(), Op.EQ);
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("role", AllFieldsSearch.entity().getRole(), Op.EQ);
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), Op.EQ);
        AllFieldsSearch.and("host", AllFieldsSearch.entity().getHostId(), Op.EQ);
        AllFieldsSearch.and("lastHost", AllFieldsSearch.entity().getLastHostId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("podId", AllFieldsSearch.entity().getPodIdToDeployIn(), Op.EQ);
        AllFieldsSearch.done();

        IdNetworkIdStatesSearch = createSearchBuilder();
        IdNetworkIdStatesSearch.and("id", IdNetworkIdStatesSearch.entity().getId(), Op.EQ);
        IdNetworkIdStatesSearch.and("network", IdNetworkIdStatesSearch.entity().getNetworkId(), Op.EQ);
        IdNetworkIdStatesSearch.and("states", IdNetworkIdStatesSearch.entity().getState(), Op.IN);
        IdNetworkIdStatesSearch.done();

        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), Op.NIN);
        SearchBuilder<NetworkVO> joinNetwork = _networksDao.createSearchBuilder();
        joinNetwork.and("guestType", joinNetwork.entity().getGuestType(), Op.EQ);
        HostUpSearch.join("network", joinNetwork, joinNetwork.entity().getId(), HostUpSearch.entity().getNetworkId(), JoinType.INNER);
        HostUpSearch.done();
        
        StateNetworkTypeSearch = createSearchBuilder();
        StateNetworkTypeSearch.and("state", StateNetworkTypeSearch.entity().getState(), Op.EQ);
        SearchBuilder<NetworkVO> joinStateNetwork = _networksDao.createSearchBuilder();
        joinStateNetwork.and("guestType", joinStateNetwork.entity().getGuestType(), Op.EQ);
        StateNetworkTypeSearch.join("network", joinStateNetwork, joinStateNetwork.entity().getId(), StateNetworkTypeSearch.entity().getNetworkId(), JoinType.INNER);
        StateNetworkTypeSearch.done();

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
        sc.setParameters("role", Role.DHCP_FIREWALL_LB_PASSWD_USERDATA);
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
    public List<DomainRouterVO> listVirtualUpByHostId(Long hostId) {
        SearchCriteria<DomainRouterVO> sc = HostUpSearch.create();
        if (hostId != null) {
            sc.setParameters("host", hostId);
        }
        sc.setParameters("states", State.Destroyed, State.Stopped, State.Expunging);
        sc.setJoinParameters("network", "guestType", GuestIpType.Virtual);
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
        sc.setParameters("network", networkId);
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
	public List<DomainRouterVO> findByNetworkAndPod(long networkId, long podId) {
	    SearchCriteria<DomainRouterVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("podId", podId);
        return listBy(sc);
	}

    @Override
    public List<DomainRouterVO> listActive(long networkId) {
        SearchCriteria<DomainRouterVO> sc = IdNetworkIdStatesSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("states", State.Running, State.Migrating, State.Stopping, State.Starting);
        return listBy(sc);
    }
    
    @Override
    public List<DomainRouterVO> listByStateAndNetworkType(State state, GuestIpType ipType) {
        SearchCriteria<DomainRouterVO> sc = StateNetworkTypeSearch.create();
        sc.setParameters("state", state);
        sc.setJoinParameters("network", "guestType", ipType);
        return listBy(sc);
    }
}
