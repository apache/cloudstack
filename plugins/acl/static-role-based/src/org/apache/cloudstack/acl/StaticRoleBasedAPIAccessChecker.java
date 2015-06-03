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

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

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
@Local(value = APIChecker.class)
public class StaticRoleBasedAPIAccessChecker extends AdapterBase implements APIChecker {

    protected static final Logger s_logger = Logger.getLogger(StaticRoleBasedAPIAccessChecker.class);

    Set<String> commandPropertyFiles = new HashSet<String>();
    Set<String> commandsPropertiesOverrides = new HashSet<String>();
    Map<RoleType, Set<String>> commandsPropertiesRoleBasedApisMap = new HashMap<RoleType, Set<String>>();
    Map<RoleType, Set<String>> annotationRoleBasedApisMap = new HashMap<RoleType, Set<String>>();

    List<PluggableService> _services;
    @Inject
    AccountService _accountService;

    protected StaticRoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType : RoleType.values()) {
            commandsPropertiesRoleBasedApisMap.put(roleType, new HashSet<String>());
            annotationRoleBasedApisMap.put(roleType, new HashSet<String>());
        }
    }

    @Override
    public boolean checkAccess(User user, String commandName) throws PermissionDeniedException {
        Account account = _accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException("The account id=" + user.getAccountId() + "for user id=" + user.getId() + "is null");
        }

        RoleType roleType = _accountService.getRoleType(account);
        boolean isAllowed =
            commandsPropertiesOverrides.contains(commandName) ? commandsPropertiesRoleBasedApisMap.get(roleType).contains(commandName) : annotationRoleBasedApisMap.get(
                roleType).contains(commandName);

        if (!isAllowed) {
            throw new PermissionDeniedException("The API does not exist or is blacklisted. Role type=" + roleType.toString() + " is not allowed to request the api: " +
                commandName);
        }
        return isAllowed;
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
        for (PluggableService service : _services) {
            for (Class<?> clz : service.getCommands()) {
                APICommand command = clz.getAnnotation(APICommand.class);
                for (RoleType role : command.authorized()) {
                    Set<String> commands = annotationRoleBasedApisMap.get(role);
                    if (!commands.contains(command.name()))
                        commands.add(command.name());
                }
            }
        }
        return super.start();
    }

    private void processMapping(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String apiName = entry.getKey();
            String roleMask = entry.getValue();
            commandsPropertiesOverrides.add(apiName);
            try {
                short cmdPermissions = Short.parseShort(roleMask);
                for (RoleType roleType : RoleType.values()) {
                    if ((cmdPermissions & roleType.getValue()) != 0)
                        commandsPropertiesRoleBasedApisMap.get(roleType).add(apiName);
                }
            } catch (NumberFormatException nfe) {
                s_logger.info("Malformed key=value pair for entry: " + entry.toString());
            }
        }
    }

    public List<PluggableService> getServices() {
        return _services;
    }

    @Inject
    public void setServices(List<PluggableService> services) {
        this._services = services;
    }

    public Set<String> getCommandPropertyFiles() {
        return commandPropertyFiles;
    }

    public void setCommandPropertyFiles(Set<String> commandPropertyFiles) {
        this.commandPropertyFiles = commandPropertyFiles;
    }

}
