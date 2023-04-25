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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.exception.UnavailableCommandException;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;

// This is the default API access checker that grab's the user's account
// based on the account type, access is granted
@Deprecated
public class StaticRoleBasedAPIAccessChecker extends AdapterBase implements APIAclChecker {

    protected static final Logger LOGGER = Logger.getLogger(StaticRoleBasedAPIAccessChecker.class);

    private Set<String> commandPropertyFiles = new HashSet<String>();
    private Set<String> commandNames = new HashSet<String>();
    private Set<String> commandsPropertiesOverrides = new HashSet<String>();
    private Map<RoleType, Set<String>> commandsPropertiesRoleBasedApisMap = new HashMap<RoleType, Set<String>>();
    private Map<RoleType, Set<String>> annotationRoleBasedApisMap = new HashMap<RoleType, Set<String>>();
    private List<PluggableService> services;

    @Inject
    private AccountService accountService;
    @Inject
    private RoleService roleService;

    public StaticRoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType : RoleType.values()) {
            commandsPropertiesRoleBasedApisMap.put(roleType, new HashSet<String>());
            annotationRoleBasedApisMap.put(roleType, new HashSet<String>());
        }
    }

    /**
     * Only one strategy should be used between StaticRoleBasedAPIAccessChecker and DynamicRoleBasedAPIAccessChecker
     * Default behavior is to use the Dynamic version. The StaticRoleBasedAPIAccessChecker is the legacy version.
     * If roleService is enabled, then it uses the DynamicRoleBasedAPIAccessChecker, otherwise, it will use the
     * StaticRoleBasedAPIAccessChecker.
     */
    @Override
    public boolean isEnabled() {
        if (roleService.isEnabled()) {
            LOGGER.debug("RoleService is enabled. We will use it instead of StaticRoleBasedAPIAccessChecker.");
        }
        return roleService.isEnabled();
    }

    @Override
    public List<String> getApisAllowedToUser(Role role, User user, List<String> apiNames) throws PermissionDeniedException {
        if (isEnabled()) {
            return apiNames;
        }

        RoleType roleType = role.getRoleType();
        apiNames.removeIf(apiName -> !isApiAllowed(apiName, roleType));

        return apiNames;
    }

    @Override
    public boolean checkAccess(User user, String commandName) throws PermissionDeniedException {
        if (isEnabled()) {
            return true;
        }

        Account account = accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException(String.format("The account with id [%s] for user with uuid [%s] is null.", user.getAccountId(), user.getUuid()));
        }

        return checkAccess(account, commandName);
    }

    @Override
    public boolean checkAccess(Account account, String commandName) {
        RoleType roleType = accountService.getRoleType(account);
        if (isApiAllowed(commandName, roleType)) {
            return true;
        }

        if (commandNames.contains(commandName)) {
            throw new PermissionDeniedException(String.format("Request to API [%s] was denied. The role type [%s] is not allowed to request it.", commandName, roleType.toString()));
        } else {
            throw new UnavailableCommandException(String.format("The API [%s] does not exist or is not available for this account.", commandName));
        }
    }

    /**
     * Verifies if the API is allowed for the given RoleType.
     *
     * @param apiName API command to be verified
     * @param roleType to be verified
     * @return 'true' if the API is allowed for the given RoleType, otherwise, 'false'.
     */
    public boolean isApiAllowed(String apiName, RoleType roleType) {
        if (commandsPropertiesOverrides.contains(apiName)) {
            return commandsPropertiesRoleBasedApisMap.get(roleType).contains(apiName);
        }
        return annotationRoleBasedApisMap.get(roleType).contains(apiName);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        for (String commandPropertyFile : commandPropertyFiles) {
            processMapping(PropertiesUtil.processConfigFile(new String[] { commandPropertyFile }));
        }
        return true;
    }

    @Override
    public boolean start() {
        for (PluggableService service : services) {
            for (Class<?> clz : service.getCommands()) {
                APICommand command = clz.getAnnotation(APICommand.class);
                for (RoleType role : command.authorized()) {
                    Set<String> commands = annotationRoleBasedApisMap.get(role);
                    if (!commands.contains(command.name()))
                        commands.add(command.name());
                }
                if (!commandNames.contains(command.name())) {
                    commandNames.add(command.name());
                }
            }
        }
        return super.start();
    }

    private void processMapping(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String apiName = entry.getKey();
            String roleMask = entry.getValue();
            if (!commandNames.contains(apiName)) {
                commandNames.add(apiName);
            }
            commandsPropertiesOverrides.add(apiName);
            try {
                short cmdPermissions = Short.parseShort(roleMask);
                for (RoleType roleType : RoleType.values()) {
                    if ((cmdPermissions & roleType.getMask()) != 0)
                        commandsPropertiesRoleBasedApisMap.get(roleType).add(apiName);
                }
            } catch (NumberFormatException nfe) {
                LOGGER.error(String.format("Malformed key=value pair for entry: [%s].", entry));
            }
        }
    }

    public List<PluggableService> getServices() {
        return services;
    }

    @Inject
    public void setServices(List<PluggableService> services) {
        this.services = services;
    }

    public Set<String> getCommandPropertyFiles() {
        return commandPropertyFiles;
    }

    public void setCommandPropertyFiles(Set<String> commandPropertyFiles) {
        this.commandPropertyFiles = commandPropertyFiles;
    }

}
