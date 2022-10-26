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
package com.cloud.network.vpc.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.Network.Service;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
@DB()
public class VpcOfferingServiceMapDaoImpl extends GenericDaoBase<VpcOfferingServiceMapVO, Long> implements VpcOfferingServiceMapDao {
    final SearchBuilder<VpcOfferingServiceMapVO> AllFieldsSearch;
    final SearchBuilder<VpcOfferingServiceMapVO> MultipleServicesSearch;
    final GenericSearchBuilder<VpcOfferingServiceMapVO, String> ServicesSearch;

    protected VpcOfferingServiceMapDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("vpcOffId", AllFieldsSearch.entity().getVpcOfferingId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        MultipleServicesSearch = createSearchBuilder();
        MultipleServicesSearch.and("vpcOffId", MultipleServicesSearch.entity().getVpcOfferingId(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.and("service", MultipleServicesSearch.entity().getService(), SearchCriteria.Op.IN);
        MultipleServicesSearch.and("provider", MultipleServicesSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.done();

        ServicesSearch = createSearchBuilder(String.class);
        ServicesSearch.and("offeringId", ServicesSearch.entity().getVpcOfferingId(), SearchCriteria.Op.EQ);
        ServicesSearch.select(null, Func.DISTINCT, ServicesSearch.entity().getService());
        ServicesSearch.done();
    }

    @Override
    public List<VpcOfferingServiceMapVO> listByVpcOffId(long vpcOffId) {
        SearchCriteria<VpcOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcOffId", vpcOffId);
        return listBy(sc);
    }

    @Override
    public boolean areServicesSupportedByVpcOffering(long vpcOfferingId, Service... services) {
        SearchCriteria<VpcOfferingServiceMapVO> sc = MultipleServicesSearch.create();
        sc.setParameters("vpcOffId", vpcOfferingId);

        if (services != null) {
            String[] servicesStr = new String[services.length];

            int i = 0;
            for (Service service : services) {
                servicesStr[i] = service.getName();
                i++;
            }

            sc.setParameters("service", (Object[])servicesStr);
        }

        List<VpcOfferingServiceMapVO> offeringServices = listBy(sc);

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
    public List<String> listServicesForVpcOffering(long offId) {
        SearchCriteria<String> sc = ServicesSearch.create();
        sc.setParameters("offeringId", offId);
        return customSearch(sc, null);
    }

    @Override
    public VpcOfferingServiceMapVO findByServiceProviderAndOfferingId(String service, String provider, long vpcOfferingId) {
        SearchCriteria<VpcOfferingServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcOffId", vpcOfferingId);
        sc.setParameters("service", service);
        sc.setParameters("provider", provider);

        return findOneBy(sc);
    }
}
