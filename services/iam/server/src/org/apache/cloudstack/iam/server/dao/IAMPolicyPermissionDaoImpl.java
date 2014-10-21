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
package org.apache.cloudstack.iam.server.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;
import org.apache.cloudstack.iam.server.IAMPolicyPermissionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class IAMPolicyPermissionDaoImpl extends GenericDaoBase<IAMPolicyPermissionVO, Long> implements
        IAMPolicyPermissionDao {

    private SearchBuilder<IAMPolicyPermissionVO> policyIdSearch;
    private SearchBuilder<IAMPolicyPermissionVO> fullSearch;
    private SearchBuilder<IAMPolicyPermissionVO> entitySearch;

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
        fullSearch.and("accessType", fullSearch.entity().getAccessType(), SearchCriteria.Op.EQ);
        fullSearch.done();

        entitySearch = createSearchBuilder();
        entitySearch.and("entityType", entitySearch.entity().getEntityType(), SearchCriteria.Op.EQ);
        entitySearch.and("scopeId", entitySearch.entity().getScopeId(), SearchCriteria.Op.EQ);
        entitySearch.done();

        return true;
    }

    @Override
    public List<IAMPolicyPermissionVO> listByPolicy(long policyId) {
        SearchCriteria<IAMPolicyPermissionVO> sc = policyIdSearch.create();
        sc.setParameters("policyId", policyId);
        return listBy(sc);
    }

    @Override
    public IAMPolicyPermissionVO findByPolicyAndEntity(long policyId, String entityType, String scope, Long scopeId,
            String action, Permission perm, String accessType) {
        SearchCriteria<IAMPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("scope", scope);
        sc.setParameters("scopeId", scopeId);
        sc.setParameters("action", action);
        sc.setParameters("permission", perm);
        if (accessType != null) {
            // accessType can be optional, used mainly in list apis with
            // ListEntry and UseEntry distinction
            sc.setParameters("accessType", accessType);
        }
        return findOneBy(sc);
    }

    @Override
    public List<IAMPolicyPermissionVO> listByPolicyActionAndScope(long policyId, String action, String scope, String accessType) {
        SearchCriteria<IAMPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("action", action);
        sc.setParameters("scope", scope);
        sc.setParameters("permission", Permission.Allow);
        if ( accessType != null ){
            // accessType can be optional, used mainly in list apis with ListEntry and UseEntry distinction
            sc.setParameters("accessType", accessType);
        }
        return listBy(sc);
    }

    @Override
    public List<IAMPolicyPermissionVO> listByPolicyActionAndEntity(long policyId, String action, String entityType) {
        SearchCriteria<IAMPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("action", action);
        return listBy(sc);
    }

    @Override
    public List<IAMPolicyPermissionVO> listByPolicyAccessAndEntity(long policyId, String accessType,
            String entityType) {
        SearchCriteria<IAMPolicyPermissionVO> sc = fullSearch.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("entityType", entityType);
        sc.setParameters("accessType", accessType);
        return listBy(sc);
    }

    @Override
    public List<IAMPolicyPermissionVO> listByEntity(String entityType, Long entityId) {
        SearchCriteria<IAMPolicyPermissionVO> sc = entitySearch.create();
        sc.setParameters("entityType", entityType);
        sc.setParameters("scopeId", entityId);
        return listBy(sc);
    }

}
