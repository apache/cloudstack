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

import org.apache.cloudstack.acl.AclPolicyPermission.Permission;
import org.apache.cloudstack.acl.AclPolicyPermissionVO;
import org.apache.cloudstack.acl.PermissionScope;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class AclPolicyPermissionDaoImpl extends GenericDaoBase<AclPolicyPermissionVO, Long> implements
        AclPolicyPermissionDao {

    private SearchBuilder<AclPolicyPermissionVO> policyIdSearch;
    private SearchBuilder<AclPolicyPermissionVO> fullSearch;
    private SearchBuilder<AclPolicyPermissionVO> actionScopeSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        policyIdSearch = createSearchBuilder();
        policyIdSearch.and("policyId", policyIdSearch.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        policyIdSearch.done();

        fullSearch = createSearchBuilder();
        fullSearch.and("policyId", fullSearch.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        fullSearch.and("entityType", fullSearch.entity().getEntityType(), SearchCriteria.Op.EQ);
        fullSearch.and("scope", fullSearch.entity().getScope(), SearchCriteria.Op.EQ);
        fullSearch.and("scopeId", fullSearch.entity().getScopeId(), SearchCriteria.Op.EQ);
        fullSearch.and("action", fullSearch.entity().getAction(), SearchCriteria.Op.EQ);
        fullSearch.and("permission", fullSearch.entity().getPermission(), SearchCriteria.Op.EQ);
        fullSearch.done();

        actionScopeSearch = createSearchBuilder();
        actionScopeSearch.and("policyId", actionScopeSearch.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        actionScopeSearch.and("scope", actionScopeSearch.entity().getScope(), SearchCriteria.Op.EQ);
        actionScopeSearch.and("action", actionScopeSearch.entity().getAction(), SearchCriteria.Op.EQ);
        actionScopeSearch.and("permission", actionScopeSearch.entity().getPermission(), SearchCriteria.Op.EQ);
        actionScopeSearch.done();

        return true;
    }

    @Override
    public List<AclPolicyPermissionVO> listByPolicy(long policyId) {
        SearchCriteria<AclPolicyPermissionVO> sc = policyIdSearch.create();
        sc.setParameters("policyId", policyId);
        return listBy(sc);
    }

    @Override
    public AclPolicyPermissionVO findByPolicyAndEntity(long policyId, String entityType, PermissionScope scope, Long scopeId, String action, Permission perm) {
        SearchCriteria<AclPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("scope", scope);
        sc.setParameters("scopeId", scopeId);
        sc.setParameters("action", action);
        sc.setParameters("permission", perm);
        return findOneBy(sc);
    }

    @Override
    public List<AclPolicyPermissionVO> listGrantedByActionAndScope(long policyId, String action, PermissionScope scope) {
        SearchCriteria<AclPolicyPermissionVO> sc = actionScopeSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("action", action);
        sc.setParameters("scope", scope);
        sc.setParameters("permission", Permission.Allow);
        return listBy(sc);
    }

    @Override
    public List<AclPolicyPermissionVO> listByPolicyActionAndEntity(long policyId, String action, String entityType) {
        SearchCriteria<AclPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("action", action);
        return listBy(sc);
    }

}
