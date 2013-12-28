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
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.api.response.AclGroupResponse;
import org.apache.cloudstack.acl.api.response.AclPolicyResponse;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.iam.api.AclGroup;
import org.apache.cloudstack.iam.api.AclPolicy;
import org.apache.cloudstack.iam.api.AclPolicyPermission;
import org.apache.cloudstack.iam.api.AclPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.api.ApiServerService;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
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


    public static HashMap<String, Class> entityClassMap = new HashMap<String, Class>();

    static {
        entityClassMap.put("VirtualMachine", UserVm.class);
        entityClassMap.put("Volume", Volume.class);
        entityClassMap.put("Template", VirtualMachineTemplate.class);
        entityClassMap.put("Snapshot", Snapshot.class);
        // To be filled in later depending on the entity permission grant scope
    }

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

    /*
    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_GRANT, eventDescription = "Granting permission to Acl Role")
    public AclP addAclPermissionToAclPolicy(final long aclRoleId, final List<String> apiNames) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclRole role = _aclPolicyDao.findById(aclRoleId);
        if (role == null) {
            throw new InvalidParameterValueException("Unable to find acl role: " + aclRoleId
                    + "; failed to grant permission to role.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, role);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_api_permission table
                for (String api : apiNames) {
                    AclApiPermissionVO perm = _apiPermissionDao.findByRoleAndApi(aclRoleId, api);
                    if (perm == null) {
                        // not there already
                        perm = new AclApiPermissionVO(aclRoleId, api);
                        _apiPermissionDao.persist(perm);
                    }
                }
            }
        });
            
        return role;

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_REVOKE, eventDescription = "Revoking permission from Acl Role")
    public AclRole revokeApiPermissionFromAclRole(final long aclRoleId, final List<String> apiNames) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclRole role = _aclPolicyDao.findById(aclRoleId);
        if (role == null) {
            throw new InvalidParameterValueException("Unable to find acl role: " + aclRoleId
                    + "; failed to revoke permission from role.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, role);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove entries from acl_api_permission table
                for (String api : apiNames) {
                    AclApiPermissionVO perm = _apiPermissionDao.findByRoleAndApi(aclRoleId, api);
                    if (perm != null) {
                        // not removed yet
                        _apiPermissionDao.remove(perm.getId());
                    }
                }
            }
        });
        return role;
    }
    */

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
            List<AclPolicyPermission> perms = _iamSrv.listPollcyPermissionByEntityType(policy.getId(), action, entityType);
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
    public boolean isAPIAccessibleForPolicies(String apiName, List<AclPolicy> policies) {
        return _iamSrv.isAPIAccessibleForPolicies(apiName, policies);
    }

    @Override
    public List<AclPolicy> getEffectivePolicies(Account caller, ControlledEntity entity) {

        // Get the static Policies of the Caller
        List<AclPolicy> policies = _iamSrv.listAclPolicies(caller.getId());

        // add any dynamic policies w.r.t the entity
        if (caller.getId() == entity.getAccountId()) {
            // The caller owns the entity
            policies.add(_iamSrv.getResourceOwnerPolicy());
        }

        return policies;
    }

    @Override
    public AclPolicyResponse createAclPolicyResponse(AclPolicy policy) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AclGroupResponse createAclGroupResponse(AclGroup group) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListResponse<org.apache.cloudstack.acl.api.response.AclGroupResponse> listAclGroups(Long aclGroupId, String aclGroupName, Long domainId, Long startIndex, Long pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListResponse<org.apache.cloudstack.acl.api.response.AclPolicyResponse> listAclPolicies(Long aclPolicyId, String aclPolicyName, Long domainId, Long startIndex,
            Long pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

}
