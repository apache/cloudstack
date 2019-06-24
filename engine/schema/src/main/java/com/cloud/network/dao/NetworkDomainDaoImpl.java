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

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB()
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
        sc.setParameters("domainId", domainId);

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
