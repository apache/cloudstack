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

package org.apache.cloudstack.acl;

import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface RoleService {

    ConfigKey<Boolean> EnableDynamicApiChecker = new ConfigKey<>("Advanced", Boolean.class, "dynamic.apichecker.enabled", "false",
            "If set to true, this enables the dynamic role-based api access checker and disables the default static role-based api access checker.", true);

    boolean isEnabled();

    /**
     *  Searches for a role with the given ID. If the ID is null or less than zero, this method will return null.
     *  This method will also return null if no role is found with the provided ID.
     *  Moreover, we will check if the requested role is of 'Admin' type; roles with 'Admin' type should only be visible to 'root admins'.
     *  Therefore, if a non-'root admin' user tries to search for an 'Admin' role, this method will return null.
     */
    Role findRole(Long id);

    Role createRole(String name, RoleType roleType, String description);

    Role createRole(String name, Role role, String description);

    Role importRole(String name, RoleType roleType, String description, List<Map<String, Object>> rules, boolean forced);

    Role updateRole(Role role, String name, RoleType roleType, String description);

    boolean deleteRole(Role role);

    RolePermission findRolePermission(Long id);

    RolePermission findRolePermissionByRoleIdAndRule(Long roleId, String rule);

    RolePermission createRolePermission(Role role, Rule rule, Permission permission, String description);

    /**
     * updateRolePermission updates the order/position of an role permission
     * @param role The role whose permissions needs to be re-ordered
     * @param newOrder The new list of ordered role permissions
     */
    boolean updateRolePermission(Role role, List<RolePermission> newOrder);

    boolean updateRolePermission(Role role, RolePermission rolePermission, Permission permission);

    boolean deleteRolePermission(RolePermission rolePermission);

    /**
     *  List all roles configured in the database. Roles that have the type {@link RoleType#Admin} will not be shown for users that are not 'root admin'.
     */
    List<Role> listRoles();

    Pair<List<Role>, Integer> listRoles(Long startIndex, Long limit);

    /**
     *  Find all roles that have the giving {@link String} as part of their name.
     *  If the user calling the method is not a 'root admin', roles of type {@link RoleType#Admin} wil lbe removed of the returned list.
     */
    List<Role> findRolesByName(String name);

    Pair<List<Role>, Integer> findRolesByName(String name, String keyword, Long startIndex, Long limit);

    /**
     *  Find all roles by {@link RoleType}. If the role type is {@link RoleType#Admin}, the calling account must be a root admin, otherwise we return an empty list.
     */
    List<Role> findRolesByType(RoleType roleType);

    Pair<List<Role>, Integer> findRolesByType(RoleType roleType, Long startIndex, Long limit);

    List<RolePermission> findAllPermissionsBy(Long roleId);

    Permission getRolePermission(String permission);
}
