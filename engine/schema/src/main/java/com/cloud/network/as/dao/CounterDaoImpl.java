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

package com.cloud.network.as.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.as.CounterVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class CounterDaoImpl extends GenericDaoBase<CounterVO, Long> implements CounterDao {
    final SearchBuilder<CounterVO> AllFieldsSearch;

    protected CounterDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), Op.LIKE);
        AllFieldsSearch.and("source", AllFieldsSearch.entity().getSource(), Op.EQ);
        AllFieldsSearch.and("provider", AllFieldsSearch.entity().getProvider(), Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public List<CounterVO> listCounters(Long id, String name, String source, String provider, String keyword, Filter filter) {
        SearchCriteria<CounterVO> sc = AllFieldsSearch.create();

        if (keyword != null) {
            SearchCriteria<CounterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (source != null) {
            sc.setParameters("source", source);
        }
        if (provider != null) {
            sc.setParameters("provider", provider);
        }
        return listBy(sc, filter);
    }

}
