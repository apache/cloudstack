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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VmwareCbtMigrationCycleVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VmwareCbtMigrationCycleDaoImpl extends GenericDaoBase<VmwareCbtMigrationCycleVO, Long> implements VmwareCbtMigrationCycleDao {

    private final SearchBuilder<VmwareCbtMigrationCycleVO> migrationSearch;

    public VmwareCbtMigrationCycleDaoImpl() {
        migrationSearch = createSearchBuilder();
        migrationSearch.and("migrationId", migrationSearch.entity().getMigrationId(), SearchCriteria.Op.EQ);
        migrationSearch.done();
    }

    @Override
    public List<VmwareCbtMigrationCycleVO> listByMigrationId(long migrationId) {
        SearchCriteria<VmwareCbtMigrationCycleVO> sc = migrationSearch.create();
        sc.setParameters("migrationId", migrationId);
        Filter filter = new Filter(VmwareCbtMigrationCycleVO.class, "cycleNumber", true, null, null);
        return listBy(sc, filter);
    }
}
