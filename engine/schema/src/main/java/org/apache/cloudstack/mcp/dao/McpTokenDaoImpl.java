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
package org.apache.cloudstack.mcp.dao;

import java.util.List;

import org.apache.cloudstack.mcp.McpTokenVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class McpTokenDaoImpl extends GenericDaoBase<McpTokenVO, Long> implements McpTokenDao {
    private static final String ID = "id";
    private static final String USER_ID = "userId";
    private static final String TOKEN_HASH = "tokenHash";

    private final SearchBuilder<McpTokenVO> tokenSearch;

    public McpTokenDaoImpl() {
        super();

        tokenSearch = createSearchBuilder();
        tokenSearch.and(TOKEN_HASH, tokenSearch.entity().getTokenHash(), SearchCriteria.Op.EQ);
        tokenSearch.and(USER_ID, tokenSearch.entity().getUserId(), SearchCriteria.Op.EQ);
        tokenSearch.done();
    }

    @Override
    public McpTokenVO findByTokenHash(String tokenHash) {
        SearchCriteria<McpTokenVO> sc = tokenSearch.create();
        sc.setParameters(TOKEN_HASH, tokenHash);
        return findOneBy(sc);
    }

    @Override
    public Pair<List<McpTokenVO>, Integer> listByUserId(long userId, Long startIndex, Long pageSize) {
        SearchCriteria<McpTokenVO> sc = tokenSearch.create();
        sc.setParameters(USER_ID, userId);
        return searchAndCount(sc, new Filter(McpTokenVO.class, ID, true, startIndex, pageSize));
    }

    @Override
    public int removeByUserId(long userId) {
        SearchCriteria<McpTokenVO> sc = tokenSearch.create();
        sc.setParameters(USER_ID, userId);
        return remove(sc);
    }
}
