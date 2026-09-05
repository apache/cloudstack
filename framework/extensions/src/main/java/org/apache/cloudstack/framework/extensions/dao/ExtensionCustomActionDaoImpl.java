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

package org.apache.cloudstack.framework.extensions.dao;

import java.util.List;

import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ExtensionCustomActionDaoImpl extends GenericDaoBase<ExtensionCustomActionVO, Long> implements ExtensionCustomActionDao {

    private final SearchBuilder<ExtensionCustomActionVO> AllFieldSearch;

    public ExtensionCustomActionDaoImpl() {
        AllFieldSearch = createSearchBuilder();
        AllFieldSearch.and("name", AllFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("extensionId", AllFieldSearch.entity().getExtensionId(), SearchCriteria.Op.EQ);
        AllFieldSearch.done();
    }

    @Override
    public ExtensionCustomActionVO findByNameAndExtensionId(long extensionId, String name) {
        SearchCriteria<ExtensionCustomActionVO> sc = AllFieldSearch.create();
        sc.setParameters("extensionId", extensionId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    @Override
    public List<Long> listIdsByExtensionId(long extensionId) {
        GenericSearchBuilder<ExtensionCustomActionVO, Long> sb = createSearchBuilder(Long.class);
        sb.selectFields(sb.entity().getId());
        sb.and("extensionId", sb.entity().getExtensionId(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<Long> sc = sb.create();
        sc.setParameters("extensionId", extensionId);
        return customSearch(sc, null);
    }
}
