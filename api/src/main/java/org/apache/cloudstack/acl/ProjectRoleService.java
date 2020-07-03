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

import org.apache.cloudstack.api.command.admin.acl.project.CreateProjectRolePermissionCmd;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;

public interface ProjectRoleService {
    /**
     * Creates a Project role in a Project to be mapped to a user/ account (all users of an account)
     * @param projectId ID of the project where the project role is to be created
     * @param name Name of the project role
     * @param description description provided for the project role
     * @return the Instance of the project role created
     */
    ProjectRole createProjectRole(Long projectId, String name, String description);

    /**
     * Updates a Project role created
     * @param role Project role reference to be updated
     * @param projectId ID of the project where the Project role exists
     * @param name new name to be given to the project role
     * @param description description for the project role
     * @return the updated instance of the project role
     */
    ProjectRole updateProjectRole(ProjectRole role, Long projectId, String name, String description);

    /**
     *
     * @param projectId ID of the project in which the project role is to be searched for
     * @param roleName name/ part of a project role name
     * @return List of Project roles matching the given name in the project
     */
    List<ProjectRole> findProjectRolesByName(Long projectId, String roleName);

    /**
     *
     * @param role Project role to be deleted
     * @param projectId ID of the project where the role is present
     * @return success/failure of the delete operation
     */
    boolean deleteProjectRole(ProjectRole role, Long projectId);

    /**
     * Determines if Dynamic Roles feature is enabled , if it isn't then the project roles will not be applied
     */
    boolean isEnabled();

    /**
     *
     * @param roleId Project role ID which needs to be found
     * @param projectId ID of the project where the role is to be found
     * @return the corresponding project role
     */
    ProjectRole findProjectRole(Long roleId, Long projectId);

    /**
     *
     * @param projectId ID of the project whosr project roles are to be listed
     * @return List of all available project roles
     */
    List<ProjectRole> findProjectRoles(Long projectId);

    /**
     * Creates a project role permission to be mapped to a project role.
     * All accounts/users mapped to this project role will impose restrictions on API access
     * to users based on the project role. This is to further limit restrictions on users in projects
     */
    ProjectRolePermission createProjectRolePermission(CreateProjectRolePermissionCmd cmd);

    /**
     * Updates the order of the project role permission
     * @param projectId ID of the project where the project role permission exists
     * @param projectRole project role to which the permission is mapped to
     * @param rolePermissionsOrder re-arranged order of permissions
     * @return success/failure of operation
     */
    boolean updateProjectRolePermission(Long projectId, ProjectRole projectRole, List<ProjectRolePermission> rolePermissionsOrder);

    /**
     *
     * Updates the permission of the project role permission
     */
    boolean updateProjectRolePermission(Long projectId, ProjectRole projectRole, ProjectRolePermission projectRolePermission, Permission newPermission);

    /**
     * Finds the project role permission for the given ID
     */
    ProjectRolePermission findProjectRolePermission(final Long projRolePermissionId);

    /**
     * deletes the given project role
     */
    boolean deleteProjectRolePermission(ProjectRolePermission projectRolePermission);

    /**
     * returns list of all project role permissions mapped to the requested project role
     */
    List<ProjectRolePermission> findAllProjectRolePermissions(Long projectId, Long projectRoleId);

}
