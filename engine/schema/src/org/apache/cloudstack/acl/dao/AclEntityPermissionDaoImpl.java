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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclEntityPermissionVO;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AclEntityPermissionDaoImpl extends GenericDaoBase<AclEntityPermissionVO, Long> implements AclEntityPermissionDao {
    private SearchBuilder<AclEntityPermissionVO> findByGroupEntity;

    public AclEntityPermissionDaoImpl()
    {

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        findByGroupEntity = createSearchBuilder();
        findByGroupEntity.and("groupId", findByGroupEntity.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        findByGroupEntity.and("entityType", findByGroupEntity.entity().getEntityType(), SearchCriteria.Op.EQ);
        findByGroupEntity.and("entityId", findByGroupEntity.entity().getEntityId(), SearchCriteria.Op.EQ);
        findByGroupEntity.and("accessType", findByGroupEntity.entity().getAccessType(), SearchCriteria.Op.EQ);
        findByGroupEntity.and("allowed", findByGroupEntity.entity().isAllowed(), SearchCriteria.Op.EQ);
        findByGroupEntity.done();

        return true;
    }

    @Override
    public AclEntityPermissionVO findByGroupAndEntity(long groupId, String entityType, long entityId, AccessType accessType) {
        SearchCriteria<AclEntityPermissionVO> sc = findByGroupEntity.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("entityId", entityId);
        sc.setParameters("accessType", accessType);
        return findOneBy(sc);
    }

    @Override
    public List<Long> findEntityIdByGroupAndPermission(long groupId, String entityType, AccessType accessType, boolean isAllowed) {
        List<Long> idList = new ArrayList<Long>();
        SearchCriteria<AclEntityPermissionVO> sc = findByGroupEntity.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("allowed", isAllowed);
        sc.setParameters("accessType", accessType);
        List<AclEntityPermissionVO> permList = listBy(sc);
        if (permList != null) {
            for (AclEntityPermissionVO perm : permList) {
                idList.add(perm.getEntityId());
            }
        }
        return idList;
    }

}
