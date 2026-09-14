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
package com.cloud.vm.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class NicNetworkMapDaoImpl extends GenericDaoBase<NicNetworkMapVO, Long> implements NicNetworkMapDao {

    private final SearchBuilder<NicNetworkMapVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicNetworkMapVO, Long> NicIdsByNetworkSearch;

    public NicNetworkMapDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("nicId", AllFieldsSearch.entity().getNicId(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.done();

        NicIdsByNetworkSearch = createSearchBuilder(Long.class);
        NicIdsByNetworkSearch.select(null, Func.DISTINCT, NicIdsByNetworkSearch.entity().getNicId());
        NicIdsByNetworkSearch.and("networkId", NicIdsByNetworkSearch.entity().getNetworkId(), Op.EQ);
        NicIdsByNetworkSearch.done();
    }

    @Override
    public List<NicNetworkMapVO> listByNicId(long nicId) {
        SearchCriteria<NicNetworkMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        return listBy(sc);
    }

    @Override
    public List<NicNetworkMapVO> listByNetworkId(long networkId) {
        SearchCriteria<NicNetworkMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public NicNetworkMapVO findByNicIdAndNetworkId(long nicId, long networkId) {
        SearchCriteria<NicNetworkMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public List<Long> listNicIdsByNetworkId(long networkId) {
        SearchCriteria<Long> sc = NicIdsByNetworkSearch.create();
        sc.setParameters("networkId", networkId);
        return customSearch(sc, null);
    }
}
