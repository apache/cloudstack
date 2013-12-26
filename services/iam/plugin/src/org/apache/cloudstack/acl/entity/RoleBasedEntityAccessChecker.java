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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.AclPolicy;
import org.apache.cloudstack.acl.AclPolicyPermissionVO;
import org.apache.cloudstack.acl.AclService;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.acl.dao.AclPolicyPermissionDao;
import org.apache.log4j.Logger;

import com.cloud.acl.DomainChecker;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.vm.VirtualMachine;

public class RoleBasedEntityAccessChecker extends DomainChecker implements SecurityChecker {

    private static final Logger s_logger = Logger.getLogger(RoleBasedEntityAccessChecker.class.getName());

    @Inject
    AccountService _accountService;
    @Inject
    AclService _aclService;
    
    @Inject DomainDao _domainDao;

    @Inject
    AclGroupAccountMapDao _aclGroupAccountMapDao;

    @Inject
    AclPolicyPermissionDao _policyPermissionDao;


    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException {
        return checkAccess(caller, entity, accessType, null);
    }

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType, String action)
            throws PermissionDeniedException {

        String entityType = entity.getEntityType().toString();

        if (accessType == null) {
            accessType = AccessType.ListEntry;
        }

        // get all Policies of this caller w.r.t the entity
        List<AclPolicy> policies = _aclService.getEffectivePolicies(caller, entity);
        HashMap<AclPolicy, Boolean> policyPermissionMap = new HashMap<AclPolicy, Boolean>();

        for (AclPolicy policy : policies) {
            List<AclPolicyPermissionVO> permissions = new ArrayList<AclPolicyPermissionVO>();

            if (action != null) {
                permissions = _policyPermissionDao.listByPolicyActionAndEntity(policy.getId(),
                    action, entityType);
            } else {
                permissions = _policyPermissionDao.listByPolicyAccessAndEntity(policy.getId(), accessType, entityType);
            }
            for (AclPolicyPermissionVO permission : permissions) {
                if (checkPermissionScope(caller, permission.getScope(), entity)) {
                    if (permission.getEntityType().equals(entityType)) {
                        policyPermissionMap.put(policy, permission.getPermission().isGranted());
                        break;
                    } else if (permission.getEntityType().equals("*")) {
                        policyPermissionMap.put(policy, permission.getPermission().isGranted());
                    }
                }
            }
            if (policyPermissionMap.containsKey(policy) && policyPermissionMap.get(policy)) {
                return true;
            }
        }

        if (!policies.isEmpty()) { // Since we reach this point, none of the
                                   // roles granted access
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Account " + caller + " does not have permission to access resource " + entity
                        + " for access type: " + accessType);
            }
            throw new PermissionDeniedException(caller + " does not have permission to access resource " + entity);
        }

        return false;
    }

    private boolean checkPermissionScope(Account caller, PermissionScope scope, ControlledEntity entity) {
        
        if(scope.equals(PermissionScope.ACCOUNT)){
            if(caller.getAccountId() == entity.getAccountId()){
                return true;
            }
        }else if(scope.equals(PermissionScope.DOMAIN)){
            if (_domainDao.isChildDomain(caller.getDomainId(), entity.getDomainId())) {
                return true;
            }
        }
        
        return false;
    }
}
