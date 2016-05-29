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

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;
import com.google.common.base.Strings;
import org.apache.cloudstack.api.APICommand;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Local(value = APIChecker.class)
public class DynamicRoleBasedAPIAccessChecker extends AdapterBase implements APIChecker {

    @Inject
    private AccountService accountService;
    @Inject
    private RoleService roleService;

    private List<PluggableService> services;
    private Map<RoleType, Set<String>> annotationRoleBasedApisMap = new HashMap<>();

    protected DynamicRoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType : RoleType.values()) {
            annotationRoleBasedApisMap.put(roleType, new HashSet<String>());
        }
    }

    private void denyApiAccess(final String commandName) throws PermissionDeniedException {
        throw new PermissionDeniedException("The API does not exist or is blacklisted for the account's role. " +
                "The account with is not allowed to request the api: " + commandName);
    }

    public boolean isDisabled() {
        return !roleService.isEnabled();
    }

    @Override
    public boolean checkAccess(User user, String commandName) throws PermissionDeniedException {
        if (isDisabled()) {
            return true;
        }
        Account account = accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException("The account id=" + user.getAccountId() + "for user id=" + user.getId() + "is null");
        }

        final Role accountRole = roleService.findRole(account.getRoleId());
        if (accountRole == null || accountRole.getId() < 1L) {
            denyApiAccess(commandName);
        }

        // Allow all APIs for root admins
        if (accountRole.getRoleType() == RoleType.Admin && accountRole.getId() == RoleType.Admin.getId()) {
            return true;
        }

        // Check against current list of permissions
        for (final RolePermission permission : roleService.findAllPermissionsBy(accountRole.getId())) {
            if (permission.getRule().matches(commandName)) {
                if (RolePermission.Permission.ALLOW.equals(permission.getPermission())) {
                    return true;
                } else {
                    denyApiAccess(commandName);
                }
            }
        }

        // Check annotations
        if (annotationRoleBasedApisMap.get(accountRole.getRoleType()) != null
                && annotationRoleBasedApisMap.get(accountRole.getRoleType()).contains(commandName)) {
            return true;
        }

        // Default deny all
        denyApiAccess(commandName);
        return false;
    }

    public void addApiToRoleBasedAnnotationsMap(final RoleType roleType, final String commandName) {
        if (roleType == null || Strings.isNullOrEmpty(commandName)) {
            return;
        }
        final Set<String> commands = annotationRoleBasedApisMap.get(roleType);
        if (commands != null && !commands.contains(commandName)) {
            commands.add(commandName);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public boolean start() {
        for (PluggableService service : services) {
            for (Class<?> clz : service.getCommands()) {
                APICommand command = clz.getAnnotation(APICommand.class);
                for (RoleType role : command.authorized()) {
                    addApiToRoleBasedAnnotationsMap(role, command.name());
                }
            }
        }
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
