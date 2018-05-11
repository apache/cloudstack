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
package org.apache.cloudstack.br.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.br.BRProviderVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class BRProviderDaoImpl extends GenericDaoBase<BRProviderVO, Long> implements BRProviderDao {

    protected SearchBuilder<BRProviderVO> brProviderSearch;

    public BRProviderDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        brProviderSearch = createSearchBuilder();
        brProviderSearch.and("zoneid", brProviderSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        brProviderSearch.and("provider", brProviderSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        brProviderSearch.done();
    }

    @Override
    public List<BRProviderVO> listByZone(long zoneId) {
        SearchCriteria<BRProviderVO> sc = brProviderSearch.create();
        sc.setParameters("zoneid", zoneId);
        return listBy(sc);
    }

    @Override
    public List<BRProviderVO> listByZoneAndProvider(long zoneId, String provider) {
        SearchCriteria<BRProviderVO> sc = brProviderSearch.create();
        sc.setParameters("zoneid", zoneId);
        sc.setParameters("provider", provider);
        return listBy(sc);
    }
}
