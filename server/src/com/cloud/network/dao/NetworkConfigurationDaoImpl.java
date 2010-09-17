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
package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkAccountDaoImpl;
import com.cloud.network.NetworkAccountVO;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value=NetworkConfigurationDao.class) @DB(txn=false)
public class NetworkConfigurationDaoImpl extends GenericDaoBase<NetworkConfigurationVO, Long> implements NetworkConfigurationDao {
    final SearchBuilder<NetworkConfigurationVO> ProfileSearch;
    final SearchBuilder<NetworkConfigurationVO> AccountSearch;
    final SearchBuilder<NetworkConfigurationVO> OfferingSearch;
    
    NetworkAccountDaoImpl _accountsDao = new NetworkAccountDaoImpl();
    
    protected NetworkConfigurationDaoImpl() {
        super();
        
        ProfileSearch = createSearchBuilder();
        ProfileSearch.and("trafficType", ProfileSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("cidr", ProfileSearch.entity().getCidr(), SearchCriteria.Op.EQ);
        ProfileSearch.and("broadcastType", ProfileSearch.entity().getBroadcastDomainType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("offering", ProfileSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        ProfileSearch.and("datacenter", ProfileSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ProfileSearch.done();
        
        AccountSearch = createSearchBuilder();
        AccountSearch.and("offering", AccountSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkAccountVO> join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.join("accounts", join, AccountSearch.entity().getId(), join.entity().getNetworkConfigurationId());
        AccountSearch.and("datacenter", AccountSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
    
        OfferingSearch = createSearchBuilder();
        OfferingSearch.and("offering", OfferingSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        OfferingSearch.and("datacenter", OfferingSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
    }
    
    public List<NetworkConfigurationVO> findBy(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastType, long networkOfferingId, long dataCenterId) {
        SearchCriteria<NetworkConfigurationVO> sc = ProfileSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        sc.setParameters("offering", networkOfferingId);
        sc.setParameters("datacenter", dataCenterId);
        
        return search(sc, null);
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        sc.setJoinParameters("accounts", "account", accountId);
        
        return listBy(sc);
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId, long offeringId, long dataCenterId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setJoinParameters("accounts", "account", accountId);
        sc.setParameters("datacenter", dataCenterId);
        
        return listBy(sc);
    }
    
    @Override
    public NetworkConfigurationVO persist(NetworkConfigurationVO config) {
        throw new UnsupportedOperationException("Use the persist for NetworkConfigurationDao");
    }
    
    @Override @DB
    public NetworkConfigurationVO persist(NetworkConfigurationVO config, long accountId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        config = super.persist(config);
        addAccountToNetworkConfiguration(config.getId(), accountId);
        txn.commit();
        return config;
    }
    
    @Override
    public void addAccountToNetworkConfiguration(long configurationId, long accountId) {
        NetworkAccountVO account = new NetworkAccountVO(configurationId, accountId);
        _accountsDao.persist(account);
    }
}
