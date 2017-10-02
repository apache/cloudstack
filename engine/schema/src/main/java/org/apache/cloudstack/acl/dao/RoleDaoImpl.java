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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.RoleVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleDaoImpl extends GenericDaoBase<RoleVO, Long> implements RoleDao {
    private final SearchBuilder<RoleVO> RoleByNameSearch;
    private final SearchBuilder<RoleVO> RoleByTypeSearch;

    public RoleDaoImpl() {
        super();

        RoleByNameSearch = createSearchBuilder();
        RoleByNameSearch.and("roleName", RoleByNameSearch.entity().getName(), SearchCriteria.Op.LIKE);
        RoleByNameSearch.done();

        RoleByTypeSearch = createSearchBuilder();
        RoleByTypeSearch.and("roleType", RoleByTypeSearch.entity().getRoleType(), SearchCriteria.Op.EQ);
        RoleByTypeSearch.done();
    }

    @Override
    public List<RoleVO> findAllByName(final String roleName) {
        SearchCriteria<RoleVO> sc = RoleByNameSearch.create();
        sc.setParameters("roleName", "%" + roleName + "%");
        return listBy(sc);
    }

    @Override
    public List<RoleVO> findAllByRoleType(final RoleType type) {
        SearchCriteria<RoleVO> sc = RoleByTypeSearch.create();
        sc.setParameters("roleType", type);
        return listBy(sc);
    }
}
