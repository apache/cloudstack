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
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.PluggableService;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.cloudstack.acl.RoleType.*;
import org.apache.log4j.Logger;

// This is the default API access checker that grab's the user's account
// based on the account type, access is granted
@Local(value=APIAccessChecker.class)
public class StaticRoleBasedAPIAccessChecker extends AdapterBase implements APIAccessChecker {

    protected static final Logger s_logger = Logger.getLogger(StaticRoleBasedAPIAccessChecker.class);
    private static Set<String> s_userCommands = null;
    private static Set<String> s_resellerCommands = null; // AKA domain-admin
    private static Set<String> s_adminCommands = null;
    private static Set<String> s_resourceDomainAdminCommands = null;
    private static Set<String> s_allCommands = null;

    protected StaticRoleBasedAPIAccessChecker() {
        super();
        s_allCommands = new HashSet<String>();
        s_userCommands = new HashSet<String>();
        s_resellerCommands = new HashSet<String>();
        s_adminCommands = new HashSet<String>();
        s_resourceDomainAdminCommands = new HashSet<String>();
    }

    @Override
    public boolean canAccessAPI(RoleType roleType, String commandName)
            throws PermissionDeniedException {

        boolean commandExists = s_allCommands.contains(commandName);
        boolean commandAccessible = false;

        if (commandExists) {
            switch (roleType) {
                case Admin:
                    commandAccessible = s_adminCommands.contains(commandName);
                    break;
                case DomainAdmin:
                    commandAccessible = s_resellerCommands.contains(commandName);
                    break;
                case ResourceAdmin:
                    commandAccessible = s_resourceDomainAdminCommands.contains(commandName);
                    break;
                case User:
                    commandAccessible = s_userCommands.contains(commandName);
                    break;
            }
        }
        return commandExists && commandAccessible;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        // Read command properties files to build the static map per role.
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        List<PluggableService> services = locator.getAllPluggableServices();
        services.add((PluggableService) ComponentLocator.getComponent(ManagementServer.Name));

        Map<String, String> configPropertiesMap = new HashMap<String, String>();
        for (PluggableService service : services) {
            configPropertiesMap.putAll(service.getProperties());
        }

        processConfigFiles(configPropertiesMap);
        return true;
    }

    private void processConfigFiles(Map<String, String> config) {
        for (Map.Entry<String, String> entry: config.entrySet()) {
            String apiName = entry.getKey();
            String roleMask = entry.getValue();
            try {
                short cmdPermissions = Short.parseShort(roleMask);
                if ((cmdPermissions & Admin.getValue()) != 0) {
                    s_adminCommands.add(apiName);
                }
                if ((cmdPermissions & ResourceAdmin.getValue()) != 0) {
                    s_resourceDomainAdminCommands.add(apiName);
                }
                if ((cmdPermissions & DomainAdmin.getValue()) != 0) {
                    s_resellerCommands.add(apiName);
                }
                if ((cmdPermissions & User.getValue()) != 0) {
                    s_userCommands.add(apiName);
                }
            } catch (NumberFormatException nfe) {
                s_logger.info("Malformed commands.properties permissions value, for entry: " + entry.toString());
            }
        }
        s_allCommands.addAll(s_adminCommands);
        s_allCommands.addAll(s_resourceDomainAdminCommands);
        s_allCommands.addAll(s_userCommands);
        s_allCommands.addAll(s_resellerCommands);
    }
}
