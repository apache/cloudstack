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
package com.cloud.simulator.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.simulator.MockVolumeVO;
import com.cloud.simulator.MockVolumeVO.MockVolumeType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
public class MockVolumeDaoImpl extends GenericDaoBase<MockVolumeVO, Long> implements MockVolumeDao {
    protected final SearchBuilder<MockVolumeVO> idTypeSearch;
    protected final SearchBuilder<MockVolumeVO> pathTypeSearch;
    protected final SearchBuilder<MockVolumeVO> namePoolSearch;
    protected final SearchBuilder<MockVolumeVO> nameSearch;
    protected final GenericSearchBuilder<MockVolumeVO, Long> totalSearch;

    @Override
    public List<MockVolumeVO> findByStorageIdAndType(long id, MockVolumeType type) {
        SearchCriteria<MockVolumeVO> sc = idTypeSearch.create();
        sc.setParameters("storageId", id);
        sc.setParameters("type", type);
        return listBy(sc);
    }

    @Override
    public Long findTotalStorageId(long id) {
        SearchCriteria<Long> sc = totalSearch.create();

        sc.setParameters("poolId", id);
        return customSearch(sc, null).get(0);
    }

    @Override
    public MockVolumeVO findByStoragePathAndType(String path) {
        SearchCriteria<MockVolumeVO> sc = pathTypeSearch.create();
        sc.setParameters("path", "%" + path + "%");
        return findOneBy(sc);
    }

    @Override
    public MockVolumeVO findByNameAndPool(String volumeName, String poolUUID) {
        SearchCriteria<MockVolumeVO> sc = namePoolSearch.create();
        sc.setParameters("name", volumeName);
        sc.setParameters("poolUuid", poolUUID);
        return findOneBy(sc);
    }

    @Override
    public MockVolumeVO findByName(String volumeName) {
        SearchCriteria<MockVolumeVO> sc = nameSearch.create();
        sc.setParameters("name", volumeName);
        return findOneBy(sc);
    }

    public MockVolumeDaoImpl() {
        idTypeSearch = createSearchBuilder();
        idTypeSearch.and("storageId", idTypeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        idTypeSearch.and("type", idTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        idTypeSearch.done();

        pathTypeSearch = createSearchBuilder();
        pathTypeSearch.and("path", pathTypeSearch.entity().getPath(), SearchCriteria.Op.LIKE);
        pathTypeSearch.done();

        namePoolSearch = createSearchBuilder();
        namePoolSearch.and("name", namePoolSearch.entity().getName(), SearchCriteria.Op.EQ);
        namePoolSearch.and("poolUuid", namePoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        namePoolSearch.done();

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getName(), SearchCriteria.Op.EQ);
        nameSearch.done();

        totalSearch = createSearchBuilder(Long.class);
        totalSearch.select(null, Func.SUM, totalSearch.entity().getSize());
        totalSearch.and("poolId", totalSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        totalSearch.done();

    }
}
