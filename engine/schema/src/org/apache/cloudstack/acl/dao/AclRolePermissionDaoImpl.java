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

import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclRolePermissionVO;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AclRolePermissionDaoImpl extends GenericDaoBase<AclRolePermissionVO, Long> implements AclRolePermissionDao {
    private SearchBuilder<AclRolePermissionVO> findByRoleEntity;

    public AclRolePermissionDaoImpl()
    {

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        findByRoleEntity = createSearchBuilder();
        findByRoleEntity.and("roleId", findByRoleEntity.entity().getAclRoleId(), SearchCriteria.Op.EQ);
        findByRoleEntity.and("entityType", findByRoleEntity.entity().getEntityType(), SearchCriteria.Op.EQ);
        findByRoleEntity.and("accessType", findByRoleEntity.entity().getAccessType(), SearchCriteria.Op.EQ);
        findByRoleEntity.done();

        return true;
    }

    @Override
    public AclRolePermissionVO findByRoleAndEntity(long roleId, String entityType, AccessType accessType) {
        SearchCriteria<AclRolePermissionVO> sc = findByRoleEntity.create();
        sc.setParameters("roleId", roleId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("accessType", accessType);
        return findOneBy(sc);
    }
}
