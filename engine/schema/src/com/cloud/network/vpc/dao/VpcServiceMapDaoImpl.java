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

import javax.ejb.Local;

import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.vpc.VpcServiceMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

@Component
@Local(value=VpcServiceMapDao.class) @DB()
public class VpcServiceMapDaoImpl extends GenericDaoBase<VpcServiceMapVO, Long> implements VpcServiceMapDao {
    final SearchBuilder<VpcServiceMapVO> AllFieldsSearch;
    final SearchBuilder<VpcServiceMapVO> MultipleServicesSearch;
    final GenericSearchBuilder<VpcServiceMapVO, String> DistinctProvidersSearch;

    protected VpcServiceMapDaoImpl(){
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        MultipleServicesSearch = createSearchBuilder();
        MultipleServicesSearch.and("vpcId", MultipleServicesSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.and("service", MultipleServicesSearch.entity().getService(), SearchCriteria.Op.IN);
        MultipleServicesSearch.and("provider", MultipleServicesSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        MultipleServicesSearch.done();

        DistinctProvidersSearch = createSearchBuilder(String.class);
        DistinctProvidersSearch.and("vpcId", DistinctProvidersSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.and("provider", DistinctProvidersSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        DistinctProvidersSearch.selectFields(DistinctProvidersSearch.entity().getProvider());
        DistinctProvidersSearch.done();
    }

    @Override
    public boolean areServicesSupportedInVpc(long vpcId, Service... services) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canProviderSupportServiceInVpc(long vpcId, Service service,
                                                  Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<NetworkServiceMapVO> getServicesInVpc(long vpcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProviderForServiceInVpc(long vpcId, Service service) {
        SearchCriteria<VpcServiceMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("service", service.getName());
        VpcServiceMapVO ntwkSvc = findOneBy(sc);
        if (ntwkSvc == null) {
            throw new UnsupportedServiceException("Service " + service.getName() + " is not supported in the vpc id=" + vpcId);
        }

        return ntwkSvc.getProvider();
    }

    @Override
    public void deleteByVpcId(long vpcId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> getDistinctProviders(long vpcId) {
        SearchCriteria<String> sc = DistinctProvidersSearch.create();
        sc.setParameters("vpcId", vpcId);
        List<String> results = customSearch(sc, null);
        return results;
    }

    @Override
    public String isProviderForVpc(long vpcId, Provider provider) {
        // TODO Auto-generated method stub
        return null;
    }

}
