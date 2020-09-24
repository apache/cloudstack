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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.RolePermissionVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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

@Component
public class RolePermissionsDaoImpl extends GenericDaoBase<RolePermissionVO, Long> implements RolePermissionsDao {
    protected static final Logger LOGGER = Logger.getLogger(RolePermissionsDaoImpl.class);

    private final SearchBuilder<RolePermissionVO> RolePermissionsSearchByRoleAndRule;
    private final SearchBuilder<RolePermissionVO> RolePermissionsSearch;
    private Attribute sortOrderAttribute;

    public RolePermissionsDaoImpl() {
        super();

        RolePermissionsSearchByRoleAndRule = createSearchBuilder();
        RolePermissionsSearchByRoleAndRule.and("roleId", RolePermissionsSearchByRoleAndRule.entity().getRoleId(), SearchCriteria.Op.EQ);
        RolePermissionsSearchByRoleAndRule.and("rule", RolePermissionsSearchByRoleAndRule.entity().getRule(), SearchCriteria.Op.EQ);
        RolePermissionsSearchByRoleAndRule.done();

        RolePermissionsSearch = createSearchBuilder();
        RolePermissionsSearch.and("uuid", RolePermissionsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        RolePermissionsSearch.and("roleId", RolePermissionsSearch.entity().getRoleId(), SearchCriteria.Op.EQ);
        RolePermissionsSearch.and("sortOrder", RolePermissionsSearch.entity().getSortOrder(), SearchCriteria.Op.EQ);
        RolePermissionsSearch.done();

        sortOrderAttribute = _allAttributes.get("sortOrder");

        assert (sortOrderAttribute != null) : "Couldn't find one of these attributes";
    }

    private boolean updateSortOrder(final RolePermissionVO permissionBeingMoved, final RolePermissionVO parentPermission) {
        if (parentPermission != null && permissionBeingMoved.getId() == parentPermission.getId()) {
            return true;
        }
        final List<RolePermissionVO> newOrderedPermissionsList = new ArrayList<>();
        // Null parent implies item needs to move to the top
        if (parentPermission == null) {
            newOrderedPermissionsList.add(permissionBeingMoved);
        }
        for (final RolePermissionVO permission : findAllByRoleIdSorted(permissionBeingMoved.getRoleId())) {
            if (permission.getId() == permissionBeingMoved.getId()) {
                continue;
            }
            newOrderedPermissionsList.add(permission);
            if (parentPermission != null && permission.getId() == parentPermission.getId()) {
                newOrderedPermissionsList.add(permissionBeingMoved);
            }
        }
        long sortOrder = 0L;
        for (final RolePermissionVO permission : newOrderedPermissionsList) {
            permission.setSortOrder(sortOrder++);
            if (!update(permission.getId(), permission)) {
                LOGGER.warn("Failed to update item's sort order with id:" + permission.getId() + " while moving permission with id:" + permissionBeingMoved.getId() + " to a new position");
                return false;
            }
        }
        return true;
    }

    @Override
    public RolePermissionVO persist(final RolePermissionVO item) {
        item.setSortOrder(0);
        final List<RolePermissionVO> permissionsList = findAllByRoleIdSorted(item.getRoleId());
        if (permissionsList != null && permissionsList.size() > 0) {
            RolePermission lastRule = permissionsList.get(permissionsList.size() - 1);
            item.setSortOrder(lastRule.getSortOrder() + 1);
        }
        return super.persist(item);
    }

    @Override
    public boolean update(final Role role, final List<RolePermission> newOrder) {
        if (role == null || newOrder == null || newOrder.isEmpty()) {
            return false;
        }
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                final String failMessage = "The role's rule permissions list has changed while you were making updates, aborted re-ordering of rules. Please try again.";
                final List<RolePermissionVO> currentOrder = findAllByRoleIdSorted(role.getId());
                if (role.getId() < 1L || newOrder.size() != currentOrder.size()) {
                    throw new CloudRuntimeException(failMessage);
                }
                final Set<Long> newOrderSet = new HashSet<>();
                for (final RolePermission permission : newOrder) {
                    if (permission == null) {
                        continue;
                    }
                    newOrderSet.add(permission.getId());
                }
                final Set<Long> currentOrderSet = new HashSet<>();
                for (final RolePermission permission : currentOrder) {
                    currentOrderSet.add(permission.getId());
                }
                if (!newOrderSet.equals(currentOrderSet)) {
                    throw new CloudRuntimeException(failMessage);
                }
                long sortOrder = 0L;
                for (RolePermission rolePermission : newOrder) {
                    final SearchCriteria<RolePermissionVO> sc = RolePermissionsSearch.create();
                    sc.setParameters("uuid", rolePermission.getUuid());
                    sc.setParameters("roleId", role.getId());
                    sc.setParameters("sortOrder", rolePermission.getSortOrder());

                    final UpdateBuilder ub = getUpdateBuilder(rolePermission);
                    ub.set(rolePermission, sortOrderAttribute, sortOrder);
                    final int result = update(ub, sc, null);
                    if (result < 1) {
                        throw new CloudRuntimeException(failMessage);
                    }
                    sortOrder++;
                }
                return true;
            }
        });
    }

    @Override
    public boolean update(Role role, RolePermission rolePermission, Permission permission) {
        if (role == null || rolePermission == null || permission == null) {
            return false;
        }
        RolePermissionVO rolePermissionVO = findById(rolePermission.getId());
        if (rolePermissionVO == null || role.getId() != rolePermission.getRoleId() || rolePermissionVO.getId() != rolePermission.getId()) {
            return false;
        }
        rolePermissionVO.setPermission(permission);
        return update(rolePermission.getId(), rolePermissionVO);
    }

    @Override
    public List<RolePermissionVO> findAllByRoleIdSorted(final Long roleId) {
        final SearchCriteria<RolePermissionVO> sc = RolePermissionsSearch.create();
        if (roleId != null && roleId > 0L) {
            sc.setParameters("roleId", roleId);
        }
        final Filter searchBySorted = new Filter(RolePermissionVO.class, "sortOrder", true, null, null);
        final List<RolePermissionVO> rolePermissionList = listBy(sc, searchBySorted);
        if (rolePermissionList == null) {
            return Collections.emptyList();
        }
        return rolePermissionList;
    }

    @Override
    public RolePermissionVO findByRoleIdAndRule(final Long roleId, final String rule) {
        final SearchCriteria<RolePermissionVO> sc = RolePermissionsSearchByRoleAndRule.create();
        sc.setParameters("roleId", roleId);
        sc.setParameters("rule", rule);
        return findOneBy(sc);
    }
}
