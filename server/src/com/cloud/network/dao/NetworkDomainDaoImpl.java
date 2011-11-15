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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.network.NetworkDomainVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=NetworkDomainDao.class) @DB(txn=false)
public class NetworkDomainDaoImpl extends GenericDaoBase<NetworkDomainVO, Long> implements NetworkDomainDao {
    final SearchBuilder<NetworkDomainVO> AllFieldsSearch;
    final SearchBuilder<NetworkDomainVO> DomainsSearch;
    
    protected NetworkDomainDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.done();
        
        DomainsSearch = createSearchBuilder();
        DomainsSearch.and("domainId", DomainsSearch.entity().getDomainId(), Op.IN);
        DomainsSearch.done();
    }
    
    @Override
    public List<NetworkDomainVO> listDomainNetworkMapByDomain(Object... domainId) {
        SearchCriteria<NetworkDomainVO> sc = DomainsSearch.create();
        sc.setParameters("domainId", (Object[])domainId);
        
        return listBy(sc);
    }
    
    @Override
    public NetworkDomainVO getDomainNetworkMapByNetworkId(long networkId) {
        SearchCriteria<NetworkDomainVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }
    
    @Override
    public List<Long> listNetworkIdsByDomain(long domainId) {
        List<Long> networkIdsToReturn = new ArrayList<Long>();
        List<NetworkDomainVO> maps = listDomainNetworkMapByDomain(domainId);
        for (NetworkDomainVO map : maps) {
            networkIdsToReturn.add(map.getNetworkId());
        } 
        return networkIdsToReturn;
    }
}
