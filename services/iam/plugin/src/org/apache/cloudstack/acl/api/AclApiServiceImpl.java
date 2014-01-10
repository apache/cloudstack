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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.api.command.AddAccountToAclGroupCmd;
import org.apache.cloudstack.acl.api.command.AddAclPermissionToAclPolicyCmd;
import org.apache.cloudstack.acl.api.command.AttachAclPolicyToAclGroupCmd;
import org.apache.cloudstack.acl.api.command.CreateAclGroupCmd;
import org.apache.cloudstack.acl.api.command.CreateAclPolicyCmd;
import org.apache.cloudstack.acl.api.command.DeleteAclGroupCmd;
import org.apache.cloudstack.acl.api.command.DeleteAclPolicyCmd;
import org.apache.cloudstack.acl.api.command.ListAclGroupsCmd;
import org.apache.cloudstack.acl.api.command.ListAclPoliciesCmd;
import org.apache.cloudstack.acl.api.command.RemoveAccountFromAclGroupCmd;
import org.apache.cloudstack.acl.api.command.RemoveAclPermissionFromAclPolicyCmd;
import org.apache.cloudstack.acl.api.command.RemoveAclPolicyFromAclGroupCmd;
import org.apache.cloudstack.acl.api.response.AclGroupResponse;
import org.apache.cloudstack.acl.api.response.AclPermissionResponse;
import org.apache.cloudstack.acl.api.response.AclPolicyResponse;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.iam.api.AclGroup;
import org.apache.cloudstack.iam.api.AclPolicy;
import org.apache.cloudstack.iam.api.AclPolicyPermission;
import org.apache.cloudstack.iam.api.AclPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.api.ApiServerService;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;

@Local(value = {AclApiService.class})
public class AclApiServiceImpl extends ManagerBase implements AclApiService, Manager {

    public static final Logger s_logger = Logger.getLogger(AclApiServiceImpl.class);
    private String _name;

    @Inject
    ApiServerService _apiServer;

    @Inject
    IAMService _iamSrv;

    @Inject
    DomainDao _domainDao;

    @Inject
    AccountDao _accountDao;

    @Inject
    AccountManager _accountMgr;

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_CREATE, eventDescription = "Creating Acl Group", create = true)
    public AclGroup createAclGroup(Account caller, String aclGroupName, String description) {
        Long domainId = caller.getDomainId();
        Domain callerDomain = _domainDao.findById(domainId);
        if (callerDomain == null) {
            throw new InvalidParameterValueException("Caller does not have a domain");
        }
        return _iamSrv.createAclGroup(aclGroupName, description, callerDomain.getPath());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_DELETE, eventDescription = "Deleting Acl Group")
    public boolean deleteAclGroup(final Long aclGroupId) {
        return _iamSrv.deleteAclGroup(aclGroupId);
    }

    @Override
    public List<AclGroup> listAclGroups(long accountId) {
        return _iamSrv.listAclGroups(accountId);
    }

    @Override
    public List<String> listAclGroupsByAccount(long accountId) {
        List<AclGroup> groups = listAclGroups(accountId);
        List<String> groupNames = new ArrayList<String>();
        for (AclGroup grp : groups) {
            groupNames.add(grp.getName());
        }
        return groupNames;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Adding accounts to acl group")
    public AclGroup addAccountsToGroup(final List<Long> acctIds, final Long groupId) {
        return _iamSrv.addAccountsToGroup(acctIds, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing accounts from acl group")
    public AclGroup removeAccountsFromGroup(final List<Long> acctIds, final Long groupId) {
        return _iamSrv.removeAccountsFromGroup(acctIds, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_CREATE, eventDescription = "Creating Acl Policy", create = true)
    public AclPolicy createAclPolicy(Account caller, final String aclPolicyName, final String description, final Long parentPolicyId) {
        return _iamSrv.createAclPolicy(aclPolicyName, description, parentPolicyId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_DELETE, eventDescription = "Deleting Acl Policy")
    public boolean deleteAclPolicy(final long aclPolicyId) {
        return _iamSrv.deleteAclPolicy(aclPolicyId);
    }


    @Override
    public List<AclPolicy> listAclPolicies(long accountId) {
        return _iamSrv.listAclPolicies(accountId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Attaching policy to acl group")
    public AclGroup attachAclPoliciesToGroup(final List<Long> policyIds, final Long groupId) {
        return _iamSrv.attachAclPoliciesToGroup(policyIds, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing policies from acl group")
    public AclGroup removeAclPoliciesFromGroup(final List<Long> policyIds, final Long groupId) {
        return _iamSrv.removeAclPoliciesFromGroup(policyIds, groupId);
    }


    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_GRANT, eventDescription = "Granting acl permission to Acl Policy")
    public AclPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, PermissionScope scope, Long scopeId, String action, Permission perm) {
        Class<?> cmdClass = _apiServer.getCmdClass(action);
        AccessType accessType = null;
        if (BaseListCmd.class.isAssignableFrom(cmdClass)) {
            accessType = AccessType.ListEntry;
        }
        return _iamSrv.addAclPermissionToAclPolicy(aclPolicyId, entityType, scope.toString(), scopeId, action, accessType.toString(), perm);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_REVOKE, eventDescription = "Revoking acl permission from Acl Policy")
    public AclPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, PermissionScope scope, Long scopeId, String action) {
        return _iamSrv.removeAclPermissionFromAclPolicy(aclPolicyId, entityType, scope.toString(), scopeId, action);
    }

    @Override
    public AclPolicyPermission getAclPolicyPermission(long accountId, String entityType, String action) {
        List<AclPolicy> policies = _iamSrv.listAclPolicies(accountId);
        AclPolicyPermission curPerm = null;
        for (AclPolicy policy : policies) {
            List<AclPolicyPermission> perms = _iamSrv.listPolicyPermissionByEntityType(policy.getId(), action,
                    entityType);
            if (perms == null || perms.size() == 0)
                continue;
            AclPolicyPermission perm = perms.get(0); // just pick one
            if (curPerm == null) {
                curPerm = perm;
            } else if (PermissionScope.valueOf(perm.getScope()).greaterThan(PermissionScope.valueOf(curPerm.getScope()))) {
                // pick the more relaxed allowed permission
                curPerm = perm;
            }
        }

        return curPerm;
    }


    @Override
    public AclPolicyResponse createAclPolicyResponse(AclPolicy policy) {
        AclPolicyResponse response = new AclPolicyResponse();
        response.setId(policy.getUuid());
        response.setName(policy.getName());
        response.setDescription(policy.getDescription());
        String domainPath = policy.getPath();
        if (domainPath != null) {
            DomainVO domain = _domainDao.findDomainByPath(domainPath);
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
            }
        }
        long accountId = policy.getAccountId();
        AccountVO owner = _accountDao.findById(accountId);
        if (owner != null) {
            response.setAccountName(owner.getAccountName());
        }
        // find permissions associated with this policy
        List<AclPolicyPermission> permissions = _iamSrv.listPolicyPermissions(policy.getId());
        if (permissions != null && permissions.size() > 0) {
            for (AclPolicyPermission permission : permissions) {
                AclPermissionResponse perm = new AclPermissionResponse();
                perm.setAction(permission.getAction());
                perm.setEntityType(AclEntityType.valueOf(permission.getEntityType()));
                perm.setScope(PermissionScope.valueOf(permission.getScope()));
                perm.setScopeId(permission.getScopeId());
                perm.setPermission(permission.getPermission());
                response.addPermission(perm);
            }
        }
        response.setObjectName("aclpolicy");
        return response;
    }

    @Override
    public AclGroupResponse createAclGroupResponse(AclGroup group) {
        AclGroupResponse response = new AclGroupResponse();
        response.setId(group.getUuid());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        String domainPath = group.getPath();
        if (domainPath != null) {
            DomainVO domain = _domainDao.findDomainByPath(domainPath);
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
            }
        }
        long accountId = group.getAccountId();
        AccountVO owner = _accountDao.findById(accountId);
        if (owner != null) {
            response.setAccountName(owner.getAccountName());
        }
        // find all the members in this group
        List<Long> members = _iamSrv.listAccountsByGroup(group.getId());
        if (members != null && members.size() > 0) {
            for (Long member : members) {
                AccountVO mem = _accountDao.findById(member);
                if (mem != null) {
                    response.addMemberAccount(mem.getAccountName());
                }
            }
        }

        // find all the policies attached to this group
        List<AclPolicy> policies = _iamSrv.listAclPoliciesByGroup(group.getId());
        if (policies != null && policies.size() > 0) {
            for (AclPolicy policy : policies) {
                response.addPolicy(policy.getName());
            }
        }

        response.setObjectName("aclgroup");
        return response;

    }

    @Override
    public ListResponse<AclGroupResponse> listAclGroups(Long aclGroupId, String aclGroupName, Long domainId, Long startIndex, Long pageSize) {
        // acl check
        Account caller = CallContext.current().getCallingAccount();

        Domain domain = null;
        if (domainId != null) {
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }

            _accountMgr.checkAccess(caller, domain);
        } else {
            domain = _domainDao.findById(caller.getDomainId());
        }
        String domainPath = domain.getPath();
        // search for groups
        Pair<List<AclGroup>, Integer> result = _iamSrv.listAclGroups(aclGroupId, aclGroupName, domainPath, startIndex, pageSize);
        // generate group response
        ListResponse<AclGroupResponse> response = new ListResponse<AclGroupResponse>();
        List<AclGroupResponse> groupResponses = new ArrayList<AclGroupResponse>();
        for (AclGroup group : result.first()) {
            AclGroupResponse resp = createAclGroupResponse(group);
            groupResponses.add(resp);
        }
        response.setResponses(groupResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<AclPolicyResponse> listAclPolicies(Long aclPolicyId, String aclPolicyName, Long domainId, Long startIndex,
            Long pageSize) {
        // acl check
        Account caller = CallContext.current().getCallingAccount();

        Domain domain = null;
        if (domainId != null) {
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }

            _accountMgr.checkAccess(caller, domain);
        } else {
            domain = _domainDao.findById(caller.getDomainId());
        }
        String domainPath = domain.getPath();
        // search for policies
        Pair<List<AclPolicy>, Integer> result = _iamSrv.listAclPolicies(aclPolicyId, aclPolicyName, domainPath, startIndex, pageSize);
        // generate policy response
        ListResponse<AclPolicyResponse> response = new ListResponse<AclPolicyResponse>();
        List<AclPolicyResponse> policyResponses = new ArrayList<AclPolicyResponse>();
        for (AclPolicy policy : result.first()) {
            AclPolicyResponse resp = createAclPolicyResponse(policy);
            policyResponses.add(resp);
        }
        response.setResponses(policyResponses, result.second());
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateAclPolicyCmd.class);
        cmdList.add(DeleteAclPolicyCmd.class);
        cmdList.add(ListAclPoliciesCmd.class);
        cmdList.add(AddAclPermissionToAclPolicyCmd.class);
        cmdList.add(RemoveAclPermissionFromAclPolicyCmd.class);
        cmdList.add(AttachAclPolicyToAclGroupCmd.class);
        cmdList.add(RemoveAclPolicyFromAclGroupCmd.class);
        cmdList.add(CreateAclGroupCmd.class);
        cmdList.add(DeleteAclGroupCmd.class);
        cmdList.add(ListAclGroupsCmd.class);
        cmdList.add(AddAccountToAclGroupCmd.class);
        cmdList.add(RemoveAccountFromAclGroupCmd.class);
        return cmdList;
    }
}
