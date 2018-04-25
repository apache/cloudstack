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

import org.apache.cloudstack.acl.RolePermission.Permission;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface RoleService {

    ConfigKey<Boolean> EnableDynamicApiChecker = new ConfigKey<>("Advanced", Boolean.class, "dynamic.apichecker.enabled", "false",
            "If set to true, this enables the dynamic role-based api access checker and disables the default static role-based api access checker.", true);

    boolean isEnabled();

    /**
     *  Searches for a role with the given ID. If the ID is null or less than zero, this method will return null.
     *  This method will also return null if no role is found with the provided ID. Moreover, we will check if the requested role is of 'Admin' type; roles with 'Admin' type should only be visible to 'root admins'.  Therefore, if a non-'root admin' user tries to search for an 'Admin' role, this method will return null.
     */
    Role findRole(Long id);

    Role createRole(final String name, final RoleType roleType, final String description);

    Role updateRole(final Role role, final String name, final RoleType roleType, final String description);

    boolean deleteRole(final Role role);

    RolePermission findRolePermission(final Long id);

    RolePermission findRolePermissionByUuid(final String uuid);

    RolePermission createRolePermission(final Role role, final Rule rule, final Permission permission, final String description);

    /**
     * updateRolePermission updates the order/position of an role permission
     * @param role The role whose permissions needs to be re-ordered
     * @param newOrder The new list of ordered role permissions
     */
    boolean updateRolePermission(final Role role, final List<RolePermission> newOrder);

    boolean updateRolePermission(final Role role, final RolePermission rolePermission, final Permission permission);

    boolean deleteRolePermission(RolePermission rolePermission);

    List<Role> listRoles();

    /**
     *  Find all roles that have the giving {@link String} as part of their name.
     *  If the user calling the method is not a 'root admin', roles of type {@link RoleType#Admin} wil lbe removed of the returned list.  
     */
    List<Role> findRolesByName(String name);

    /**
     *  Find all roles by {@link RoleType}. If the role type is {@link RoleType#Admin}, the calling account must be a root admin, otherwise we return an empty list. 
     */
    List<Role> findRolesByType(RoleType roleType);

    List<RolePermission> findAllPermissionsBy(Long roleId);
}
