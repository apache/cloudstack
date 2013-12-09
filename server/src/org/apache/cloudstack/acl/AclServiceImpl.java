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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclPolicyPermission.Permission;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.dao.AclApiPermissionDao;
import org.apache.cloudstack.acl.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.acl.dao.AclGroupDao;
import org.apache.cloudstack.acl.dao.AclGroupPolicyMapDao;
import org.apache.cloudstack.acl.dao.AclPolicyDao;
import org.apache.cloudstack.acl.dao.AclPolicyPermissionDao;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.ApiServerService;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;

@Local(value = {AclService.class})
public class AclServiceImpl extends ManagerBase implements AclService, Manager {

    public static final Logger s_logger = Logger.getLogger(AclServiceImpl.class);
    private String _name;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AccountDao _accountDao;

    @Inject
    AclPolicyDao _aclPolicyDao;

    @Inject
    AclGroupDao _aclGroupDao;

    @Inject
    EntityManager _entityMgr;

    @Inject
    AclGroupPolicyMapDao _aclGroupPolicyMapDao;

    @Inject
    AclGroupAccountMapDao _aclGroupAccountMapDao;

    @Inject
    AclApiPermissionDao _apiPermissionDao;

    @Inject
    AclPolicyPermissionDao _policyPermissionDao;

    @Inject
    ApiServerService _apiServer;


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

        if (!_accountMgr.isRootAdmin(caller.getAccountId())) {
            // domain admin can only create role for his domain
            if (caller.getDomainId() != domainId.longValue()) {
                throw new PermissionDeniedException("Can't create acl group in domain " + domainId + ", permission denied");
            }
        }
        // check if the role is already existing
        AclGroup grp = _aclGroupDao.findByName(domainId, aclGroupName);
        if (grp != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl group with name " + aclGroupName
                            + " already exisits for domain " + domainId);
        }
        AclGroupVO rvo = new AclGroupVO(aclGroupName, description);
        rvo.setAccountId(caller.getAccountId());
        rvo.setDomainId(domainId);

        return _aclGroupDao.persist(rvo);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_DELETE, eventDescription = "Deleting Acl Group")
    public boolean deleteAclGroup(final Long aclGroupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        final AclGroup grp = _aclGroupDao.findById(aclGroupId);
        if (grp == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + aclGroupId
                    + "; failed to delete acl group.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, grp);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove this group related entry in acl_group_role_map
                List<AclGroupPolicyMapVO> groupPolicyMap = _aclGroupPolicyMapDao.listByGroupId(grp.getId());
                if (groupPolicyMap != null) {
                    for (AclGroupPolicyMapVO gr : groupPolicyMap) {
                        _aclGroupPolicyMapDao.remove(gr.getId());
                    }
                }

                // remove this group related entry in acl_group_account table
                List<AclGroupAccountMapVO> groupAcctMap = _aclGroupAccountMapDao.listByGroupId(grp.getId());
                if (groupAcctMap != null) {
                    for (AclGroupAccountMapVO grpAcct : groupAcctMap) {
                        _aclGroupAccountMapDao.remove(grpAcct.getId());
                    }
                }

                // remove this group from acl_group table
                _aclGroupDao.remove(aclGroupId);
            }
        });

        return true;
    }

    @Override
    public List<AclGroup> listAclGroups(long accountId) {

        GenericSearchBuilder<AclGroupAccountMapVO, Long> groupSB = _aclGroupAccountMapDao.createSearchBuilder(Long.class);
        groupSB.selectFields(groupSB.entity().getAclGroupId());
        groupSB.and("account", groupSB.entity().getAccountId(), Op.EQ);
        SearchCriteria<Long> groupSc = groupSB.create();

        List<Long> groupIds = _aclGroupAccountMapDao.customSearch(groupSc, null);

        SearchBuilder<AclGroupVO> sb = _aclGroupDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        SearchCriteria<AclGroupVO> sc = sb.create();
        sc.setParameters("ids", groupIds.toArray(new Object[groupIds.size()]));
        List<AclGroupVO> groups = _aclGroupDao.search(sc, null);

        return new ArrayList<AclGroup>(groups);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Adding accounts to acl group")
    public AclGroup addAccountsToGroup(final List<Long> acctIds, final Long groupId) {
        final Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add accounts to acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_account_map table
                for (Long acctId : acctIds) {
                    // check account permissions
                    Account account = _accountDao.findById(acctId);
                    if (account == null) {
                        throw new InvalidParameterValueException("Unable to find account: " + acctId
                                + "; failed to add account to acl group.");
                    }
                    _accountMgr.checkAccess(caller, null, true, account);

                    AclGroupAccountMapVO grMap = _aclGroupAccountMapDao.findByGroupAndAccount(groupId, acctId);
                    if (grMap == null) {
                        // not there already
                        grMap = new AclGroupAccountMapVO(groupId, acctId);
                        _aclGroupAccountMapDao.persist(grMap);
                    }
                }
            }
        });
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing accounts from acl group")
    public AclGroup removeAccountsFromGroup(final List<Long> acctIds, final Long groupId) {
        final Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove accounts from acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove entries from acl_group_account_map table
                for (Long acctId : acctIds) {
                    // check account permissions
                    Account account = _accountDao.findById(acctId);
                    if (account == null) {
                        throw new InvalidParameterValueException("Unable to find account: " + acctId
                                + "; failed to add account to acl group.");
                    }
                    _accountMgr.checkAccess(caller, null, true, account);

                    AclGroupAccountMapVO grMap = _aclGroupAccountMapDao.findByGroupAndAccount(groupId, acctId);
                    if (grMap != null) {
                        // not removed yet
                        _aclGroupAccountMapDao.remove(grMap.getId());
                    }
                }
            }
        });
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_CREATE, eventDescription = "Creating Acl Policy", create = true)
    public AclPolicy createAclPolicy(Account caller, final String aclPolicyName, final String description, final Long parentPolicyId) {
        Long domainId = caller.getDomainId();

        if (!_accountMgr.isRootAdmin(caller.getAccountId())) {
            // domain admin can only create role for his domain
            if (caller.getDomainId() != domainId.longValue()) {
                throw new PermissionDeniedException("Can't create acl role in domain " + domainId + ", permission denied");
            }
        }
        // check if the role is already existing
        AclPolicy ro = _aclPolicyDao.findByName(domainId, aclPolicyName);
        if (ro != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl policy with name " + aclPolicyName
                            + " already exisits for domain " + domainId);
        }

        final long account_id = caller.getAccountId();
        final long domain_id = domainId;
        AclPolicy role = Transaction.execute(new TransactionCallback<AclPolicy>() {
            @Override
            public AclPolicy doInTransaction(TransactionStatus status) {
                AclPolicyVO rvo = new AclPolicyVO(aclPolicyName, description);
                rvo.setAccountId(account_id);
                rvo.setDomainId(domain_id);
                AclPolicy role = _aclPolicyDao.persist(rvo);
                if (parentPolicyId != null) {
                    // copy parent role permissions
                    List<AclPolicyPermissionVO> perms = _policyPermissionDao.listByPolicy(parentPolicyId);
                    if (perms != null) {
                        for (AclPolicyPermissionVO perm : perms) {
                            perm.setAclPolicyId(role.getId());
                            _policyPermissionDao.persist(perm);
                        }
                    }
                }
                return role;
            }
        });
                

        return role;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_DELETE, eventDescription = "Deleting Acl Policy")
    public boolean deleteAclPolicy(final long aclPolicyId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Policy entity
        final AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to delete acl policy.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, policy);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove this role related entry in acl_group_role_map
                List<AclGroupPolicyMapVO> groupPolicyMap = _aclGroupPolicyMapDao.listByPolicyId(policy.getId());
                if (groupPolicyMap != null) {
                    for (AclGroupPolicyMapVO gr : groupPolicyMap) {
                        _aclGroupPolicyMapDao.remove(gr.getId());
                    }
                }

                // remove this policy related entry in acl_policy_permission table
                List<AclPolicyPermissionVO> policyPermMap = _policyPermissionDao.listByPolicy(policy.getId());
                if (policyPermMap != null) {
                    for (AclPolicyPermissionVO policyPerm : policyPermMap) {
                        _policyPermissionDao.remove(policyPerm.getId());
                    }
                }

                // remove this role from acl_role table
                _aclPolicyDao.remove(aclPolicyId);
            }
        });

        return true;
    }


    @Override
    public List<AclPolicy> listAclPolicies(long accountId) {

        // static roles of the account
        SearchBuilder<AclGroupAccountMapVO> groupSB = _aclGroupAccountMapDao.createSearchBuilder();
        groupSB.and("account", groupSB.entity().getAccountId(), Op.EQ);

        GenericSearchBuilder<AclGroupPolicyMapVO, Long> policySB = _aclGroupPolicyMapDao.createSearchBuilder(Long.class);
        policySB.selectFields(policySB.entity().getAclPolicyId());
        policySB.join("accountgroupjoin", groupSB, groupSB.entity().getAclGroupId(), policySB.entity().getAclGroupId(),
                JoinType.INNER);
        policySB.done();
        SearchCriteria<Long> policySc = policySB.create();
        policySc.setJoinParameters("accountgroupjoin", "account", accountId);

        List<Long> roleIds = _aclGroupPolicyMapDao.customSearch(policySc, null);

        SearchBuilder<AclPolicyVO> sb = _aclPolicyDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        SearchCriteria<AclPolicyVO> sc = sb.create();
        sc.setParameters("ids", roleIds.toArray(new Object[roleIds.size()]));
        List<AclPolicyVO> policies = _aclPolicyDao.customSearch(sc, null);

        return new ArrayList<AclPolicy>(policies);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Attaching policy to acl group")
    public AclGroup attachAclPoliciesToGroup(final List<Long> policyIds, final Long groupId) {
        final Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add roles to acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_policy_map table
                for (Long policyId : policyIds) {
                    // check policy permissions
                    AclPolicy policy = _aclPolicyDao.findById(policyId);
                    if (policy == null) {
                        throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                                + "; failed to add policies to acl group.");
                    }
                    _accountMgr.checkAccess(caller, null, true, policy);

                    AclGroupPolicyMapVO grMap = _aclGroupPolicyMapDao.findByGroupAndPolicy(groupId, policyId);
                    if (grMap == null) {
                        // not there already
                        grMap = new AclGroupPolicyMapVO(groupId, policyId);
                        _aclGroupPolicyMapDao.persist(grMap);
                    }
                }
            }
        });

        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing policies from acl group")
    public AclGroup removeAclPoliciesFromGroup(final List<Long> policyIds, final Long groupId) {
        final Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove roles from acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_role_map table
                for (Long policyId : policyIds) {
                    // check policy permissions
                    AclPolicy policy = _aclPolicyDao.findById(policyId);
                    if (policy == null) {
                        throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                                + "; failed to add policies to acl group.");
                    }
                    _accountMgr.checkAccess(caller, null, true, policy);

                    AclGroupPolicyMapVO grMap = _aclGroupPolicyMapDao.findByGroupAndPolicy(groupId, policyId);
                    if (grMap != null) {
                        // not removed yet
                        _aclGroupPolicyMapDao.remove(grMap.getId());
                    }
                }
            }
        });
        return group;
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
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Policy entity
        AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to add permission to policy.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, policy);

        // get the entity and check permission
        Class entityClass = entityClassMap.get(entityType);
        if (entityClass == null) {
            throw new InvalidParameterValueException("Entity type " + entityType + " permission granting is not supported yet");
        }
        if (scope == PermissionScope.RESOURCE && scopeId != null) {
            ControlledEntity entity = (ControlledEntity)_entityMgr.findById(entityClass, scopeId);
            if (entity == null) {
                throw new InvalidParameterValueException("Unable to find entity " + entityType + " by id: " + scopeId);
            }
            _accountMgr.checkAccess(caller, null, true, entity);
        }

        // add entry in acl_policy_permission table
        AclPolicyPermissionVO permit = _policyPermissionDao.findByPolicyAndEntity(aclPolicyId, entityType, scope, scopeId, action, perm);
        if (permit == null) {
            // not there already
            Class<?> cmdClass = _apiServer.getCmdClass(action);
            AccessType accessType = null;
            if (BaseListCmd.class.isAssignableFrom(cmdClass)) {
                accessType = AccessType.ListEntry;
            }
            permit = new AclPolicyPermissionVO(aclPolicyId, action, entityType, accessType,
                    scope, scopeId, perm);
            _policyPermissionDao.persist(permit);
        }
        return policy;

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_REVOKE, eventDescription = "Revoking acl permission from Acl Policy")
    public AclPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, PermissionScope scope, Long scopeId, String action) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Policy entity
        AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to revoke permission from policy.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, policy);

        // get the entity and check permission
        Class entityClass = entityClassMap.get(entityType);
        if (entityClass == null) {
            throw new InvalidParameterValueException("Entity type " + entityType + " permission revoke is not supported yet");
        }
        if (scope == PermissionScope.RESOURCE && scopeId != null) {
            ControlledEntity entity = (ControlledEntity)_entityMgr.findById(entityClass, scopeId);
            if (entity == null) {
                throw new InvalidParameterValueException("Unable to find entity " + entityType + " by id: " + scopeId);
            }
            _accountMgr.checkAccess(caller, null, true, entity);
        }

        // remove entry from acl_entity_permission table
        AclPolicyPermissionVO permit = _policyPermissionDao.findByPolicyAndEntity(aclPolicyId, entityType, scope, scopeId, action, null);
        if (permit != null) {
            // not removed yet
            _policyPermissionDao.remove(permit.getId());
        }
        return policy;
    }



    @Override
    public AclPolicyPermission getAclPolicyPermission(long accountId, String entityType, String action) {
        List<AclPolicy> roles = listAclPolicies(accountId);
        AclPolicyPermission curPerm = null;
        for (AclPolicy role : roles) {
            AclPolicyPermissionVO perm = _policyPermissionDao.findByPolicyAndEntity(role.getId(), entityType, null, null, action, Permission.Allow);
            if (perm == null)
                continue;
            if (curPerm == null) {
                curPerm = perm;
            } else if (perm.getScope().greaterThan(curPerm.getScope())) {
                // pick the more relaxed allowed permission
                curPerm = perm;
            }
        }

        return curPerm;
    }



    @Override
    public boolean isAPIAccessibleForPolicies(String apiName, List<AclPolicy> policies) {

        boolean accessible = false;

        List<Long> policyIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            policyIds.add(policy.getId());
        }

        SearchBuilder<AclPolicyPermissionVO> sb = _policyPermissionDao.createSearchBuilder();
        sb.and("action", sb.entity().getAction(), Op.EQ);
        sb.and("policyId", sb.entity().getAclPolicyId(), Op.IN);
        sb.and("entityType", sb.entity().getEntityType(), Op.NULL);

        SearchCriteria<AclPolicyPermissionVO> sc = sb.create();
        sc.setParameters("policyId", policyIds.toArray(new Object[policyIds.size()]));

        List<AclPolicyPermissionVO> permissions = _policyPermissionDao.customSearch(sc, null);

        if (permissions != null && !permissions.isEmpty()) {
            accessible = true;
        }

        return accessible;
    }

    @Override
    public List<AclPolicy> getEffectivePolicies(Account caller, ControlledEntity entity) {

        // Get the static Policies of the Caller
        List<AclPolicy> policies = listAclPolicies(caller.getId());

        // add any dynamic roles w.r.t the entity
        if (caller.getId() == entity.getAccountId()) {
            // The caller owns the entity
            AclPolicy owner = _aclPolicyDao.findByName(Domain.ROOT_DOMAIN, "RESOURCE_OWNER");
            policies.add(owner);
        }

        return policies;
    }

    @Override
    public List<Long> getGrantedDomains(long accountId, String action) {
        // Get the static Policies of the Caller
        List<AclPolicy> policies = listAclPolicies(accountId);
        // for each policy, find granted permission with Domain scope
        List<Long> domainIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            List<AclPolicyPermissionVO> pp = _policyPermissionDao.listGrantedByActionAndScope(policy.getId(), action, PermissionScope.DOMAIN);
            if (pp != null) {
                for (AclPolicyPermissionVO p : pp) {
                    if (p.getScopeId() != null) {
                        domainIds.add(p.getScopeId());
                    }
                }
            }
        }
        return domainIds;
    }

    @Override
    public List<Long> getGrantedAccounts(long accountId, String action) {
        // Get the static Policies of the Caller
        List<AclPolicy> policies = listAclPolicies(accountId);
        // for each policy, find granted permission with Account scope
        List<Long> accountIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            List<AclPolicyPermissionVO> pp = _policyPermissionDao.listGrantedByActionAndScope(policy.getId(), action, PermissionScope.ACCOUNT);
            if (pp != null) {
                for (AclPolicyPermissionVO p : pp) {
                    if (p.getScopeId() != null) {
                        accountIds.add(p.getScopeId());
                    }
                }
            }
        }
        return accountIds;
    }

    @Override
    public List<Long> getGrantedResources(long accountId, String action) {
        // Get the static Policies of the Caller
        List<AclPolicy> policies = listAclPolicies(accountId);
        // for each policy, find granted permission with Resource scope
        List<Long> entityIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            List<AclPolicyPermissionVO> pp = _policyPermissionDao.listGrantedByActionAndScope(policy.getId(), action, PermissionScope.RESOURCE);
            if (pp != null) {
                for (AclPolicyPermissionVO p : pp) {
                    if (p.getScopeId() != null) {
                        entityIds.add(p.getScopeId());
                    }
                }
            }
        }
        return entityIds;
    }

}
