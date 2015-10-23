//
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
//

package com.cloud.network.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = NiciraNvpRouterMappingDao.class)
public class NiciraNvpRouterMappingDaoImpl extends GenericDaoBase<NiciraNvpRouterMappingVO, Long> implements NiciraNvpRouterMappingDao {

    protected final SearchBuilder<NiciraNvpRouterMappingVO> networkSearch;

    public NiciraNvpRouterMappingDaoImpl() {
        networkSearch = createSearchBuilder();
        networkSearch.and("network_id", networkSearch.entity().getNetworkId(), Op.EQ);
        networkSearch.done();
    }

    @Override
    public NiciraNvpRouterMappingVO findByNetworkId(final long id) {
        SearchCriteria<NiciraNvpRouterMappingVO> sc = networkSearch.create();
        sc.setParameters("network_id", id);
        return findOneBy(sc);
    }

}
