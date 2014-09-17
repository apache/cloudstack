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
package com.cloud.acl;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;

import com.cloud.domain.DomainVO;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = SecurityChecker.class)
public class AffinityGroupAccessChecker extends DomainChecker {

    @Inject
    AffinityGroupService _affinityGroupService;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AffinityGroupDomainMapDao _affinityGroupDomainMapDao;

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType) throws PermissionDeniedException {
        if (entity instanceof AffinityGroup) {
            AffinityGroup group = (AffinityGroup)entity;

            if (_affinityGroupService.isAdminControlledGroup(group)) {
                if (accessType != null && accessType == AccessType.OperateEntry
                        && !_accountMgr.isRootAdmin(caller.getId())) {
                    throw new PermissionDeniedException(caller + " does not have permission to operate with resource "
                            + entity);
                }
            }

            if (group.getAclType() == ACLType.Domain) {
                if (!_affinityGroupService.isAffinityGroupAvailableInDomain(group.getId(), caller.getDomainId())) {
                    DomainVO callerDomain = _domainDao.findById(caller.getDomainId());
                    if (callerDomain == null) {
                        throw new CloudRuntimeException("cannot check permission on account " + caller.getAccountName() + " whose domain does not exist");
                    }

                    throw new PermissionDeniedException("Affinity group is not available in domain id=" + callerDomain.getUuid());
                } else {
                    return true;
                }
            } else {
                //acl_type account
                if (caller.getId() != group.getAccountId()) {
                    throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                } else {
                    return true;
                }

            }

        }

        return false;
    }
}
