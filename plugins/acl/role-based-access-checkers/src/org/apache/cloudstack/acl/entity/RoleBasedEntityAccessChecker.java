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

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.AclEntityPermissionVO;
import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.AclGroupAccountMapVO;
import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.acl.AclRolePermissionVO;
import org.apache.cloudstack.acl.AclService;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.dao.AclEntityPermissionDao;
import org.apache.cloudstack.acl.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.acl.dao.AclGroupDao;
import org.apache.cloudstack.acl.dao.AclRolePermissionDao;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.log4j.Logger;

import com.cloud.acl.DomainChecker;
import com.cloud.api.ApiDispatcher;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.template.VirtualMachineTemplate;
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
    AclEntityPermissionDao _entityPermissionDao;

    @Inject
    AclRolePermissionDao _rolePermissionDao;

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException {
        if (entity instanceof VirtualMachine) {

            String entityType = AclEntityType.VM.toString();

            if (accessType == null) {
                accessType = AccessType.ListEntry;
            }

            // check if explicit allow/deny is present for this entity in
            // acl_entity_permission

            if (entity instanceof InternalIdentity) {
                InternalIdentity entityWithId = (InternalIdentity) entity;

                List<AclGroupAccountMapVO> acctGroups = _aclGroupAccountMapDao.listByAccountId(caller.getId());

                for (AclGroupAccountMapVO groupMapping : acctGroups) {
                    AclEntityPermissionVO entityPermission = _entityPermissionDao.findByGroupAndEntity(
                            groupMapping.getAclGroupId(), entityType, entityWithId.getId(), accessType);

                    if (entityPermission != null) {
                        if (entityPermission.isAllowed()) {
                            return true;
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Account " + caller + " does not have permission to access resource "
                                        + entity + " for access type: " + accessType);
                            }
                            throw new PermissionDeniedException(caller
                                    + " does not have permission to access resource " + entity);
                        }
                    }
                }
            }

            // get all Roles of this caller w.r.t the entity
            List<AclRole> roles = _aclService.getEffectiveRoles(caller, entity);
            HashMap<AclRole, Boolean> rolePermissionMap = new HashMap<AclRole, Boolean>();

            for (AclRole role : roles) {
                List<AclRolePermissionVO> permissions = _rolePermissionDao.listByRoleAndEntity(role.getId(),
                        entityType, accessType);
                for (AclRolePermissionVO permission : permissions) {
                    if (checkPermissionScope(caller, permission.getScope(), entity)) {
                        if (permission.getEntityType().equals(entityType)) {
                            rolePermissionMap.put(role, permission.isAllowed());
                            break;
                        } else if (permission.getEntityType().equals("*")) {
                            rolePermissionMap.put(role, permission.isAllowed());
                        }
                    }
                }
                if (rolePermissionMap.containsKey(role) && rolePermissionMap.get(role)) {
                    return true;
                }
            }

            if (!roles.isEmpty()) { // Since we reach this point, none of the
                                    // roles granted access
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Account " + caller + " does not have permission to access resource " + entity
                            + " for access type: " + accessType);
                }
                throw new PermissionDeniedException(caller + " does not have permission to access resource " + entity);
            }
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
