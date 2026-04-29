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

package com.cloud.hypervisor.vmware.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.hypervisor.vmware.LegacyZoneVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB
public class LegacyZoneDaoImpl extends GenericDaoBase<LegacyZoneVO, Long> implements LegacyZoneDao {

    final SearchBuilder<LegacyZoneVO> zoneSearch;
    final SearchBuilder<LegacyZoneVO> fullTableSearch;

    public LegacyZoneDaoImpl() {
        super();

        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), Op.EQ);
        zoneSearch.done();

        fullTableSearch = createSearchBuilder();
        fullTableSearch.done();
    }

    @Override
    public LegacyZoneVO findByZoneId(Long zoneId) {
        SearchCriteria<LegacyZoneVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        return findOneBy(sc);
    }

    @Override
    public List<LegacyZoneVO> listAllLegacyZones() {
        SearchCriteria<LegacyZoneVO> sc = fullTableSearch.create();
        return search(sc, null);
    }

}
