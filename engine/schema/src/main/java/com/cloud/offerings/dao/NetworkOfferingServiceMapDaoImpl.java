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
package com.cloud.offerings.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
@DB()
public class NetworkOfferingServiceMapDaoImpl extends GenericDaoBase<NetworkOfferingServiceMapVO, Long> implements NetworkOfferingServiceMapDao {

    final SearchBuilder<NetworkOfferingServiceMapVO> AllFieldsSearch;
    final SearchBuilder<NetworkOfferingServiceMapVO> MultipleServicesSearch;
    final GenericSearchBuilder<NetworkOfferingServiceMapVO, String> ProvidersSearch;
    final GenericSearchBuilder<NetworkOfferingServiceMapVO, String> ServicesSearch;
    final GenericSearchBuilder<NetworkOfferingServiceMapVO, String> DistinctProvidersSearch;

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

        DistinctProvidersSearch = createSearchBuilder(String.class);
        DistinctProvidersSearch.and("offId", DistinctProvidersSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.and("provider", DistinctProvidersSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.selectFields(DistinctProvidersSearch.entity().getProvider());
        DistinctProvidersSearch.done();
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
    public boolean canProviderSupportServiceInNetworkOffering(long networkOfferingId, Service service, Provider provider) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());
        sc.setParameters("provider", provider.getName());
        return findOneBy(sc) != null;
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
        SearchCriteria<String> sc = ProvidersSearch.create();

        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("service", service.getName());

        return customSearch(sc, null);
    }

    @Override
    public boolean isProviderForNetworkOffering(long networkOfferingId, Provider provider) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();

        sc.setParameters("networkOfferingId", networkOfferingId);
        sc.setParameters("provider", provider.getName());

        return findOneBy(sc) != null;
    }

    @Override
    public List<String> listServicesForNetworkOffering(long networkOfferingId) {
        SearchCriteria<String> sc = ServicesSearch.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        return customSearch(sc, null);
    }

    @Override
    public NetworkOfferingServiceMapVO persist(NetworkOfferingServiceMapVO entity) {
        SearchCriteria<NetworkOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkOfferingId", entity.getNetworkOfferingId());
        sc.setParameters("service", entity.getService());
        sc.setParameters("provider", entity.getProvider());
        NetworkOfferingServiceMapVO mappingInDb = findOneBy(sc);
        return mappingInDb != null ? mappingInDb : super.persist(entity);
    }

    @Override
    public List<String> getDistinctProviders(long offId) {
        SearchCriteria<String> sc = DistinctProvidersSearch.create();
        sc.setParameters("offId", offId);
        List<String> results = customSearch(sc, null);
        return results;
    }
}
