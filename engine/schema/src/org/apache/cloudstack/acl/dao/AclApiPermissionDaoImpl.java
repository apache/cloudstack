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

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclApiPermissionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AclApiPermissionDaoImpl extends GenericDaoBase<AclApiPermissionVO, Long> implements AclApiPermissionDao {
    private SearchBuilder<AclApiPermissionVO> findByRoleApi;
    private SearchBuilder<AclApiPermissionVO> ListByRoleId;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        findByRoleApi = createSearchBuilder();
        findByRoleApi.and("roleId", findByRoleApi.entity().getAclRoleId(), SearchCriteria.Op.EQ);
        findByRoleApi.and("api", findByRoleApi.entity().getApiName(), SearchCriteria.Op.EQ);
        findByRoleApi.done();

        ListByRoleId = createSearchBuilder();
        ListByRoleId.and("roleId", ListByRoleId.entity().getAclRoleId(), SearchCriteria.Op.EQ);
        ListByRoleId.done();

        return true;
    }

    @Override
    public AclApiPermissionVO findByRoleAndApi(long roleId, String api) {
        SearchCriteria<AclApiPermissionVO> sc = findByRoleApi.create();
        sc.setParameters("roleId", roleId);
        sc.setParameters("api", api);
        return findOneBy(sc);
    }

    @Override
    public List<AclApiPermissionVO> listByRoleId(long roleId) {
        SearchCriteria<AclApiPermissionVO> sc = ListByRoleId.create();
        sc.setParameters("roleId", roleId);
        return listBy(sc);
    }

}
