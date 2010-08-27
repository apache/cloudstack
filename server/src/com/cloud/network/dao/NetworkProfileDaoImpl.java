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
import com.cloud.network.NetworkProfileVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=NetworkProfileDao.class)
public class NetworkProfileDaoImpl extends GenericDaoBase<NetworkProfileVO, Long> implements NetworkProfileDao {
    final SearchBuilder<NetworkProfileVO> ProfileSearch;
    final SearchBuilder<NetworkProfileVO> AccountSearch;
    
    protected NetworkProfileDaoImpl() {
        super();
        
        ProfileSearch = createSearchBuilder();
        ProfileSearch.and("account", ProfileSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ProfileSearch.and("trafficType", ProfileSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("cidr", ProfileSearch.entity().getCidr(), SearchCriteria.Op.EQ);
        ProfileSearch.and("broadcastType", ProfileSearch.entity().getBroadcastDomainType(), SearchCriteria.Op.EQ);
        ProfileSearch.done();
        
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
    }
    
    public NetworkProfileVO findBy(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastType,  long accountId) {
        SearchCriteria<NetworkProfileVO> sc = ProfileSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        
        return null;
        
    }
    
    @Override
    public List<NetworkProfileVO> listBy(long accountId) {
        SearchCriteria<NetworkProfileVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        
        return listActiveBy(sc);
    }
}
