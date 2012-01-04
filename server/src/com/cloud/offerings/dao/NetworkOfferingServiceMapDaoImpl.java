/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.offerings.dao;


import java.util.List;

import javax.ejb.Local;

import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.NetworkServiceMapVO;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.Provider;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Local(value=NetworkOfferingServiceMapDao.class) @DB(txn=false)
public class NetworkOfferingServiceMapDaoImpl extends GenericDaoBase<NetworkOfferingServiceMapVO, Long> implements NetworkOfferingServiceMapDao {
    final SearchBuilder<NetworkOfferingServiceMapVO> AllFieldsSearch;
    final SearchBuilder<NetworkOfferingServiceMapVO> MultipleServicesSearch;
    final GenericSearchBuilder<NetworkOfferingServiceMapVO, String> ProvidersSearch;
    final GenericSearchBuilder<NetworkOfferingServiceMapVO, String> ServicesSearch;
    
    protected NetworkOfferingServiceMapDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("networkOfferingId", AllFieldsSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        MultipleServicesSearch = createSearchBuilder();
        MultipleServicesSearch.and("networkOfferingId", MultipleServicesSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.and("service", MultipleServicesSearch.entity().getService(), SearchCriteria.Op.IN);
        MultipleServicesSearch.and("provider", MultipleServicesSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.done();
        
        ProvidersSearch = createSearchBuilder(String.class);
        ProvidersSearch.and("networkOfferingId", ProvidersSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        ProvidersSearch.and("service", ProvidersSearch.entity().getService(), SearchCriteria.Op.EQ);
        ProvidersSearch.select(null, Func.DISTINCT, ProvidersSearch.entity().getProvider());
        ProvidersSearch.done();
        
        ServicesSearch = createSearchBuilder(String.class);
        ServicesSearch.and("networkOfferingId", ServicesSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        ServicesSearch.select(null, Func.DISTINCT, ServicesSearch.entity().getService());
        ServicesSearch.done();
    }
    
    @Override
    public boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = MultipleServicesSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        
        if (services != null) {
            String[] servicesStr = new String[services.length];
            
            int i = 0;
            for (Service service : services) {
                servicesStr[i] = service.getName();
                i++;
            }
            
            sc.setParameters("service", (Object[])servicesStr);
        }
        
        List<NetworkOfferingServiceMapVO> offeringServices = listBy(sc);
        
        if (services != null) {
            if (offeringServices.size() == services.length) {
                return true;
            }
        } else if (!offeringServices.isEmpty()) {
            return true;
        }
        
        return false;
    }
 
    @Override
    public List<NetworkOfferingServiceMapVO> listByNetworkOfferingId(long networkOfferingId) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        return listBy(sc);
    }
    
    @Override
    public void deleteByOfferingId(long networkOfferingId) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        remove(sc);
    }
    
    @Override
    public List<String> listProvidersForServiceForNetworkOffering(long networkOfferingId, Service service) {
    	SearchCriteria<String> sc = ProvidersSearch.create();;
    	
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());
        
        return customSearch(sc, null);
    }

    @Override
    public boolean isProviderForNetworkOffering(long networkOfferingId, Provider provider) {
    	SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();;
    	
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("provider", provider.getName());
        
        if (findOneBy(sc) != null) {
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> listServicesForNetworkOffering(long networkOfferingId) {
    	SearchCriteria<String> sc = ServicesSearch.create();;
        sc.setParameters("networkOfferingId", networkOfferingId);
        
        return customSearch(sc, null);
    }
}
