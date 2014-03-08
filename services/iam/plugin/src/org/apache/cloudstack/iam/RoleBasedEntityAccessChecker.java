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
package org.apache.cloudstack.iam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.iam.api.IAMGroup;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.acl.DomainChecker;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

public class RoleBasedEntityAccessChecker extends DomainChecker implements SecurityChecker {

    private static final Logger s_logger = Logger.getLogger(RoleBasedEntityAccessChecker.class.getName());

    @Inject
    AccountService _accountService;

    @Inject DomainDao _domainDao;

    @Inject
    IAMService _iamSrv;


    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException {
        return checkAccess(caller, entity, accessType, null);
    }

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType, String action)
            throws PermissionDeniedException {

        if (entity == null && action != null) {
            // check if caller can do this action
            List<IAMPolicy> policies = _iamSrv.listIAMPolicies(caller.getAccountId());

            boolean isAllowed = _iamSrv.isActionAllowedForPolicies(action, policies);
            if (!isAllowed) {
                throw new PermissionDeniedException("The action '" + action + "' not allowed for account " + caller);
            }
            return true;
        }

        String entityType = entity.getEntityType().toString();

        if (accessType == null) {
            accessType = AccessType.UseEntry;
        }

        // get all Policies of this caller w.r.t the entity
        List<IAMPolicy> policies = getEffectivePolicies(caller, entity);
        HashMap<IAMPolicy, Boolean> policyPermissionMap = new HashMap<IAMPolicy, Boolean>();

        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> permissions = new ArrayList<IAMPolicyPermission>();

            if (action != null) {
                permissions = _iamSrv.listPolicyPermissionByActionAndEntity(policy.getId(), action, entityType);
                if (permissions.isEmpty()) {
                    if (accessType != null) {
                        permissions.addAll(_iamSrv.listPolicyPermissionByAccessAndEntity(policy.getId(),
                                accessType.toString(), entityType));
                    }
                }
            } else {
                if (accessType != null) {
                    permissions.addAll(_iamSrv.listPolicyPermissionByAccessAndEntity(policy.getId(),
                            accessType.toString(), entityType));
                }
            }
            for (IAMPolicyPermission permission : permissions) {
                if (checkPermissionScope(caller, permission.getScope(), permission.getScopeId(), entity)) {
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

    private boolean checkPermissionScope(Account caller, String scope, Long scopeId, ControlledEntity entity) {

        if(scopeId != null && !scopeId.equals(new Long(IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER))){
            //scopeId is set
            if (scope.equals(PermissionScope.ACCOUNT.name())) {
                if(scopeId == entity.getAccountId()){
                    return true;
                }
            } else if (scope.equals(PermissionScope.DOMAIN.name())) {
                if (_domainDao.isChildDomain(scopeId, entity.getDomainId())) {
                    return true;
                }
            } else if (scope.equals(PermissionScope.RESOURCE.name())) {
                if (entity instanceof InternalIdentity) {
                    InternalIdentity entityWithId = (InternalIdentity) entity;
                    if(scopeId.equals(entityWithId.getId())){
                        return true;
                    }
                }
            }
        } else if (scopeId == null || scopeId.equals(new Long(IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER))) {
            if (scope.equals(PermissionScope.ACCOUNT.name())) {
                if(caller.getAccountId() == entity.getAccountId()){
                    return true;
                }
            } else if (scope.equals(PermissionScope.DOMAIN.name())) {
                if (_domainDao.isChildDomain(caller.getDomainId(), entity.getDomainId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<IAMPolicy> getEffectivePolicies(Account caller, ControlledEntity entity) {

        // Get the static Policies of the Caller
        List<IAMPolicy> policies = _iamSrv.listIAMPolicies(caller.getId());

        // add any dynamic policies w.r.t the entity
        if (caller.getId() == entity.getAccountId()) {
            // The caller owns the entity
            policies.add(_iamSrv.getResourceOwnerPolicy());
        }

        List<IAMGroup> groups = _iamSrv.listIAMGroups(caller.getId());
        for (IAMGroup group : groups) {
            // for each group find the grand parent groups.
            List<IAMGroup> parentGroups = _iamSrv.listParentIAMGroups(group.getId());
            for (IAMGroup parentGroup : parentGroups) {
                policies.addAll(_iamSrv.listRecursiveIAMPoliciesByGroup(parentGroup.getId()));
            }
        }

        return policies;
    }
}
