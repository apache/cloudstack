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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.UnavailableCommandException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;
import org.apache.commons.lang3.StringUtils;

public class DynamicRoleBasedAPIAccessChecker extends AdapterBase implements APIAclChecker {

    @Inject
    private AccountService accountService;
    @Inject
    private RoleService roleService;

    private List<PluggableService> services;
    private Map<RoleType, Set<String>> annotationRoleBasedApisMap = new HashMap<RoleType, Set<String>>();


    protected DynamicRoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType : RoleType.values()) {
            annotationRoleBasedApisMap.put(roleType, new HashSet<String>());
        }
    }

    @Override
    public List<String> getApisAllowedToUser(Role role, User user, List<String> apiNames) throws PermissionDeniedException {
        if (!isEnabled()) {
            return apiNames;
        }

        List<RolePermission> allPermissions = roleService.findAllPermissionsBy(role.getId());
        List<String> allowedApis = new ArrayList<>();
        for (String api : apiNames) {
            if (checkApiPermissionByRole(role, api, allPermissions)) {
                allowedApis.add(api);
            }
        }
        return allowedApis;
    }

    /**
     * Checks if the given Role of an Account has the allowed permission for the given API.
     *
     * @param role to be used on the verification
     * @param apiName to be verified
     * @param allPermissions list of role permissions for the given role
     * @return if the role has the permission for the API
     */
    public boolean checkApiPermissionByRole(Role role, String apiName, List<RolePermission> allPermissions) {
        for (final RolePermission permission : allPermissions) {
            if (!permission.getRule().matches(apiName)) {
                continue;
            }

            if (!Permission.ALLOW.equals(permission.getPermission())) {
                return false;
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("The API [%s] is allowed for the role %s by the permission [%s].", apiName, role, permission.getRule().toString()));
            }
            return true;
        }
        return annotationRoleBasedApisMap.get(role.getRoleType()) != null &&
                annotationRoleBasedApisMap.get(role.getRoleType()).contains(apiName);
    }

    @Override
    public boolean checkAccess(User user, String commandName) throws PermissionDeniedException {
        if (!isEnabled()) {
            return true;
        }

        Account account = accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException(String.format("The account id [%s] for user id [%s] is null.", user.getAccountId(), user.getUuid()));
        }

        return checkAccess(account, commandName);
    }

    public boolean checkAccess(Account account, String commandName) {
        final Role accountRole = roleService.findRole(account.getRoleId());
        if (accountRole == null || accountRole.getId() < 1L) {
            throw new PermissionDeniedException(String.format("The account [%s] has role null or unknown.", account));
        }

        if (accountRole.getRoleType() == RoleType.Admin && accountRole.getId() == RoleType.Admin.getId()) {
            logger.info(String.format("Account [%s] is Root Admin or Domain Admin, all APIs are allowed.", account));
            return true;
        }

        List<RolePermission> allPermissions = roleService.findAllPermissionsBy(accountRole.getId());
        if (checkApiPermissionByRole(accountRole, commandName, allPermissions)) {
            return true;
        }
        throw new UnavailableCommandException(String.format("The API [%s] does not exist or is not available for the account %s.", commandName, account));
    }

    /**
     * Only one strategy should be used between StaticRoleBasedAPIAccessChecker and DynamicRoleBasedAPIAccessChecker
     * Default behavior is to use the Dynamic version. The StaticRoleBasedAPIAccessChecker is the legacy version.
     * If roleService is enabled, then it uses the DynamicRoleBasedAPIAccessChecker, otherwise, it will use the
     * StaticRoleBasedAPIAccessChecker.
     */
    @Override
    public boolean isEnabled() {
        if (!roleService.isEnabled()) {
            logger.trace("RoleService is disabled. We will not use DynamicRoleBasedAPIAccessChecker.");
        }
        return roleService.isEnabled();
    }

    public void addApiToRoleBasedAnnotationsMap(final RoleType roleType, final String commandName) {
        if (roleType == null || StringUtils.isEmpty(commandName)) {
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
