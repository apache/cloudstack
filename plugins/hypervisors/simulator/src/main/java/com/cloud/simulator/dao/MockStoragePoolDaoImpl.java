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


import org.springframework.stereotype.Component;

import com.cloud.simulator.MockStoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class MockStoragePoolDaoImpl extends GenericDaoBase<MockStoragePoolVO, Long> implements MockStoragePoolDao {
    protected final SearchBuilder<MockStoragePoolVO> uuidSearch;
    protected final SearchBuilder<MockStoragePoolVO> hostguidSearch;

    @Override
    public MockStoragePoolVO findByUuid(String uuid) {
        SearchCriteria<MockStoragePoolVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    public MockStoragePoolDaoImpl() {
        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.done();

        hostguidSearch = createSearchBuilder();
        hostguidSearch.and("hostguid", hostguidSearch.entity().getHostGuid(), SearchCriteria.Op.EQ);
        hostguidSearch.and("type", hostguidSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        hostguidSearch.done();
    }

    @Override
    public MockStoragePoolVO findByHost(String hostUuid) {
        SearchCriteria<MockStoragePoolVO> sc = hostguidSearch.create();
        sc.setParameters("hostguid", hostUuid);
        sc.setParameters("type", StoragePoolType.Filesystem);
        return findOneBy(sc);
    }

}
