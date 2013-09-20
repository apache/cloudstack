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

import org.apache.cloudstack.acl.AclGroupRoleMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AclGroupRoleMapDaoImpl extends GenericDaoBase<AclGroupRoleMapVO, Long> implements AclGroupRoleMapDao {
    private SearchBuilder<AclGroupRoleMapVO> ListByGroupId;
    private SearchBuilder<AclGroupRoleMapVO> ListByRoleId;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        ListByGroupId = createSearchBuilder();
        ListByGroupId.and("groupId", ListByGroupId.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        ListByGroupId.done();

        ListByRoleId = createSearchBuilder();
        ListByRoleId.and("roleId", ListByRoleId.entity().getAclRoleId(), SearchCriteria.Op.EQ);
        ListByRoleId.done();

        return true;
    }

    @Override
    public List<AclGroupRoleMapVO> listByGroupId(long groupId) {
        SearchCriteria<AclGroupRoleMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public List<AclGroupRoleMapVO> listByRoleId(long roleId) {
        SearchCriteria<AclGroupRoleMapVO> sc = ListByRoleId.create();
        sc.setParameters("roleId", roleId);
        return listBy(sc);
    }

}
