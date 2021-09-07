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


    public boolean isDisabled() {
        return !roleService.isEnabled();
    }

    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        if (isDisabled()) {
            return true;
        }

        Account userAccount = accountService.getAccount(user.getAccountId());
        Project project = CallContext.current().getProject();
        if (project == null) {
            return true;
        }

        if (accountService.isRootAdmin(userAccount.getId()) || accountService.isDomainAdmin(userAccount.getAccountId())) {
            return true;
        }

        ProjectAccount projectUser = projectAccountDao.findByProjectIdUserId(project.getId(), userAccount.getAccountId(), user.getId());
        if (projectUser != null) {
            if (projectUser.getAccountRole() == ProjectAccount.Role.Admin) {
                return true;
            } else {
                return isPermitted(project, projectUser, apiCommandName);
            }
        }

        ProjectAccount projectAccount = projectAccountDao.findByProjectIdAccountId(project.getId(), userAccount.getAccountId());
        if (projectAccount != null) {
            if (projectAccount.getAccountRole() == ProjectAccount.Role.Admin) {
                return true;
            } else {
                return isPermitted(project, projectAccount, apiCommandName);
            }
        }
        // Default deny all
        if ("updateProjectInvitation".equals(apiCommandName)) {
            return true;
        }
        throw new UnavailableCommandException("The API " + apiCommandName + " does not exist or is not available for this account/user in project "+project.getUuid());
    }

    private boolean isPermitted(Project project, ProjectAccount projectUser, String apiCommandName) {
        ProjectRole projectRole = null;
        if(projectUser.getProjectRoleId() != null) {
            projectRole = projectRoleService.findProjectRole(projectUser.getProjectRoleId(), project.getId());
        }

        if (projectRole == null) {
            return true;
        }

        for (ProjectRolePermission permission : projectRoleService.findAllProjectRolePermissions(project.getId(), projectRole.getId())) {
            if (permission.getRule().matches(apiCommandName)) {
                if (Permission.ALLOW.equals(permission.getPermission())) {
                    return true;
                } else {
                    denyApiAccess(apiCommandName);
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
