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

import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ExtensionDaoImpl extends GenericDaoBase<ExtensionVO, Long> implements ExtensionDao {

    private final SearchBuilder<ExtensionVO> AllFieldSearch;

    public ExtensionDaoImpl() {
        AllFieldSearch = createSearchBuilder();
        AllFieldSearch.and("name", AllFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("type", AllFieldSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("state", AllFieldSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldSearch.done();
    }

    @Override
    public ExtensionVO findByName(String name) {
        SearchCriteria<ExtensionVO> sc = AllFieldSearch.create();
        sc.setParameters("name", name);

        return findOneBy(sc);
    }
}
