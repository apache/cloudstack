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
package org.apache.cloudstack.acl.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.acl.AclService;
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
public class RoleBasedAPIAccessChecker extends AdapterBase implements APIChecker {

    protected static final Logger s_logger = Logger.getLogger(RoleBasedAPIAccessChecker.class);

    @Inject AccountService _accountService;
    @Inject AclService _aclService;

    protected RoleBasedAPIAccessChecker() {
        super();
    }

    @Override
    public boolean checkAccess(User user, String commandName)
            throws PermissionDeniedException {
        Account account = _accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException("The account id=" + user.getAccountId() + "for user id=" + user.getId() + "is null");
        }

        List<AclRole> roles = _aclService.getAclRoles(account.getAccountId());


        boolean isAllowed = _aclService.isAPIAccessibleForRoles(commandName, roles);
        if (!isAllowed) {
            throw new PermissionDeniedException("The API does not exist or is blacklisted. api: " + commandName);
        }
        return isAllowed;
    }

}
