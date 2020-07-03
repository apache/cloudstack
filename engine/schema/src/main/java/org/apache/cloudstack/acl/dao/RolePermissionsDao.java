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

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermission;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RolePermissionVO;

import com.cloud.utils.db.GenericDao;

public interface RolePermissionsDao extends GenericDao<RolePermissionVO, Long> {
    /**
     * Adds a new role permission at the end of the list of role permissions
     * @param item the new role permission
     * @return returns persisted role permission
     */
    RolePermissionVO persist(final RolePermissionVO item);

    /**
     * Moves an existing role permission under a given parent role permission
     * @param role the existing role
     * @param newOrder the new role permissions order
     * @return returns true on success
     */
    boolean update(final Role role, final List<RolePermission> newOrder);

    /**
     * Updates existing role permission
     * @param role role of which rule belongs
     * @param rolePermission role permission
     * @param permission permission
     * @return true on success, false if not
     */
    boolean update(final Role role, final RolePermission rolePermission, final RolePermissionEntity.Permission permission);

    /**
     * Returns ordered linked-list of role permission for a given role
     * @param roleId the ID of the role
     * @return returns list of role permissions
     */
    List<RolePermissionVO> findAllByRoleIdSorted(Long roleId);

    /**
     * Returns role permission for a given role and rule
     * @param roleId the ID of the role
     * @param roleId rule for the role
     * @return returns role permission
     */
    RolePermissionVO findByRoleIdAndRule(Long roleId, String rule);
}
