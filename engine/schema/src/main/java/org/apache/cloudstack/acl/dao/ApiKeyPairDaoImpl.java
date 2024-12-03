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
import org.apache.cloudstack.acl.ApiKeyPairVO;
import org.apache.cloudstack.api.command.admin.user.ListUserKeysCmd;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiKeyPairDaoImpl extends GenericDaoBase<ApiKeyPairVO, Long> implements ApiKeyPairDao {

    private final SearchBuilder<ApiKeyPairVO> keyPairSearch;

    ApiKeyPairDaoImpl() {
        super();

        keyPairSearch = createSearchBuilder();
        keyPairSearch.and("apiKey", keyPairSearch.entity().getApiKey(), SearchCriteria.Op.EQ);
        keyPairSearch.and("secretKey", keyPairSearch.entity().getSecretKey(), SearchCriteria.Op.EQ);
        keyPairSearch.and("id", keyPairSearch.entity().getId(), SearchCriteria.Op.EQ);
        keyPairSearch.and("userId", keyPairSearch.entity().getUserId(), SearchCriteria.Op.IN);
        keyPairSearch.done();
    }

    @Override
    public ApiKeyPairVO findByApiKey(String apiKey) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters("apiKey", apiKey);
        return findOneBy(sc);
    }

    public ApiKeyPairVO findBySecretKey(String secretKey) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters("secretKey", secretKey);
        return findOneBy(sc);
    }

    public Pair<List<ApiKeyPairVO>, Integer> listApiKeysByUserOrApiKeyId(Long userId, Long apiKeyId) {
        SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        if (userId != null) {
            sc.setParametersIfNotNull("userId", String.valueOf(userId));
        }
        sc.setParametersIfNotNull("id", apiKeyId);
        final Filter searchFilter = new Filter(100);
        return searchAndCount(sc, searchFilter);
    }

    public ApiKeyPairVO getLastApiKeyCreatedByUser(Long userId) {
        final SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        if (userId != null) {
            sc.setParameters("userId", String.valueOf(userId));
        }
        final Filter searchBySorted = new Filter(ApiKeyPairVO.class, "id", false, null, null);
        final List<ApiKeyPairVO> apiKeyPairVOList = listBy(sc, searchBySorted);
        if (CollectionUtils.isEmpty(apiKeyPairVOList)) {
            return null;
        }
        return apiKeyPairVOList.get(0);
    }

    public Pair<List<ApiKeyPairVO>, Integer> listByUserIdsPaginated(List<Long> userIds, ListUserKeysCmd cmd) {
        Long pageSizeVal = cmd.getPageSizeVal();
        Long startIndex = cmd.getStartIndex();
        Filter searchFilter = new Filter(ApiKeyPairVO.class, "id", true, startIndex, pageSizeVal);

        final SearchCriteria<ApiKeyPairVO> sc = keyPairSearch.create();
        sc.setParameters("userId", (Object[]) userIds.toArray(new Long[0]));

        final Pair<List<ApiKeyPairVO>, Integer> apiKeyPairVOList = searchAndCount(sc, searchFilter);
        if (CollectionUtils.isEmpty(apiKeyPairVOList.first())) {
            return new Pair(List.of(), 0);
        }
        return apiKeyPairVOList;
    }

        @Override
    public boolean update(Long id, ApiKeyPairVO apiKeyPair) {
        ApiKeyPairVO ub = createForUpdate();

        ub.setUuid(apiKeyPair.getUuid());
        ub.setUserId(apiKeyPair.getUserId());
        ub.setName(apiKeyPair.getName());
        ub.setDomainId(apiKeyPair.getDomainId());
        ub.setAccountId(apiKeyPair.getAccountId());
        ub.setStartDate(apiKeyPair.getStartDate());
        ub.setEndDate(apiKeyPair.getEndDate());
        ub.setCreated(apiKeyPair.getCreated());
        ub.setDescription(apiKeyPair.getDescription());
        ub.setApiKey(apiKeyPair.getApiKey());
        ub.setSecretKey(apiKeyPair.getSecretKey());
        ub.setRemoved(apiKeyPair.getRemoved());

        return super.update(id, ub);
    }
}
