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

/**
 * 
 */
package com.cloud.offerings.dao;


import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=NetworkOfferingServiceMapDao.class) @DB(txn=false)
public class NetworkOfferingServiceMapDaoImpl extends GenericDaoBase<NetworkOfferingServiceMapVO, Long> implements NetworkOfferingServiceMapDao {
    final SearchBuilder<NetworkOfferingServiceMapVO> AllFieldsSearch;
    
    protected NetworkOfferingServiceMapDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("networkOfferingId", AllFieldsSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public boolean isServiceSupported(long networkOfferingId, Service service) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean isProviderSupported(long networkOfferingId, Service service, Provider provider) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());
        sc.setParameters("provider", provider.getName());
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<String> getServicesForProvider(long networkOfferingId, Provider provider) {
        List<String> services = new ArrayList<String>();
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("provider", provider.getName());
        List<NetworkOfferingServiceMapVO> map = listBy(sc);
        for (NetworkOfferingServiceMapVO instance : map) {
            services.add(instance.getService());
        }
        
        return services;
    }
    
    @Override
    public List<String> getProvidersForService(long networkOfferingId, Service service) {
        List<String> providers = new ArrayList<String>();
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());
        List<NetworkOfferingServiceMapVO> map = listBy(sc);
        if (map.isEmpty()) {
            throw new UnsupportedServiceException("Service " + service + " is not supported by the network offering id=" + networkOfferingId);
        }
        
        for (NetworkOfferingServiceMapVO instance : map) {
            providers.add(instance.getProvider());
        }
        
        return providers;
    }
 
    @Override
    public List<NetworkOfferingServiceMapVO> getServices(long networkOfferingId) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        return listBy(sc);
    }
}
