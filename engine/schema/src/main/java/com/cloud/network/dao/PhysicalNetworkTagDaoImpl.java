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
public class PhysicalNetworkTagDaoImpl extends GenericDaoBase<PhysicalNetworkTagVO, Long> implements GenericDao<PhysicalNetworkTagVO, Long> {
    private final GenericSearchBuilder<PhysicalNetworkTagVO, String> TagSearch;
    private final SearchBuilder<PhysicalNetworkTagVO> AllFieldsSearch;

    protected PhysicalNetworkTagDaoImpl() {
        super();
        TagSearch = createSearchBuilder(String.class);
        TagSearch.selectFields(TagSearch.entity().getTag());
        TagSearch.and("physicalNetworkId", TagSearch.entity().getPhysicalNetworkId(), Op.EQ);
        TagSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.and("tag", AllFieldsSearch.entity().getTag(), Op.EQ);
        AllFieldsSearch.done();
    }

    public List<String> getTags(long physicalNetworkId) {
        SearchCriteria<String> sc = TagSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return customSearch(sc, null);
    }

    public int clearTags(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return remove(sc);
    }

}
