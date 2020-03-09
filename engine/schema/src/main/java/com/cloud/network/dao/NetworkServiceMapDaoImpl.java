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
package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB()
public class NetworkServiceMapDaoImpl extends GenericDaoBase<NetworkServiceMapVO, Long> implements NetworkServiceMapDao {
    final SearchBuilder<NetworkServiceMapVO> AllFieldsSearch;
    final SearchBuilder<NetworkServiceMapVO> MultipleServicesSearch;
    final GenericSearchBuilder<NetworkServiceMapVO, String> DistinctProvidersSearch;

    protected NetworkServiceMapDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        MultipleServicesSearch = createSearchBuilder();
        MultipleServicesSearch.and("networkId", MultipleServicesSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.and("service", MultipleServicesSearch.entity().getService(), SearchCriteria.Op.IN);
        MultipleServicesSearch.and("provider", MultipleServicesSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.done();

        DistinctProvidersSearch = createSearchBuilder(String.class);
        DistinctProvidersSearch.and("networkId", DistinctProvidersSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.and("provider", DistinctProvidersSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.and("service", DistinctProvidersSearch.entity().getService(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.selectFields(DistinctProvidersSearch.entity().getProvider());
        DistinctProvidersSearch.done();
    }

    @Override
    public boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        SearchCriteria<NetworkServiceMapVO> sc = MultipleServicesSearch.create();
        sc.setParameters("networkId", networkId);

        if (services != null) {
            String[] servicesStr = new String[services.length];

            int i = 0;
            for (Service service : services) {
                servicesStr[i] = service.getName();
                i++;
            }

            sc.setParameters("service", (Object[])servicesStr);
        }

        List<NetworkServiceMapVO> networkServices = listBy(sc);

        if (services != null) {
            if (networkServices.size() == services.length) {
                return true;
            }
        } else if (!networkServices.isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canProviderSupportServiceInNetwork(long networkId, Service service, Provider provider) {
        SearchCriteria<NetworkServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("service", service.getName());
        sc.setParameters("provider", provider.getName());
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    protected List<String> getServicesForProviderInNetwork(long networkId, Provider provider) {
        List<String> services = new ArrayList<String>();
        SearchCriteria<NetworkServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("provider", provider.getName());
        List<NetworkServiceMapVO> map = listBy(sc);
        for (NetworkServiceMapVO instance : map) {
            services.add(instance.getService());
        }

        return services;
    }

    @Override
    public String getProviderForServiceInNetwork(long networkId, Service service) {
        SearchCriteria<NetworkServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("service", service.getName());
        NetworkServiceMapVO ntwkSvc = findOneBy(sc);
        if (ntwkSvc == null) {
            throw new UnsupportedServiceException("Service " + service.getName() + " is not supported in the network id=" + networkId);
        }

        return ntwkSvc.getProvider();
    }

    @Override
    public List<NetworkServiceMapVO> getServicesInNetwork(long networkId) {
        SearchCriteria<NetworkServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public void deleteByNetworkId(long networkId) {
        SearchCriteria<NetworkServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        remove(sc);
    }

    @Override
    public List<String> getDistinctProviders(long networkId) {
        SearchCriteria<String> sc = DistinctProvidersSearch.create();
        sc.setParameters("networkId", networkId);
        List<String> results = customSearch(sc, null);
        return results;
    }

    @Override
    public String isProviderForNetwork(long networkId, Provider provider) {
        SearchCriteria<String> sc = DistinctProvidersSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("provider", provider.getName());
        List<String> results = customSearch(sc, null);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

    @Override
    public List<String> getProvidersForServiceInNetwork(long networkId, Service service) {
        SearchCriteria<String> sc = DistinctProvidersSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("service", service.getName());
        return customSearch(sc, null);
    }

}
