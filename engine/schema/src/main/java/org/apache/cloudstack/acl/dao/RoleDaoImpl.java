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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.RoleVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleDaoImpl extends GenericDaoBase<RoleVO, Long> implements RoleDao {
    private final SearchBuilder<RoleVO> RoleByNameSearch;
    private final SearchBuilder<RoleVO> RoleByTypeSearch;
    private final SearchBuilder<RoleVO> RoleByNameAndTypeSearch;

    public RoleDaoImpl() {
        super();

        RoleByNameSearch = createSearchBuilder();
        RoleByNameSearch.and("roleName", RoleByNameSearch.entity().getName(), SearchCriteria.Op.LIKE);
        RoleByNameSearch.done();

        RoleByTypeSearch = createSearchBuilder();
        RoleByTypeSearch.and("roleType", RoleByTypeSearch.entity().getRoleType(), SearchCriteria.Op.EQ);
        RoleByTypeSearch.done();

        RoleByNameAndTypeSearch = createSearchBuilder();
        RoleByNameAndTypeSearch.and("roleName", RoleByNameAndTypeSearch.entity().getName(), SearchCriteria.Op.EQ);
        RoleByNameAndTypeSearch.and("roleType", RoleByNameAndTypeSearch.entity().getRoleType(), SearchCriteria.Op.EQ);
        RoleByNameAndTypeSearch.done();
    }

    @Override
    public List<RoleVO> findAllByName(final String roleName) {
        return findAllByName(roleName, null, null, null).first();
    }

    @Override
    public Pair<List<RoleVO>, Integer> findAllByName(final String roleName, String keyword, Long offset, Long limit) {
        SearchCriteria<RoleVO> sc = RoleByNameSearch.create();
        if (StringUtils.isNotEmpty(roleName)) {
            sc.setParameters("roleName", roleName);
        }
        if (StringUtils.isNotEmpty(keyword)) {
            sc.setParameters("roleName", "%" + keyword + "%");
        }

        return searchAndCount(sc, new Filter(RoleVO.class, "id", true, offset, limit));
    }

    @Override
    public List<RoleVO> findAllByRoleType(final RoleType type) {
        return findAllByRoleType(type, null, null).first();
    }

    public Pair<List<RoleVO>, Integer> findAllByRoleType(final RoleType type, Long offset, Long limit) {
        SearchCriteria<RoleVO> sc = RoleByTypeSearch.create();
        sc.setParameters("roleType", type);
        return searchAndCount(sc, new Filter(RoleVO.class, "id", true, offset, limit));
    }

    @Override
    public List<RoleVO> findByName(String roleName) {
        SearchCriteria<RoleVO> sc = RoleByNameSearch.create();
        sc.setParameters("roleName", roleName);
        return listBy(sc);
    }

    @Override
    public RoleVO findByNameAndType(String roleName, RoleType type) {
        SearchCriteria<RoleVO> sc = RoleByNameAndTypeSearch.create();
        sc.setParameters("roleName", roleName);
        sc.setParameters("roleType", type);
        return findOneBy(sc);
    }
}
