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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.acl.ApiKeyPairPermissionVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class ApiKeyPairPermissionsDaoImpl extends GenericDaoBase<ApiKeyPairPermissionVO, Long> implements ApiKeyPairPermissionsDao {
    private final SearchBuilder<ApiKeyPairPermissionVO> permissionByApiKeyPairIdSearch;

    public ApiKeyPairPermissionsDaoImpl() {
        super();

        permissionByApiKeyPairIdSearch = createSearchBuilder();
        permissionByApiKeyPairIdSearch.and("apiKeyPairId", permissionByApiKeyPairIdSearch.entity().getApiKeyPairId(), SearchCriteria.Op.EQ);
        permissionByApiKeyPairIdSearch.done();
    }

    public List<ApiKeyPairPermissionVO> findAllByApiKeyPairId(Long apiKeyPairId) {
        SearchCriteria<ApiKeyPairPermissionVO> sc = permissionByApiKeyPairIdSearch.create();
        sc.setParameters("apiKeyPairId", String.valueOf(apiKeyPairId));
        return listBy(sc);
    }

    @Override
    public ApiKeyPairPermissionVO persist(final ApiKeyPairPermissionVO item) {
        item.setSortOrder(0);
        final List<ApiKeyPairPermissionVO> permissionsList = findAllByKeyPairIdSorted(item.getApiKeyPairId());
        if (permissionsList != null && !permissionsList.isEmpty()) {
            ApiKeyPairPermissionVO lastPermission = permissionsList.get(permissionsList.size() - 1);
            item.setSortOrder(lastPermission.getSortOrder() + 1);
        }
        return super.persist(item);
    }

    @Override
    public List<ApiKeyPairPermissionVO> findAllByKeyPairIdSorted(Long apiKeyPairId) {
        final SearchCriteria<ApiKeyPairPermissionVO> sc = permissionByApiKeyPairIdSearch.create();
        sc.setParameters("apiKeyPairId", apiKeyPairId);
        final Filter searchBySorted = new Filter(ApiKeyPairPermissionVO.class, "sortOrder", true, null, null);
        final List<ApiKeyPairPermissionVO> apiKeyPairPermissionList = listBy(sc, searchBySorted);
        return Objects.requireNonNullElse(apiKeyPairPermissionList, Collections.emptyList());
    }
}
