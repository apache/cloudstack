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

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class RouterNetworkDaoImpl extends GenericDaoBase<RouterNetworkVO, Long> implements RouterNetworkDao {
    protected final GenericSearchBuilder<RouterNetworkVO, Long> RouterNetworksSearch;
    protected final SearchBuilder<RouterNetworkVO> AllFieldsSearch;

    public RouterNetworkDaoImpl() {
        super();

        RouterNetworksSearch = createSearchBuilder(Long.class);
        RouterNetworksSearch.selectField(RouterNetworksSearch.entity().getNetworkId());
        RouterNetworksSearch.and("routerId", RouterNetworksSearch.entity().getRouterId(), Op.EQ);
        RouterNetworksSearch.done();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("routerId", AllFieldsSearch.entity().getRouterId(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.done();
    }
    
    public List<Long> getRouterNetworks(long routerId) {
        SearchCriteria<Long> sc = RouterNetworksSearch.create();
        sc.setParameters("routerId", routerId);
        return customSearch(sc, null);
    }
    
    public RouterNetworkVO findByRouterAndNetwork (long routerId, long networkId) {
        SearchCriteria<RouterNetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("routerId", routerId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }
    
}
