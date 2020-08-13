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
import org.apache.log4j.Logger;

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
import com.google.common.base.Strings;

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

    private static final Logger LOGGER = Logger.getLogger(ProjectRoleManagerImpl.class);

    private Project validateProjectId(Long projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new CloudRuntimeException("Invalid project id provided");
        }
        return project;
    }

    private void checkAccess(Long projectId) {
        Project project = validateProjectId(projectId);
        CallContext.current().setProject(project);

        if (!isEnabled()) {
            throw new PermissionDeniedException("Dynamic api checker is not enabled, aborting role operation");
        }

        User user = getCurrentUser();
        Account callerAcc = accountDao.findById(user.getAccountId());

        if (callerAcc == null || callerAcc.getRoleId() == null) {
            throw new PermissionDeniedException("Restricted API called by an invalid user account");
        }

        if (accountService.isRootAdmin(callerAcc.getId()) || accountService.isDomainAdmin(callerAcc.getAccountId())) {
            return;
        }

        ProjectAccount projectAccount = projAccDao.findByProjectIdUserId(projectId, callerAcc.getAccountId(), user.getId());
        if (projectAccount == null) {
            projectAccount = projAccDao.findByProjectIdAccountId(projectId, callerAcc.getAccountId());
            if (projectAccount == null) {
                throw new PermissionDeniedException("User/Account not part of project");
            }
        }
        if (ProjectAccount.Role.Admin != projectAccount.getAccountRole()) {
            throw new PermissionDeniedException("User unauthorized to perform operation in the project");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_CREATE, eventDescription = "creating Project Role")
    public ProjectRole createProjectRole(Long projectId, String name, String description) {
        checkAccess(projectId);
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
        checkAccess(projectId);
        ProjectRoleVO projectRoleVO = (ProjectRoleVO) role;
        if (!Strings.isNullOrEmpty(name)) {
            projectRoleVO.setName(name);
        }
        if (!Strings.isNullOrEmpty(description)) {
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
            LOGGER.warn("Invalid project ID provided");
            return null;
        }

        if (roleId != null && roleId < 1L) {
            LOGGER.warn(String.format("Project Role ID is invalid [%s]", roleId));
            return null;
        }

        ProjectRoleVO role = projRoleDao.findById(roleId);
        if (role == null) {
            LOGGER.warn(String.format("Project Role not found [id=%s]", roleId));
            return null;
        }
        if (!(role.getProjectId().equals(projectId))) {
            LOGGER.warn(String.format("Project role : %s doesn't belong to the project" + role.getName()));
            return null;
        }
        return role;
    }

    @Override
    public List<ProjectRole> findProjectRoles(Long projectId) {
        if (projectId == null || projectId < 1L || projectDao.findById(projectId) == null) {
            LOGGER.warn("Invalid project ID provided");
            return null;
        }
        return ListUtils.toListOfInterface(projRoleDao.findAllRoles(projectId));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_CREATE, eventDescription = "Creating Project Role Permission")
    public ProjectRolePermission createProjectRolePermission(CreateProjectRolePermissionCmd cmd) {
        Long projectId = cmd.getProjectId();
        Long projectRoleId = cmd.getProjectRoleId();
        Rule rule = cmd.getRule();
        Permission permission = cmd.getPermission();
        String description = cmd.getDescription();
        checkAccess(projectId);
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
        checkAccess(projectId);
        return projectRole != null && rolePermissionsOrder != null && projRolePermissionsDao.update(projectRole, projectId, rolePermissionsOrder);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_PERMISSION_UPDATE, eventDescription = "updating Project Role Permission")
    public boolean updateProjectRolePermission(Long projectId, ProjectRole projectRole, ProjectRolePermission projectRolePermission, Permission newPermission) {
        checkAccess(projectId);
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
        checkAccess(projectRolePermission.getProjectId());
        return projRolePermissionsDao.remove(projectRolePermission.getId());
    }

    @Override
    public List<ProjectRolePermission> findAllProjectRolePermissions(Long projectId, Long projectRoleId) {
        List<? extends ProjectRolePermission> permissions = projRolePermissionsDao.findAllByRoleIdSorted(projectRoleId, projectId);
        if (permissions != null) {
            return new ArrayList<>(permissions);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ProjectRole> findProjectRolesByName(Long projectId, String roleName) {
        List<? extends ProjectRole> roles = null;
        if (StringUtils.isNotBlank(roleName)) {
            roles = projRoleDao.findByName(roleName, projectId);
        }
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ROLE_DELETE, eventDescription = "deleting Project Role")
    public boolean deleteProjectRole(ProjectRole role, Long projectId) {
        checkAccess(projectId);
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
