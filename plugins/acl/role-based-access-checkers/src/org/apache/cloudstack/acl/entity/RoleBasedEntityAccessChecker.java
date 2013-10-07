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
package org.apache.cloudstack.acl.entity;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.acl.AclService;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.acl.DomainChecker;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.vm.VirtualMachine;

public class RoleBasedEntityAccessChecker extends DomainChecker implements SecurityChecker {

    @Inject
    AccountService _accountService;
    @Inject
    AclService _aclService;

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException {

        // Is Caller RootAdmin? Yes, granted true
        if (_accountService.isRootAdmin(caller.getId())) {
            return true;
        }
        // Is Caller Owner of the entity? Yes, granted true
        if (caller.getId() == entity.getAccountId()) {
            return true;
        }
        // Get the Roles of the Caller
        List<AclRole> roles = _aclService.getAclRoles(caller.getId());

        // Do you have DomainAdmin Role? If yes can access the entity in the
        // domaintree

        // check the entity grant table



        return false;
    }
}
