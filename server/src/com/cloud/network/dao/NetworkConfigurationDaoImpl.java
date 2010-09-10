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
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=NetworkConfigurationDao.class)
public class NetworkConfigurationDaoImpl extends GenericDaoBase<NetworkConfigurationVO, Long> implements NetworkConfigurationDao {
    final SearchBuilder<NetworkConfigurationVO> ProfileSearch;
    final SearchBuilder<NetworkConfigurationVO> AccountSearch;
    NetworkAccountDaoImpl _accountsDao = new NetworkAccountDaoImpl();
    
    protected NetworkConfigurationDaoImpl() {
        super();
        
        ProfileSearch = createSearchBuilder();
        ProfileSearch.and("trafficType", ProfileSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("cidr", ProfileSearch.entity().getCidr(), SearchCriteria.Op.EQ);
        ProfileSearch.and("broadcastType", ProfileSearch.entity().getBroadcastDomainType(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkAccountVO> join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), SearchCriteria.Op.EQ);
        ProfileSearch.join("accounts", join, ProfileSearch.entity().getId(), join.entity().getNetworkConfigurationId());
        ProfileSearch.done();
        
        AccountSearch = createSearchBuilder();
        AccountSearch.and("offering", AccountSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.join("accounts", join, AccountSearch.entity().getId(), join.entity().getNetworkConfigurationId());
        AccountSearch.done();
        
        
    }
    
    public NetworkConfigurationVO findBy(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastType, long accountId) {
        SearchCriteria<NetworkConfigurationVO> sc = ProfileSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        sc.setJoinParameters("accounts", "account", accountId);
        
        return null;
        
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        sc.setJoinParameters("accounts", "account", accountId);
        
        return listActiveBy(sc);
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId, long offeringId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setJoinParameters("accounts", "account", accountId);
        
        return listActiveBy(sc);
    }
}
