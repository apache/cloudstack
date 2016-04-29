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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.acl.RolePermissionVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Local(value = {RolePermissionsDao.class})
public class RolePermissionsDaoImpl extends GenericDaoBase<RolePermissionVO, Long> implements RolePermissionsDao {
    protected static final Logger LOGGER = Logger.getLogger(RolePermissionsDaoImpl.class);

    private final SearchBuilder<RolePermissionVO> RolePermissionsSearch;

    public RolePermissionsDaoImpl() {
        super();

        RolePermissionsSearch = createSearchBuilder();
        RolePermissionsSearch.and("roleId", RolePermissionsSearch.entity().getRoleId(), SearchCriteria.Op.EQ);
        RolePermissionsSearch.and("sortOrder", RolePermissionsSearch.entity().getSortOrder(), SearchCriteria.Op.EQ);
        RolePermissionsSearch.done();
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
            item.setSortOrder(permissionsList.size());
        }
        return super.persist(item);
    }

    @Override
    public boolean remove(Long id) {
        final RolePermissionVO itemToBeRemoved = findById(id);
        if (itemToBeRemoved == null) {
            return false;
        }
        boolean updateStatus = true;
        final boolean removal = super.remove(id);
        if (removal) {
            long sortOrder = 0L;
            for (final RolePermissionVO permission : findAllByRoleIdSorted(itemToBeRemoved.getRoleId())) {
                permission.setSortOrder(sortOrder++);
                if (!update(permission.getId(), permission)) {
                    updateStatus = false;
                    break;
                }
            }
        }
        return removal && updateStatus;
    }

    @Override
    public boolean update(final RolePermissionVO item, final RolePermissionVO parent) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                return updateSortOrder(item, parent);
            }
        });
    }

    public List<RolePermissionVO> findAllByRoleAndParentId(final Long parentId, final Long roleId) {
        if (parentId == null || roleId == null) {
            return Collections.emptyList();
        }
        SearchCriteria<RolePermissionVO> sc = RolePermissionsSearch.create();
        sc.setParameters("roleId", roleId);
        sc.setParameters("parentId", parentId);
        return listBy(sc);
    }

    @Override
    public List<RolePermissionVO> findAllByRoleIdSorted(final Long roleId) {
        SearchCriteria<RolePermissionVO> sc = RolePermissionsSearch.create();
        if (roleId != null && roleId > 0L) {
            sc.setParameters("roleId", roleId);
        }
        List<RolePermissionVO> rolePermissionList = listBy(sc, new Filter(RolePermissionVO.class, "sortOrder", true, null, null));
        if (rolePermissionList == null) {
            return Collections.emptyList();
        }
        return rolePermissionList;
    }
}
