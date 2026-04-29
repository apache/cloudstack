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
package org.apache.cloudstack.secstorage.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.secstorage.HeuristicVO;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SecondaryStorageHeuristicDaoImpl extends GenericDaoBase<HeuristicVO, Long> implements SecondaryStorageHeuristicDao {
    private SearchBuilder<HeuristicVO> zoneAndTypeSearch;

    @PostConstruct
    public void init() {
        zoneAndTypeSearch = createSearchBuilder();
        zoneAndTypeSearch.and("zoneId", zoneAndTypeSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        zoneAndTypeSearch.and("type", zoneAndTypeSearch.entity().getType(), SearchCriteria.Op.IN);
        zoneAndTypeSearch.done();
    }

    @Override
    public HeuristicVO findByZoneIdAndType(long zoneId, HeuristicType type) {
        SearchCriteria<HeuristicVO> searchCriteria = zoneAndTypeSearch.create();
        searchCriteria.setParameters("zoneId", zoneId);
        searchCriteria.setParameters("type", type.toString());
        final Filter filter = new Filter(HeuristicVO.class, "created", false);

        return findOneBy(searchCriteria, filter);
    }
}
