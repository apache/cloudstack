/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=PhysicalNetworkServiceProviderDao.class) @DB(txn=false)
public class PhysicalNetworkServiceProviderDaoImpl extends GenericDaoBase<PhysicalNetworkServiceProviderVO, Long> implements PhysicalNetworkServiceProviderDao {
    final SearchBuilder<PhysicalNetworkServiceProviderVO> physicalNetworkSearch;
    final SearchBuilder<PhysicalNetworkServiceProviderVO> physicalNetworkServiceProviderSearch;

    protected PhysicalNetworkServiceProviderDaoImpl() {
        super();
        physicalNetworkSearch = createSearchBuilder();
        physicalNetworkSearch.and("physicalNetworkId", physicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkSearch.done();

        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("physicalNetworkId", physicalNetworkServiceProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkServiceProviderSearch.and("serviceProvderType", physicalNetworkServiceProviderSearch.entity().getProviderName(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();
    }

    @Override
    public List<PhysicalNetworkServiceProviderVO> listBy(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public PhysicalNetworkServiceProviderVO findByServiceProvider(long physicalNetworkId, String providerType) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("serviceProvderType", providerType);
        return findOneBy(sc);
    }
    
    @Override
    public void deleteProviders(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        remove(sc);
    }
    
}
