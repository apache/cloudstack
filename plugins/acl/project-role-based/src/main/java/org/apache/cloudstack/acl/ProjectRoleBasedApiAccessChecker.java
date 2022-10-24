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

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;

import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.UnavailableCommandException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;

public class ProjectRoleBasedApiAccessChecker  extends AdapterBase implements APIAclChecker {

    @Inject
    ProjectAccountDao projectAccountDao;
    @Inject
    ProjectRoleService projectRoleService;
    @Inject
    RoleService roleService;
    @Inject
    AccountService accountService;

    private List<PluggableService> services;
    private static final Logger LOGGER = Logger.getLogger(ProjectRoleBasedApiAccessChecker.class.getName());
    protected ProjectRoleBasedApiAccessChecker() {
        super();
    }

    private void denyApiAccess(final String commandName) throws PermissionDeniedException {
        throw new PermissionDeniedException("The API " + commandName + " is denied for the user's/account's project role.");
    }

    @Override
    public boolean isEnabled() {
        if (!roleService.isEnabled()) {
            LOGGER.trace("RoleService is disabled. We will not use ProjectRoleBasedApiAccessChecker.");
        }
        return roleService.isEnabled();
    }

    @Override
    public List<String> getApisAllowedToUser(Role role, User user, List<String> apiNames) throws PermissionDeniedException {
        if (!isEnabled()) {
            return apiNames;
        }

        Project project = CallContext.current().getProject();
        if (project == null) {
            LOGGER.warn(String.format("Project is null, ProjectRoleBasedApiAccessChecker only applies to projects, returning APIs [%s] for user [%s] as allowed.", apiNames, user));
            return apiNames;
        }

        long accountID = user.getAccountId();
        ProjectAccount projectUser = projectAccountDao.findByProjectIdUserId(project.getId(), accountID, user.getId());
        if (projectUser != null) {
            if (projectUser.getAccountRole() != ProjectAccount.Role.Admin) {
                apiNames.removeIf(apiName -> !isPermitted(project, projectUser, apiName));
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Returning APIs [%s] as allowed for user [%s].", apiNames, user));
            }
            return apiNames;
        }

        ProjectAccount projectAccount = projectAccountDao.findByProjectIdAccountId(project.getId(), accountID);
        if (projectAccount == null) {
            throw new PermissionDeniedException(String.format("The user [%s] does not belong to the project [%s].", user, project));
        }

        if (projectAccount.getAccountRole() != ProjectAccount.Role.Admin) {
            apiNames.removeIf(apiName -> !isPermitted(project, projectAccount, apiName));
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Returning APIs [%s] as allowed for user [%s].", apiNames, user));
        }
        return apiNames;
    }

    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        if (!isEnabled()) {
            return true;
        }

        Project project = CallContext.current().getProject();
        if (project == null) {
            LOGGER.warn(String.format("Project is null, ProjectRoleBasedApiAccessChecker only applies to projects, returning API [%s] for user [%s] as allowed.", apiCommandName,
                user));
            return true;
        }

        Account userAccount = accountService.getAccount(user.getAccountId());
        if (accountService.isRootAdmin(userAccount.getId()) || accountService.isDomainAdmin(userAccount.getAccountId())) {
            LOGGER.info(String.format("Account [%s] is Root Admin or Domain Admin, all APIs are allowed.", userAccount.getAccountName()));
            return true;
        }

        ProjectAccount projectUser = projectAccountDao.findByProjectIdUserId(project.getId(), userAccount.getAccountId(), user.getId());
        if (projectUser != null) {
            if (projectUser.getAccountRole() == ProjectAccount.Role.Admin || isPermitted(project, projectUser, apiCommandName)) {
                return true;
            }
            denyApiAccess(apiCommandName);
        }

        ProjectAccount projectAccount = projectAccountDao.findByProjectIdAccountId(project.getId(), userAccount.getAccountId());
        if (projectAccount != null) {
            if (projectAccount.getAccountRole() == ProjectAccount.Role.Admin || isPermitted(project, projectAccount, apiCommandName)) {
                return true;
            }
            denyApiAccess(apiCommandName);
        }

        // Default deny all
        if ("updateProjectInvitation".equals(apiCommandName)) {
            return true;
        }

        throw new UnavailableCommandException(String.format("The API [%s] does not exist or is not available for this account/user in project [%s].", apiCommandName, project.getUuid()));
    }

    @Override
    public boolean checkAccess(Account account, String apiCommandName) throws PermissionDeniedException {
        return true;
    }

    public boolean isPermitted(Project project, ProjectAccount projectUser, String ... apiCommandNames) {
        ProjectRole projectRole = null;
        if(projectUser.getProjectRoleId() != null) {
            projectRole = projectRoleService.findProjectRole(projectUser.getProjectRoleId(), project.getId());
        }

        if (projectRole == null) {
            return true;
        }

        List<ProjectRolePermission> allProjectRolePermissions = projectRoleService.findAllProjectRolePermissions(project.getId(), projectRole.getId());
        for (String api : apiCommandNames) {
            for (ProjectRolePermission permission : allProjectRolePermissions) {
                if (permission.getRule().matches(api)) {
                    return Permission.ALLOW.equals(permission.getPermission());
                }
            }
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public boolean start() {
        return super.start();
    }

    public List<PluggableService> getServices() {
        return services;
    }

    @Inject
    public void setServices(List<PluggableService> services) {
        this.services = services;
    }
}
