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

import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class MockHostDaoImpl extends GenericDaoBase<MockHostVO, Long> implements MockHostDao {
    protected final SearchBuilder<MockHostVO> GuidSearch;

    public MockHostDaoImpl() {
        GuidSearch = createSearchBuilder();
        GuidSearch.and("guid", GuidSearch.entity().getGuid(), SearchCriteria.Op.EQ);
        GuidSearch.done();
    }

    @Override
    public MockHost findByGuid(String guid) {
        SearchCriteria<MockHostVO> sc = GuidSearch.create();
        sc.setParameters("guid", guid);
        return findOneBy(sc);
    }

    @Override
    public MockHost findByVmId(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeByGuid(String guid) {
        MockHost host = this.findByGuid(guid);
        if (host == null) {
            return false;
        }
        return this.remove(host.getId());
    }

}
