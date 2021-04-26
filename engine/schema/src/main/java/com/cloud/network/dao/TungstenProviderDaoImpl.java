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

import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DB()
public class TungstenProviderDaoImpl extends GenericDaoBase<TungstenProviderVO, Long>
        implements TungstenProviderDao {

    final SearchBuilder<TungstenProviderVO> AllFieldsSearch;

    public TungstenProviderDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("hostname", AllFieldsSearch.entity().getHostname(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider_name", AllFieldsSearch.entity().getProviderName(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("port", AllFieldsSearch.entity().getPort(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("zone_id", AllFieldsSearch.entity().getZoneId(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("gateway", AllFieldsSearch.entity().getGateway(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vrouter_port", AllFieldsSearch.entity().getVrouterPort(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("introspect_port", AllFieldsSearch.entity().getIntrospectPort(),
            SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public TungstenProviderVO findByZoneId(long nspId) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("zone_id", nspId);
        return findOneBy(sc);
    }

    @Override
    public TungstenProviderVO findByUuid(String uuid) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public void deleteProviderByUuid(String providerUuid) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", providerUuid);
        remove(sc);
    }

    @Override
    public List<TungstenProviderVO> findAll() {
        return listAll();
    }
}
