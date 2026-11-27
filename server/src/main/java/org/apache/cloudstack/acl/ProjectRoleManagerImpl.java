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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.dao.ProjectRoleDao;
import org.apache.cloudstack.acl.dao.ProjectRolePermissionsDao;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.api.command.admin.acl.project.CreateProjectRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.project.CreateProjectRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.project.DeleteProjectRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.project.DeleteProjectRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.project.ListProjectRolePermissionsCmd;
import org.apache.cloudstack.api.command.admin.acl.project.ListProjectRolesCmd;
import org.apache.cloudstack.api.command.admin.acl.project.UpdateProjectRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.project.UpdateProjectRolePermissionCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.ListUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class ProjectRoleManagerImpl extends ManagerBase implements ProjectRoleService, PluggableService {
    @Inject
    ProjectAccountDao projAccDao;
    @Inject
    ProjectRoleDao projRoleDao;
    @Inject
    ProjectDao projectDao;
    @Inject
    AccountDao accountDao;
    @Inject
    ProjectRolePermissionsDao projRolePermissionsDao;
    @Inject
    AccountService accountService;
    @Inject
    DomainDao domainDao;


    private Project validateProjectId(Long projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new CloudRuntimeException("Invalid project id provided");
        }
        return project;
    }

    /**
     * Guard for creating, updating and deleting project roles and their permissions. Requires the
     * dynamic API checker to be enabled and the caller to be a root admin, a domain admin whose
     * domain hierarchy covers the project, or a project Admin member.
     */
    private void checkProjectRoleManageAccess(Long projectId) {
        Project project = validateProjectId(projectId);
        CallContext.current().setProject(project);

        if (!isEnabled()) {
            throw new PermissionDeniedException("Dynamic api checker is not enabled, aborting role operation");
        }

        ProjectAccount projectAccount = checkProjectMembership(project);
        if (projectAccount != null && ProjectAccount.Role.Admin != projectAccount.getAccountRole()) {
            throw new PermissionDeniedException("User unauthorized to perform operation in the project");
        }
    }

    /**
     * Guard for looking up project roles and their permissions. Any member of the project is
     * allowed: the API access checkers resolve a member's own project role on every call made in
     * project context, so a lookup must not demand project Admin or the dynamic API checker.
     */
    private void checkProjectRoleReadAccess(Long projectId) {
        Project project = validateProjectId(projectId);
        checkProjectMembership(project);
    }

    /**
     * Returns the caller's membership in the project, or null for a root admin or a domain admin
     * whose domain hierarchy covers the project. Throws when the caller may not see the project.
     */
    private ProjectAccount checkProjectMembership(Project project) {
        User user = getCurrentUser();
        Account callerAcc = accountDao.findById(user.getAccountId());

        if (callerAcc == null || callerAcc.getRoleId() == null) {
            throw new PermissionDeniedException("Restricted API called by an invalid user account");
        }

        if (accountService.isRootAdmin(callerAcc.getId())) {
            return null;
        }

        if (accountService.isDomainAdmin(callerAcc.getAccountId())) {
            // A domain admin may only manage a project that belongs to their own domain hierarchy,
            // not an arbitrary project elsewhere in the system.
            accountService.checkAccess(callerAcc, domainDao.findById(project.getDomainId()));
            return null;
        }

        ProjectAccount projectAccount = projAccDao.findByProjectIdUserId(project.getId(), callerAcc.getAccountId(), user.getId());
        if (projectAccount == null) {
            projectAccount = projAccDao.findByProjectIdAccountId(project.getId(), callerAcc.getAccountId());
            if (projectAccount == null) {
                throw new PermissionDeniedException("User/Account not part of project");
            }
        }
        return projectAccount;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_CREATE, eventDescription = "creating Project Role")
    public ProjectRole createProjectRole(Long projectId, String name, String description) {
        checkProjectRoleManageAccess(projectId);
        return Transaction.execute(new TransactionCallback<ProjectRoleVO>() {
            @Override
            public ProjectRoleVO doInTransaction(TransactionStatus status) {
                return projRoleDao.persist(new ProjectRoleVO(name, description, projectId));
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_UPDATE, eventDescription = "updating Project Role")
    public ProjectRole updateProjectRole(ProjectRole role, Long projectId, String name, String description) {
        checkProjectRoleManageAccess(projectId);
        ProjectRoleVO projectRoleVO = (ProjectRoleVO) role;
        if (StringUtils.isNotEmpty(name)) {
            projectRoleVO.setName(name);
        }
        if (StringUtils.isNotEmpty(description)) {
            projectRoleVO.setDescription(description);
        }
        projRoleDao.update(role.getId(), projectRoleVO);
        return projectRoleVO;
    }

    @Override
    public boolean isEnabled() {
        return RoleService.EnableDynamicApiChecker.value();
    }

    @Override
    public ProjectRole findProjectRole(Long roleId, Long projectId) {
        if (projectId == null || projectId < 1L || projectDao.findById(projectId) == null) {
            logger.warn("Invalid project ID provided");
            return null;
        }

        if (roleId != null && roleId < 1L) {
            logger.warn(String.format("Project Role ID is invalid [%s]", roleId));
            return null;
        }

        checkProjectRoleReadAccess(projectId);

        ProjectRoleVO role = projRoleDao.findById(roleId);
        if (role == null) {
            logger.warn(String.format("Project Role not found [id=%s]", roleId));
            return null;
        }
        if (!(role.getProjectId().equals(projectId))) {
            logger.warn("Project role: {} doesn't belong to the project", role);
            return null;
        }
        return role;
    }

    @Override
    public List<ProjectRole> findProjectRoles(Long projectId, String keyword) {
        if (projectId == null || projectId < 1L || projectDao.findById(projectId) == null) {
            logger.warn("Invalid project ID provided");
            return null;
        }
        checkProjectRoleReadAccess(projectId);
        return ListUtils.toListOfInterface(projRoleDao.findAllRoles(projectId, keyword));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_CREATE, eventDescription = "Creating Project Role Permission")
    public ProjectRolePermission createProjectRolePermission(CreateProjectRolePermissionCmd cmd) {
        Long projectId = cmd.getProjectId();
        Long projectRoleId = cmd.getProjectRoleId();
        Rule rule = cmd.getRule();
        Permission permission = cmd.getPermission();
        String description = cmd.getDescription();
        checkProjectRoleManageAccess(projectId);
        return Transaction.execute(new TransactionCallback<ProjectRolePermissionVO>() {
            @Override
            public ProjectRolePermissionVO doInTransaction(TransactionStatus status) {
                try {
                    return projRolePermissionsDao.persist(new ProjectRolePermissionVO(projectId, projectRoleId, rule.toString(), permission, description));
                } catch (Exception e) {
                    throw new CloudRuntimeException("Project role permission for " + rule.toString()+ " seems to already exist.");
                }
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_UPDATE, eventDescription = "updating Project Role Permission order")
    public boolean updateProjectRolePermission(Long projectId, ProjectRole projectRole, List<ProjectRolePermission> rolePermissionsOrder) {
        checkProjectRoleManageAccess(projectId);
        return projectRole != null && rolePermissionsOrder != null && projRolePermissionsDao.update(projectRole, projectId, rolePermissionsOrder);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_UPDATE, eventDescription = "updating Project Role Permission")
    public boolean updateProjectRolePermission(Long projectId, ProjectRole projectRole, ProjectRolePermission projectRolePermission, Permission newPermission) {
        checkProjectRoleManageAccess(projectId);
        return projectRole != null && projRolePermissionsDao.update(projectRole, projectRolePermission, newPermission);
    }

    @Override
    public ProjectRolePermission findProjectRolePermission(Long projRolePermissionId) {
        if (projRolePermissionId == null) {
            return null;
        }
        return projRolePermissionsDao.findById(projRolePermissionId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_DELETE, eventDescription = "deleting Project Role Permission")
    public boolean deleteProjectRolePermission(ProjectRolePermission projectRolePermission) {
        checkProjectRoleManageAccess(projectRolePermission.getProjectId());
        return projRolePermissionsDao.remove(projectRolePermission.getId());
    }

    @Override
    public List<ProjectRolePermission> findAllProjectRolePermissions(Long projectId, Long projectRoleId) {
        checkProjectRoleReadAccess(projectId);
        List<? extends ProjectRolePermission> permissions = projRolePermissionsDao.findAllByRoleIdSorted(projectRoleId, projectId);
        if (permissions != null) {
            return new ArrayList<>(permissions);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ProjectRole> findProjectRolesByName(Long projectId, String roleName) {
        List<? extends ProjectRole> roles = null;
        checkProjectRoleReadAccess(projectId);
        if (StringUtils.isNotBlank(roleName)) {
            roles = projRoleDao.findByName(roleName, projectId);
        }
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_DELETE, eventDescription = "deleting Project Role")
    public boolean deleteProjectRole(ProjectRole role, Long projectId) {
        checkProjectRoleManageAccess(projectId);
        if (role == null) {
            return false;
        }

        Long roleProjectId = role.getProjectId();
        if (role.getProjectId() != null && !roleProjectId.equals(projectId)) {
            throw new PermissionDeniedException("Not authorized to delete the given project role");
        }

        List<? extends ProjectAccount> users = projAccDao.listUsersOrAccountsByRole(role.getId());
        if (users != null && users.size() != 0) {
            throw new PermissionDeniedException("Found users that have the project role in use, cannot delete the Project Role");
        }
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                List<? extends ProjectRolePermission> rolePermissions = projRolePermissionsDao.findAllByRoleIdSorted(role.getId(), projectId);
                if (rolePermissions != null && !rolePermissions.isEmpty()) {
                    for (ProjectRolePermission rolePermission : rolePermissions) {
                        projRolePermissionsDao.remove(rolePermission.getId());
                    }
                }
                if (projRoleDao.remove(role.getId())) {
                    ProjectRoleVO projRoleVO = projRoleDao.findByIdIncludingRemoved(role.getId());
                    projRoleVO.setName(null);
                    return projRoleDao.update(role.getId(), projRoleVO);
                }
                return false;
            }
        });
    }

    protected Account getCurrentAccount() {
        return CallContext.current().getCallingAccount();
    }

    private Long getProjectIdOfAccount() {
        Project project = projectDao.findByProjectAccountId(getCurrentAccount().getAccountId());
        if (project != null) {
            return project.getId();
        }
        return null;
    }

    protected User getCurrentUser() {
        return CallContext.current().getCallingUser();
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateProjectRoleCmd.class);
        cmdList.add(ListProjectRolesCmd.class);
        cmdList.add(UpdateProjectRoleCmd.class);
        cmdList.add(DeleteProjectRoleCmd.class);
        cmdList.add(CreateProjectRolePermissionCmd.class);
        cmdList.add(ListProjectRolePermissionsCmd.class);
        cmdList.add(UpdateProjectRolePermissionCmd.class);
        cmdList.add(DeleteProjectRolePermissionCmd.class);
        return cmdList;
    }
}
