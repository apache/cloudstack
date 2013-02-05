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

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;

// This is the default API access checker that grab's the user's account
// based on the account type, access is granted
@Local(value=APIChecker.class)
public class StaticRoleBasedAPIAccessChecker extends AdapterBase implements APIChecker {

    protected static final Logger s_logger = Logger.getLogger(StaticRoleBasedAPIAccessChecker.class);

    private static Map<RoleType, Set<String>> s_roleBasedApisMap =
            new HashMap<RoleType, Set<String>>();

    @Inject List<PluggableService> _services;
    @Inject AccountService _accountService;

    protected StaticRoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType: RoleType.values())
            s_roleBasedApisMap.put(roleType, new HashSet<String>());
    }

    @Override
    public boolean checkAccess(User user, String commandName)
            throws PermissionDeniedException {
        Account account = _accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException("The account id=" + user.getAccountId() + "for user id=" + user.getId() + "is null");
        }

        RoleType roleType = _accountService.getRoleType(account);
        boolean isAllowed = s_roleBasedApisMap.get(roleType).contains(commandName);
        if (!isAllowed) {
            throw new PermissionDeniedException("The API does not exist or is blacklisted. Role type=" + roleType.toString() + " is not allowed to request the api: " + commandName);
        }
        return isAllowed;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        processMapping(PropertiesUtil.processConfigFile(new String[]
                {"commands.properties"}));
        return true;
    }

    private void processMapping(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry: configMap.entrySet()) {
            String apiName = entry.getKey();
            String roleMask = entry.getValue();
            try {
                short cmdPermissions = Short.parseShort(roleMask);
                for (RoleType roleType: RoleType.values()) {
                    if ((cmdPermissions & roleType.getValue()) != 0)
                        s_roleBasedApisMap.get(roleType).add(apiName);
                }
            } catch (NumberFormatException nfe) {
                s_logger.info("Malformed key=value pair for entry: " + entry.toString());
            }
        }
    }
}
