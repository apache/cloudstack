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
package org.apache.cloudstack.iam.server;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.iam.api.AclGroup;
import org.apache.cloudstack.iam.api.AclPolicy;
import org.apache.cloudstack.iam.api.AclPolicyPermission;
import org.apache.cloudstack.iam.api.AclPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;
import org.apache.cloudstack.iam.server.dao.AclAccountPolicyMapDao;
import org.apache.cloudstack.iam.server.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.iam.server.dao.AclGroupDao;
import org.apache.cloudstack.iam.server.dao.AclGroupPolicyMapDao;
import org.apache.cloudstack.iam.server.dao.AclPolicyDao;
import org.apache.cloudstack.iam.server.dao.AclPolicyPermissionDao;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;

@Local(value = {IAMService.class})
public class IAMServiceImpl extends ManagerBase implements IAMService, Manager {

    public static final Logger s_logger = Logger.getLogger(IAMServiceImpl.class);
    private String _name;

    @Inject
    AclPolicyDao _aclPolicyDao;

    @Inject
    AclGroupDao _aclGroupDao;

    @Inject
    EntityManager _entityMgr;

    @Inject
    AclGroupPolicyMapDao _aclGroupPolicyMapDao;

    @Inject
    AclAccountPolicyMapDao _aclAccountPolicyMapDao;

    @Inject
    AclGroupAccountMapDao _aclGroupAccountMapDao;

    @Inject
    AclPolicyPermissionDao _policyPermissionDao;

    @DB
    @Override
    public AclGroup createAclGroup(String aclGroupName, String description, String path) {
        // check if the group is already existing
        AclGroup grp = _aclGroupDao.findByName(path, aclGroupName);
        if (grp != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl group with name " + aclGroupName
                    + " already exisits for path " + path);
        }
        AclGroupVO rvo = new AclGroupVO(aclGroupName, description);
        rvo.setPath(path);

        return _aclGroupDao.persist(rvo);
    }

    @DB
    @Override
    public boolean deleteAclGroup(final Long aclGroupId) {
        // get the Acl Group entity
        final AclGroup grp = _aclGroupDao.findById(aclGroupId);
        if (grp == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + aclGroupId
                    + "; failed to delete acl group.");
        }

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

    @SuppressWarnings("unchecked")
    @Override
    public List<AclGroup> listAclGroups(long accountId) {

        GenericSearchBuilder<AclGroupAccountMapVO, Long> groupSB = _aclGroupAccountMapDao.createSearchBuilder(Long.class);
        groupSB.selectFields(groupSB.entity().getAclGroupId());
        groupSB.and("account", groupSB.entity().getAccountId(), Op.EQ);
        SearchCriteria<Long> groupSc = groupSB.create();
        groupSc.setParameters("account", accountId);

        List<Long> groupIds = _aclGroupAccountMapDao.customSearch(groupSc, null);

        SearchBuilder<AclGroupVO> sb = _aclGroupDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        SearchCriteria<AclGroupVO> sc = sb.create();
        sc.setParameters("ids", groupIds.toArray(new Object[groupIds.size()]));
        @SuppressWarnings("rawtypes")
        List groups = _aclGroupDao.search(sc, null);
        return groups;
    }

    @DB
    @Override
    public AclGroup addAccountsToGroup(final List<Long> acctIds, final Long groupId) {
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add accounts to acl group.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_account_map table
                for (Long acctId : acctIds) {
                    // check account permissions
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
    public AclGroup removeAccountsFromGroup(final List<Long> acctIds, final Long groupId) {
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove accounts from acl group.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove entries from acl_group_account_map table
                for (Long acctId : acctIds) {
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

    @Override
    public List<Long> listAccountsByGroup(long groupId) {
        List<AclGroupAccountMapVO> grpAcctMap = _aclGroupAccountMapDao.listByGroupId(groupId);
        if (grpAcctMap == null || grpAcctMap.size() == 0) {
            return new ArrayList<Long>();
        }

        List<Long> accts = new ArrayList<Long>();
        for (AclGroupAccountMapVO grpAcct : grpAcctMap) {
            accts.add(grpAcct.getAccountId());
        }
        return accts;
    }

    @Override
    public Pair<List<AclGroup>, Integer> listAclGroups(Long aclGroupId, String aclGroupName, String path, Long startIndex, Long pageSize) {
        if (aclGroupId != null) {
            AclGroup group = _aclGroupDao.findById(aclGroupId);
            if (group == null) {
                throw new InvalidParameterValueException("Unable to find acl group by id " + aclGroupId);
            }
        }

        Filter searchFilter = new Filter(AclGroupVO.class, "id", true, startIndex, pageSize);

        SearchBuilder<AclGroupVO> sb = _aclGroupDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<AclGroupVO> sc = sb.create();

        if (aclGroupName != null) {
            sc.setParameters("name", aclGroupName);
        }

        if (aclGroupId != null) {
            sc.setParameters("id", aclGroupId);
        }

        sc.setParameters("path", path + "%");

        Pair<List<AclGroupVO>, Integer> groups = _aclGroupDao.searchAndCount(sc, searchFilter);
        return new Pair<List<AclGroup>, Integer>(new ArrayList<AclGroup>(groups.first()), groups.second());
    }

    @Override
    public List<AclGroup> listParentAclGroups(long groupId) {
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group by id " + groupId);
        }

        String path = group.getPath();
        List<String> pathList = new ArrayList<String>();

        String[] parts = path.split("/");

        for (String part : parts) {
            int start = path.indexOf(part);
            if (start > 0) {
                String subPath = path.substring(0, start);
                pathList.add(subPath);
            }
        }

        if (pathList.isEmpty()) {
            return new ArrayList<AclGroup>();
        }

        SearchBuilder<AclGroupVO> sb = _aclGroupDao.createSearchBuilder();
        sb.and("paths", sb.entity().getPath(), SearchCriteria.Op.IN);

        SearchCriteria<AclGroupVO> sc = sb.create();
        sc.setParameters("paths", pathList.toArray());

        List<AclGroupVO> groups = _aclGroupDao.search(sc, null);

        return new ArrayList<AclGroup>(groups);

    }

    @DB
    @Override
    public AclPolicy createAclPolicy(final String aclPolicyName, final String description, final Long parentPolicyId, final String path) {

        // check if the policy is already existing
        AclPolicy ro = _aclPolicyDao.findByName(aclPolicyName);
        if (ro != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl policy with name " + aclPolicyName
                    + " already exisits");
        }

        AclPolicy role = Transaction.execute(new TransactionCallback<AclPolicy>() {
            @Override
            public AclPolicy doInTransaction(TransactionStatus status) {
                AclPolicyVO rvo = new AclPolicyVO(aclPolicyName, description);
                rvo.setPath(path);

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
    public boolean deleteAclPolicy(final long aclPolicyId) {
        // get the Acl Policy entity
        final AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to delete acl policy.");
        }

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

                // remove this policy related entry in acl_account_policy_map table
                List<AclAccountPolicyMapVO> policyAcctMap = _aclAccountPolicyMapDao.listByPolicyId(policy.getId());
                if (policyAcctMap != null) {
                    for (AclAccountPolicyMapVO policyAcct : policyAcctMap) {
                        _aclAccountPolicyMapDao.remove(policyAcct.getId());
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


    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicy> listAclPolicies(long accountId) {

        // static policies of the account
        SearchBuilder<AclGroupAccountMapVO> groupSB = _aclGroupAccountMapDao.createSearchBuilder();
        groupSB.and("account", groupSB.entity().getAccountId(), Op.EQ);

        GenericSearchBuilder<AclGroupPolicyMapVO, Long> policySB = _aclGroupPolicyMapDao.createSearchBuilder(Long.class);
        policySB.selectFields(policySB.entity().getAclPolicyId());
        policySB.join("accountgroupjoin", groupSB, groupSB.entity().getAclGroupId(), policySB.entity().getAclGroupId(),
                JoinType.INNER);
        policySB.done();
        SearchCriteria<Long> policySc = policySB.create();
        policySc.setJoinParameters("accountgroupjoin", "account", accountId);

        List<Long> policyIds = _aclGroupPolicyMapDao.customSearch(policySc, null);
        // add policies directly attached to the account
        List<AclAccountPolicyMapVO> acctPolicies = _aclAccountPolicyMapDao.listByAccountId(accountId);
        for (AclAccountPolicyMapVO p : acctPolicies) {
            policyIds.add(p.getAclPolicyId());
        }
        if (policyIds.size() == 0) {
            return new ArrayList<AclPolicy>();
        }
        SearchBuilder<AclPolicyVO> sb = _aclPolicyDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        SearchCriteria<AclPolicyVO> sc = sb.create();
        sc.setParameters("ids", policyIds.toArray(new Object[policyIds.size()]));
        @SuppressWarnings("rawtypes")
        List policies = _aclPolicyDao.customSearch(sc, null);

        return policies;

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicy> listAclPoliciesByGroup(long groupId) {
        List<AclGroupPolicyMapVO> policyGrpMap = _aclGroupPolicyMapDao.listByGroupId(groupId);
        if (policyGrpMap == null || policyGrpMap.size() == 0) {
            return new ArrayList<AclPolicy>();
        }

        List<Long> policyIds = new ArrayList<Long>();
        for (AclGroupPolicyMapVO pg : policyGrpMap) {
            policyIds.add(pg.getAclPolicyId());
        }

        SearchBuilder<AclPolicyVO> sb = _aclPolicyDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        SearchCriteria<AclPolicyVO> sc = sb.create();
        sc.setParameters("ids", policyIds.toArray(new Object[policyIds.size()]));
        @SuppressWarnings("rawtypes")
        List policies = _aclPolicyDao.customSearch(sc, null);

        return policies;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicy> listRecursiveAclPoliciesByGroup(long groupId) {
        List<AclGroupPolicyMapVO> policyGrpMap = _aclGroupPolicyMapDao.listByGroupId(groupId);
        if (policyGrpMap == null || policyGrpMap.size() == 0) {
            return new ArrayList<AclPolicy>();
        }

        List<Long> policyIds = new ArrayList<Long>();
        for (AclGroupPolicyMapVO pg : policyGrpMap) {
            policyIds.add(pg.getAclPolicyId());
        }

        SearchBuilder<AclPolicyPermissionVO> permSb = _policyPermissionDao.createSearchBuilder();
        permSb.and("isRecursive", permSb.entity().isRecursive(), Op.EQ);

        SearchBuilder<AclPolicyVO> sb = _aclPolicyDao.createSearchBuilder();
        sb.and("ids", sb.entity().getId(), Op.IN);
        sb.join("recursivePerm", permSb, sb.entity().getId(), permSb.entity().getAclPolicyId(),
                JoinBuilder.JoinType.INNER);

        SearchCriteria<AclPolicyVO> sc = sb.create();
        sc.setParameters("ids", policyIds.toArray(new Object[policyIds.size()]));
        sc.setJoinParameters("recursivePerm", "isRecursive", true);

        @SuppressWarnings("rawtypes")
        List policies = _aclPolicyDao.customSearch(sc, null);

        return policies;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Pair<List<AclPolicy>, Integer> listAclPolicies(Long aclPolicyId, String aclPolicyName, String path, Long startIndex, Long pageSize) {

        if (aclPolicyId != null) {
            AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
            if (policy == null) {
                throw new InvalidParameterValueException("Unable to find acl policy by id " + aclPolicyId);
            }
        }

        Filter searchFilter = new Filter(AclPolicyVO.class, "id", true, startIndex, pageSize);

        SearchBuilder<AclPolicyVO> sb = _aclPolicyDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<AclPolicyVO> sc = sb.create();

        if (aclPolicyName != null) {
            sc.setParameters("name", aclPolicyName);
        }

        if (aclPolicyId != null) {
            sc.setParameters("id", aclPolicyId);
        }

        sc.setParameters("path", path + "%");

        Pair<List<AclPolicyVO>, Integer> policies = _aclPolicyDao.searchAndCount(sc, searchFilter);
        @SuppressWarnings("rawtypes")
        List policyList = policies.first();
        return new Pair<List<AclPolicy>, Integer>(policyList, policies.second());
    }

    @DB
    @Override
    public AclGroup attachAclPoliciesToGroup(final List<Long> policyIds, final Long groupId) {
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add roles to acl group.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_policy_map table
                for (Long policyId : policyIds) {
                    AclPolicy policy = _aclPolicyDao.findById(policyId);
                    if (policy == null) {
                        throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                                + "; failed to add policies to acl group.");
                    }

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
    public AclGroup removeAclPoliciesFromGroup(final List<Long> policyIds, final Long groupId) {
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove roles from acl group.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_role_map table
                for (Long policyId : policyIds) {
                    AclPolicy policy = _aclPolicyDao.findById(policyId);
                    if (policy == null) {
                        throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                                + "; failed to add policies to acl group.");
                    }

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


    @Override
    public void attachAclPolicyToAccounts(final Long policyId, final List<Long> acctIds) {
        AclPolicy policy = _aclPolicyDao.findById(policyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                    + "; failed to add policy to account.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_policy_map table
                for (Long acctId : acctIds) {
                    AclAccountPolicyMapVO acctMap = _aclAccountPolicyMapDao.findByAccountAndPolicy(acctId, policyId);
                    if (acctMap == null) {
                        // not there already
                        acctMap = new AclAccountPolicyMapVO(acctId, policyId);
                        _aclAccountPolicyMapDao.persist(acctMap);
                    }
                }
            }
        });
    }

    @Override
    public void removeAclPolicyFromAccounts(final Long policyId, final List<Long> acctIds) {
        AclPolicy policy = _aclPolicyDao.findById(policyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + policyId
                    + "; failed to add policy to account.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // add entries in acl_group_policy_map table
                for (Long acctId : acctIds) {
                    AclAccountPolicyMapVO acctMap = _aclAccountPolicyMapDao.findByAccountAndPolicy(acctId, policyId);
                    if (acctMap == null) {
                        // not there already
                        acctMap = new AclAccountPolicyMapVO(acctId, policyId);
                        _aclAccountPolicyMapDao.remove(acctMap.getId());
                    }
                }
            }
        });
    }

    @DB
    @Override
    public AclPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action, String accessType, Permission perm, Boolean recursive) {
        // get the Acl Policy entity
        AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to add permission to policy.");
        }

        // add entry in acl_policy_permission table
        AclPolicyPermissionVO permit = _policyPermissionDao.findByPolicyAndEntity(aclPolicyId, entityType, scope, scopeId, action, perm);
        if (permit == null) {
            // not there already
            permit = new AclPolicyPermissionVO(aclPolicyId, action, entityType, accessType, scope, scopeId, perm,
                    recursive);
            _policyPermissionDao.persist(permit);
        }
        return policy;

    }

    @DB
    @Override
    public AclPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action) {
        // get the Acl Policy entity
        AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to revoke permission from policy.");
        }
        // remove entry from acl_entity_permission table
        AclPolicyPermissionVO permit = _policyPermissionDao.findByPolicyAndEntity(aclPolicyId, entityType, scope, scopeId, action, null);
        if (permit != null) {
            // not removed yet
            _policyPermissionDao.remove(permit.getId());
        }
        return policy;
    }

    @DB
    @Override
    public void removeAclPermissionForEntity(final String entityType, final Long entityId) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // remove entry from acl_entity_permission table
                List<AclPolicyPermissionVO> permitList = _policyPermissionDao.listByEntity(entityType, entityId);
                for (AclPolicyPermissionVO permit : permitList) {
                    long policyId = permit.getAclPolicyId();
                    _policyPermissionDao.remove(permit.getId());

                    // remove the policy if there are no other permissions
                    if ((_policyPermissionDao.listByPolicy(policyId)).isEmpty()) {
                        deleteAclPolicy(policyId);
                    }
                }
            }
        });
    }

    @DB
    @Override
    public AclPolicy resetAclPolicy(long aclPolicyId) {
        // get the Acl Policy entity
        AclPolicy policy = _aclPolicyDao.findById(aclPolicyId);
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find acl policy: " + aclPolicyId
                    + "; failed to reset the policy.");
        }

        SearchBuilder<AclPolicyPermissionVO> sb = _policyPermissionDao.createSearchBuilder();
        sb.and("policyId", sb.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        sb.and("scope", sb.entity().getScope(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<AclPolicyPermissionVO> permissionSC = sb.create();
        permissionSC.setParameters("policyId", aclPolicyId);
        _policyPermissionDao.expunge(permissionSC);

        return policy;
    }

    @Override
    public boolean isActionAllowedForPolicies(String action, List<AclPolicy> policies) {

        boolean allowed = false;

        if (policies == null || policies.size() == 0) {
            return allowed;
        }

        List<Long> policyIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            policyIds.add(policy.getId());
        }

        SearchBuilder<AclPolicyPermissionVO> sb = _policyPermissionDao.createSearchBuilder();
        sb.and("action", sb.entity().getAction(), Op.EQ);
        sb.and("policyId", sb.entity().getAclPolicyId(), Op.IN);

        SearchCriteria<AclPolicyPermissionVO> sc = sb.create();
        sc.setParameters("policyId", policyIds.toArray(new Object[policyIds.size()]));
        sc.setParameters("action", action);

        List<AclPolicyPermissionVO> permissions = _policyPermissionDao.customSearch(sc, null);

        if (permissions != null && !permissions.isEmpty()) {
            allowed = true;
        }

        return allowed;
    }


    @Override
    public List<Long> getGrantedEntities(long accountId, String action, String scope) {
        // Get the static Policies of the Caller
        List<AclPolicy> policies = listAclPolicies(accountId);
        // for each policy, find granted permission within the given scope
        List<Long> entityIds = new ArrayList<Long>();
        for (AclPolicy policy : policies) {
            List<AclPolicyPermissionVO> pp = _policyPermissionDao.listGrantedByActionAndScope(policy.getId(), action,
                    scope);
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

    @Override
    @SuppressWarnings("unchecked")
    public List<AclPolicyPermission> listPolicyPermissions(long policyId) {
        @SuppressWarnings("rawtypes")
        List pp = _policyPermissionDao.listByPolicy(policyId);
        return pp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicyPermission> listPolicyPermissionsByScope(long policyId, String action, String scope) {
        @SuppressWarnings("rawtypes")
        List pp = _policyPermissionDao.listGrantedByActionAndScope(policyId, action, scope);
        return pp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicyPermission> listPolicyPermissionByActionAndEntity(long policyId, String action,
            String entityType) {
        @SuppressWarnings("rawtypes")
        List pp = _policyPermissionDao.listByPolicyActionAndEntity(policyId, action, entityType);
        return pp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AclPolicyPermission> listPolicyPermissionByAccessAndEntity(long policyId, String accessType,
            String entityType) {
        @SuppressWarnings("rawtypes")
        List pp = _policyPermissionDao.listByPolicyAccessAndEntity(policyId, accessType, entityType);
        return pp;
    }

    @Override
    public AclPolicy getResourceOwnerPolicy() {
        return _aclPolicyDao.findByName("RESOURCE_OWNER");
    }

    // search for policy with only one resource grant permission
    @Override
    public AclPolicy getResourceGrantPolicy(String entityType, Long entityId, String accessType, String action) {
        List<AclPolicyVO> policyList = _aclPolicyDao.listAll();
        for (AclPolicyVO policy : policyList){
            List<AclPolicyPermission> pp = listPolicyPermissions(policy.getId());
            if ( pp != null && pp.size() == 1){
                // resource grant policy should only have one ACL permission assigned
                AclPolicyPermission permit = pp.get(0);
                if ( permit.getEntityType().equals(entityType) && permit.getScope().equals(PermissionScope.RESOURCE.toString()) && permit.getScopeId().longValue() == entityId.longValue()){
                    if (accessType != null && permit.getAccessType().equals(accessType)){
                        return policy;
                    } else if (action != null && permit.getAction().equals(action)) {
                        return policy;
                    }
                }
            }
        }
        return null;
    }

}
