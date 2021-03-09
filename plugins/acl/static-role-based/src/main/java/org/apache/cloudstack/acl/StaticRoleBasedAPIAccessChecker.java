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

    public boolean isDisabled() {
        return roleService.isEnabled();
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

        RoleType roleType = accountService.getRoleType(account);
        boolean isAllowed =
            commandsPropertiesOverrides.contains(commandName) ? commandsPropertiesRoleBasedApisMap.get(roleType).contains(commandName) : annotationRoleBasedApisMap.get(
                roleType).contains(commandName);

        if (isAllowed) {
            return true;
        }

        if (commandNames.contains(commandName)) {
            throw new PermissionDeniedException("The API is denied. Role type=" + roleType.toString() + " is not allowed to request the api: " + commandName);
        } else {
            throw new UnavailableCommandException("The API " + commandName + " does not exist or is not available for this account.");
        }
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
                LOGGER.info("Malformed key=value pair for entry: " + entry.toString());
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
