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
package com.cloud.cluster.dao;

import com.cloud.cluster.ManagementServerStatusVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class ManagementServerStatusDaoImpl extends GenericDaoBase<ManagementServerStatusVO, Long> implements ManagementServerStatusDao {
    private final SearchBuilder<ManagementServerStatusVO> MsIdSearch;

    public ManagementServerStatusDaoImpl() {
        MsIdSearch = createSearchBuilder();
        MsIdSearch.and("msid", MsIdSearch.entity().getMsId(), SearchCriteria.Op.EQ);
        MsIdSearch.done();
    }

    @Override
    public ManagementServerStatusVO findByMsId(String msId) {
        SearchCriteria<ManagementServerStatusVO> sc = MsIdSearch.create();
        sc.setParameters("msid", msId);

        List<ManagementServerStatusVO> allServerStats = listIncludingRemovedBy(sc);
        if (CollectionUtils.isNotEmpty(allServerStats)) {
            return allServerStats.get(0);
        }

        return null;
    }
}
