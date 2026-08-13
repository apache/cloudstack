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
package org.apache.cloudstack.acl.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import java.util.List;
import org.apache.cloudstack.acl.ApiKeyPairVO;
import org.apache.cloudstack.api.command.admin.user.ListUserKeysCmd;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyPairDaoImpl extends GenericDaoBase<ApiKeyPairVO, Long> implements ApiKeyPairDao {
    private static final String ID = "id";
    private static final String USER_ID = "userId";
    private static final String API_KEY = "apiKey";
    private static final String SECRET_KEY = "secretKey";

    private final SearchBuilder<ApiKeyPairVO> keyPairSearch;

    ApiKeyPairDaoImpl() {
        super();

        keyPairSearch = createSearchBuilder();
        keyPairSearch.and(API_KEY, keyPairSearch.entity().getApiKey(), SearchCriteria.Op.EQ);
        keyPairSearch.and(SECRET_KEY, keyPairSearch.entity().getSecretKey(), SearchCriteria.Op.EQ);
        keyPairSearch.and(ID, keyPairSearch.entity().getId(), SearchCriteria.Op.EQ);
        keyPairSearch.and(USER_ID, keyPairSearch.entity().getUserId(), SearchCriteria.Op.IN);
        keyPairSearch.done();
    }

    @Override
    public ApiKeyPairVO findByApiKey(String apiKey) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters(API_KEY, apiKey);
        return findOneBy(sc);
    }

    public ApiKeyPairVO findBySecretKey(String secretKey) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters(SECRET_KEY, secretKey);
        return findOneBy(sc);
    }

    public Pair<List<ApiKeyPairVO>, Integer> listApiKeysByUserOrApiKeyId(Long userId, Long apiKeyId) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParametersIfNotNull(USER_ID, userId);
        sc.setParametersIfNotNull(ID, apiKeyId);
        final Filter searchFilter = new Filter(100);
        return searchAndCount(sc, searchFilter);
    }

    public ApiKeyPairVO getLastApiKeyCreatedByUser(Long userId) {
        final SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParametersIfNotNull(USER_ID, userId);
        final Filter searchBySorted = new Filter(ApiKeyPairVO.class, ID, false, null, null);
        return findOneBy(sc, searchBySorted);
    }

    public Pair<List<ApiKeyPairVO>, Integer> listByUserIdsPaginated(List<Long> userIds, ListUserKeysCmd cmd) {
        Long pageSizeVal = cmd.getPageSizeVal();
        Long startIndex = cmd.getStartIndex();
        Filter searchFilter = new Filter(ApiKeyPairVO.class, ID, true, startIndex, pageSizeVal);

        final SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters(USER_ID, (Object[]) userIds.toArray(new Long[0]));

        Pair<List<ApiKeyPairVO>, Integer> apiKeyPairVOList = searchAndCount(sc, searchFilter);
        if (CollectionUtils.isEmpty(apiKeyPairVOList.first())) {
            return new Pair<>(List.of(), 0);
        }
        return apiKeyPairVOList;
    }
}
