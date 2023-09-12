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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.ProjectRolePermissionVO;
import org.apache.log4j.Logger;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

public class ProjectRolePermissionsDaoImpl  extends GenericDaoBase<ProjectRolePermissionVO, Long>  implements  ProjectRolePermissionsDao{

    private static final Logger LOGGER = Logger.getLogger(ProjectRolePermissionsDaoImpl.class);
    private final SearchBuilder<ProjectRolePermissionVO> ProjectRolePermissionsSearch;
    private Attribute sortOrderAttribute;

    public ProjectRolePermissionsDaoImpl() {
        super();

        ProjectRolePermissionsSearch = createSearchBuilder();
        ProjectRolePermissionsSearch.and("uuid", ProjectRolePermissionsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        ProjectRolePermissionsSearch.and("projectRoleId", ProjectRolePermissionsSearch.entity().getProjectRoleId(), SearchCriteria.Op.EQ);
        ProjectRolePermissionsSearch.and("projectId", ProjectRolePermissionsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        ProjectRolePermissionsSearch.and("sortOrder", ProjectRolePermissionsSearch.entity().getSortOrder(), SearchCriteria.Op.EQ);
        ProjectRolePermissionsSearch.done();

        sortOrderAttribute = _allAttributes.get("sortOrder");

        assert (sortOrderAttribute != null) : "Couldn't find one of these attributes";
    }

    @Override
    public List<ProjectRolePermissionVO> findAllByRoleIdSorted(Long roleId, Long projectId) {
        final SearchCriteria<ProjectRolePermissionVO> sc = ProjectRolePermissionsSearch.create();
        if (roleId != null && roleId > 0L) {
            sc.setParameters("projectRoleId", roleId);
        }
        if (projectId != null && projectId > 0L) {
            sc.setParameters("projectId", projectId);
        }
        final Filter searchBySorted = new Filter(ProjectRolePermissionVO.class, "sortOrder", true, null, null);
        final List<ProjectRolePermissionVO> projectRolePermissionList = listBy(sc, searchBySorted);
        if (projectRolePermissionList == null) {
            return Collections.emptyList();
        }
        return projectRolePermissionList;
    }

    @Override
    public boolean update(ProjectRole role, Long projectId, List<ProjectRolePermission> newOrder) {
        if (role == null || newOrder == null || newOrder.isEmpty()) {
            return false;
        }
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                final String failMsg = "Project role's rule permissions list has changed while you were making updates, aborted re-ordering of rules. Please try again.";
                final List<ProjectRolePermissionVO> currentOrder = findAllByRoleIdSorted(role.getId(), projectId);
                if (role.getId() < 1L || newOrder.size() != currentOrder.size()) {
                    throw new CloudRuntimeException(failMsg);
                }
                Set<Long> newOrderSet = new HashSet<>();
                newOrderSet = newOrder.stream().map(perm -> perm.getId()).collect(Collectors.toSet());

                Set<Long> currOrderSet = new HashSet<>();
                currOrderSet = currentOrder.stream().map(perm -> perm.getId()).collect(Collectors.toSet());

                long sortOrder = 0L;
                if (!newOrderSet.equals(currOrderSet)) {
                    throw new CloudRuntimeException(failMsg);
                }
                for (ProjectRolePermission projectRolePermission : newOrder) {
                    final SearchCriteria<ProjectRolePermissionVO> sc = ProjectRolePermissionsSearch.create();
                    sc.setParameters("uuid", projectRolePermission.getUuid());
                    sc.setParameters("projectRoleId", role.getId());
                    sc.setParameters("projectId", role.getProjectId());
                    sc.setParameters("sortOrder", projectRolePermission.getSortOrder());

                    final UpdateBuilder ub = getUpdateBuilder(projectRolePermission);
                    ub.set(projectRolePermission, sortOrderAttribute, sortOrder);
                    final int result = update(ub, sc, null);
                    if (result < 1) {
                        throw new CloudRuntimeException(failMsg);
                    }
                    sortOrder++;
                }
                return true;
            }
        });
    }

    @Override
    public boolean update(ProjectRole role, ProjectRolePermission rolePermission, Permission permission) {
        if (role == null || rolePermission == null || permission == null) {
            return false;
        }
        ProjectRolePermissionVO projectRolePermissionVO = findById(rolePermission.getId());
        if (projectRolePermissionVO == null) {
            return false;
        }
        projectRolePermissionVO.setPermission(permission);
        return update(rolePermission.getId(), projectRolePermissionVO);
    }

    @Override
    public ProjectRolePermissionVO persist(final ProjectRolePermissionVO item) {
        item.setSortOrder(0);
        final List<ProjectRolePermissionVO> permissionsList = findAllByRoleIdSorted(item.getProjectRoleId(), item.getProjectId());
        if (permissionsList != null && permissionsList.size() > 0) {
            ProjectRolePermission lastRule = permissionsList.get(permissionsList.size() - 1);
            item.setSortOrder(lastRule.getSortOrder() + 1);
        }
        return super.persist(item);
    }
}
