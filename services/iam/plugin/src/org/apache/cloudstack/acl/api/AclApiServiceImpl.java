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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.command.acl.AddAccountToAclGroupCmd;
import org.apache.cloudstack.api.command.acl.AddAclPermissionToAclPolicyCmd;
import org.apache.cloudstack.api.command.acl.AttachAclPolicyToAccountCmd;
import org.apache.cloudstack.api.command.acl.AttachAclPolicyToAclGroupCmd;
import org.apache.cloudstack.api.command.acl.CreateAclGroupCmd;
import org.apache.cloudstack.api.command.acl.CreateAclPolicyCmd;
import org.apache.cloudstack.api.command.acl.DeleteAclGroupCmd;
import org.apache.cloudstack.api.command.acl.DeleteAclPolicyCmd;
import org.apache.cloudstack.api.command.acl.ListAclGroupsCmd;
import org.apache.cloudstack.api.command.acl.ListAclPoliciesCmd;
import org.apache.cloudstack.api.command.acl.RemoveAccountFromAclGroupCmd;
import org.apache.cloudstack.api.command.acl.RemoveAclPermissionFromAclPolicyCmd;
import org.apache.cloudstack.api.command.acl.RemoveAclPolicyFromAccountCmd;
import org.apache.cloudstack.api.command.acl.RemoveAclPolicyFromAclGroupCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.acl.AclGroupResponse;
import org.apache.cloudstack.api.response.acl.AclPermissionResponse;
import org.apache.cloudstack.api.response.acl.AclPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
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
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;

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

    @Inject
    MessageBus _messageBus;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _messageBus.subscribe(AccountManager.MESSAGE_ADD_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                HashMap<Long, Long> acctGroupMap = (HashMap<Long, Long>) obj;
                for (Long accountId : acctGroupMap.keySet()) {
                    Long groupId = acctGroupMap.get(accountId);
                    s_logger.debug("MessageBus message: new Account Added: " + accountId + ", adding it to groupId :"
                            + groupId);
                    addAccountToAclGroup(accountId, groupId);
                    // add it to domain group too
                    AccountVO account = _accountDao.findById(accountId);
                    Domain domain = _domainDao.findById(account.getDomainId());
                    if (domain != null) {
                        List<AclGroup> domainGroups = listDomainGroup(domain);

                        if (domainGroups != null) {
                            for (AclGroup group : domainGroups) {
                                addAccountToAclGroup(accountId, new Long(group.getId()));
                            }
                        }
                    }
                }
            }
        });

        _messageBus.subscribe(AccountManager.MESSAGE_REMOVE_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long accountId = ((Long) obj);
                if (accountId != null) {
                    s_logger.debug("MessageBus message: Account removed: " + accountId
                            + ", releasing the group associations");
                    removeAccountFromAclGroups(accountId);
                }
            }
        });

        _messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long domainId = ((Long) obj);
                if (domainId != null) {
                    s_logger.debug("MessageBus message: new Domain created: " + domainId + ", creating a new group");
                    Domain domain = _domainDao.findById(domainId);
                    _iamSrv.createAclGroup("DomainGrp-" + domain.getUuid(), "Domain group", domain.getPath());
                }
            }
        });

        _messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long domainId = ((Long) obj);
                if (domainId != null) {
                    s_logger.debug("MessageBus message: Domain removed: " + domainId + ", removing the domain group");
                    Domain domain = _domainDao.findById(domainId);
                    List<AclGroup> groups = listDomainGroup(domain);
                    for (AclGroup group : groups) {
                        _iamSrv.deleteAclGroup(group.getId());
                    }
                }
            }
        });

        _messageBus.subscribe(TemplateManager.MESSAGE_REGISTER_PUBLIC_TEMPLATE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long templateId = (Long)obj;
                if (templateId != null) {
                    s_logger.debug("MessageBus message: new public template registered: " + templateId + ", grant permission to domain admin and normal user policies");
                    _iamSrv.addAclPermissionToAclPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), AclEntityType.VirtualMachineTemplate.toString(),
                            PermissionScope.RESOURCE.toString(), templateId, "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
                    _iamSrv.addAclPermissionToAclPolicy(new Long(Account.ACCOUNT_TYPE_NORMAL + 1), AclEntityType.VirtualMachineTemplate.toString(),
                            PermissionScope.RESOURCE.toString(), templateId, "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
                }
            }
        });

        _messageBus.subscribe(TemplateManager.MESSAGE_RESET_TEMPLATE_PERMISSION_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long templateId = (Long)obj;
                if (templateId != null) {
                    s_logger.debug("MessageBus message: reset template permission: " + templateId);
                    resetTemplatePermission(templateId);
                }
            }
        });

        _messageBus.subscribe(EntityManager.MESSAGE_REMOVE_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Pair<AclEntityType, Long> entity = (Pair<AclEntityType, Long>)obj;
                if (entity != null) {
                    String entityType = entity.first().toString();
                    Long entityId = entity.second();
                    s_logger.debug("MessageBus message: delete an entity: (" + entityType + "," + entityId + "), remove its related permission");
                    _iamSrv.removeAclPermissionForEntity(entityType, entityId);
                }
            }
        });


        _messageBus.subscribe(EntityManager.MESSAGE_GRANT_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Map<String, Object> permit = (Map<String, Object>)obj;
                if (permit != null) {
                    String entityType = (String)permit.get(ApiConstants.ENTITY_TYPE);
                    Long entityId = (Long)permit.get(ApiConstants.ENTITY_ID);
                    AccessType accessType = (AccessType)permit.get(ApiConstants.ACCESS_TYPE);
                    String action = (String)permit.get(ApiConstants.ACL_ACTION);
                    List<Long> acctIds = (List<Long>)permit.get(ApiConstants.ACCOUNTS);
                    s_logger.debug("MessageBus message: grant accounts permission to an entity: (" + entityType + "," + entityId + ")");
                    grantEntityPermissioinToAccounts(entityType, entityId, accessType, action, acctIds);
                }
            }
        });

        _messageBus.subscribe(EntityManager.MESSAGE_REVOKE_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Map<String, Object> permit = (Map<String, Object>)obj;
                if (permit != null) {
                    String entityType = (String)permit.get(ApiConstants.ENTITY_TYPE);
                    Long entityId = (Long)permit.get(ApiConstants.ENTITY_ID);
                    AccessType accessType = (AccessType)permit.get(ApiConstants.ACCESS_TYPE);
                    String action = (String)permit.get(ApiConstants.ACL_ACTION);
                    List<Long> acctIds = (List<Long>)permit.get(ApiConstants.ACCOUNTS);
                    s_logger.debug("MessageBus message: revoke from accounts permission to an entity: (" + entityType + "," + entityId + ")");
                    revokeEntityPermissioinFromAccounts(entityType, entityId, accessType, action, acctIds);
                }
            }
        });

        _messageBus.subscribe(EntityManager.MESSAGE_ADD_DOMAIN_WIDE_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Map<String, Object> params = (Map<String, Object>) obj;
                if (params != null) {
                    addDomainWideResourceAccess(params);
                }
            }
        });

        return super.configure(name, params);
    }

    private void addDomainWideResourceAccess(Map<String, Object> params) {

        AclEntityType entityType = (AclEntityType)params.get(ApiConstants.ENTITY_TYPE);
        Long entityId = (Long) params.get(ApiConstants.ENTITY_ID);
        Long domainId = (Long) params.get(ApiConstants.DOMAIN_ID);
        Boolean isRecursive = (Boolean) params.get(ApiConstants.SUBDOMAIN_ACCESS);

        if (entityType == AclEntityType.Network) {
            createPolicyAndAddToDomainGroup("DomainWideNetwork-" + entityId, "domain wide network", entityType.toString(),
                    entityId, "listNetworks", AccessType.UseEntry, domainId, isRecursive);
        } else if (entityType == AclEntityType.AffinityGroup) {
            createPolicyAndAddToDomainGroup("DomainWideNetwork-" + entityId, "domain wide affinityGroup", entityType.toString(),
                    entityId, "listAffinityGroups", AccessType.UseEntry, domainId, isRecursive);
        }

    }

    private void createPolicyAndAddToDomainGroup(String policyName, String description, String entityType,
            Long entityId, String action, AccessType accessType, Long domainId, Boolean recursive) {

       Domain domain = _domainDao.findById(domainId);
       if (domain != null) {
            AclPolicy policy = _iamSrv.createAclPolicy(policyName, description, null, domain.getPath());
            _iamSrv.addAclPermissionToAclPolicy(policy.getId(), entityType, PermissionScope.RESOURCE.toString(),
                    entityId, action, accessType.toString(), Permission.Allow, recursive);
            List<Long> policyList = new ArrayList<Long>();
            policyList.add(new Long(policy.getId()));

           List<AclGroup> domainGroups = listDomainGroup(domain);
           if (domainGroups != null) {
               for (AclGroup group : domainGroups) {
                   _iamSrv.attachAclPoliciesToGroup(policyList, group.getId());
               }
           }
       }
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


    private void removeAccountFromAclGroups(long accountId) {
        List<AclGroup> groups = listAclGroups(accountId);
        List<Long> accts = new ArrayList<Long>();
        accts.add(accountId);
        if (groups != null) {
            for (AclGroup grp : groups) {
                removeAccountsFromGroup(accts, grp.getId());
            }
        }
    }

    private void addAccountToAclGroup(long accountId, long groupId) {
        List<Long> accts = new ArrayList<Long>();
        accts.add(accountId);
        addAccountsToGroup(accts, groupId);
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
        Long domainId = caller.getDomainId();
        Domain callerDomain = _domainDao.findById(domainId);
        if (callerDomain == null) {
            throw new InvalidParameterValueException("Caller does not have a domain");
        }
        return _iamSrv.createAclPolicy(aclPolicyName, description, parentPolicyId, callerDomain.getPath());
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
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ACCOUNT_POLICY_UPDATE, eventDescription = "Attaching policy to accounts")
    public void attachAclPolicyToAccounts(final Long policyId, final List<Long> accountIds) {
        _iamSrv.attachAclPolicyToAccounts(policyId, accountIds);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ACCOUNT_POLICY_UPDATE, eventDescription = "Removing policy from accounts")
    public void removeAclPolicyFromAccounts(final Long policyId, final List<Long> accountIds) {
        _iamSrv.removeAclPolicyFromAccounts(policyId, accountIds);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_POLICY_GRANT, eventDescription = "Granting acl permission to Acl Policy")
    public AclPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, PermissionScope scope,
            Long scopeId, String action, Permission perm, Boolean recursive) {
        Class<?> cmdClass = _apiServer.getCmdClass(action);
        AccessType accessType = null;
        if (BaseListCmd.class.isAssignableFrom(cmdClass)) {
            accessType = AccessType.UseEntry;
        }
        return _iamSrv.addAclPermissionToAclPolicy(aclPolicyId, entityType, scope.toString(), scopeId, action,
                accessType.toString(), perm, recursive);
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
            List<AclPolicyPermission> perms = _iamSrv.listPolicyPermissionByActionAndEntity(policy.getId(), action,
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
                if (permission.getEntityType() != null) {
                    perm.setEntityType(AclEntityType.valueOf(permission.getEntityType()));
                }
                if (permission.getScope() != null) {
                    perm.setScope(PermissionScope.valueOf(permission.getScope()));
                }
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

    public List<AclGroup> listDomainGroup(Domain domain) {

        if (domain != null) {
            String domainPath = domain.getPath();
            // search for groups
            Pair<List<AclGroup>, Integer> result = _iamSrv.listAclGroups(null, "DomainGrp-" + domain.getUuid(),
                    domainPath, null, null);
            return result.first();
        }
        return new ArrayList<AclGroup>();

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
    public void grantEntityPermissioinToAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds) {
        // check if there is already a policy with only this permission added to it
        AclPolicy policy = _iamSrv.getResourceGrantPolicy(entityType, entityId, accessType.toString(), action);
        if (policy == null) {
            // not found, just create a policy with resource grant permission
            Account caller = CallContext.current().getCallingAccount();
            String aclPolicyName = "policyGrant" + entityType + entityId;
            String description = "Policy to grant permission to " + entityType + entityId;
            policy = createAclPolicy(caller, aclPolicyName, description, null);
            // add permission to this policy
            addAclPermissionToAclPolicy(policy.getId(), entityType, PermissionScope.RESOURCE, entityId, action, Permission.Allow, false);
        }
        // attach this policy to list of accounts if not attached already
        Long policyId = policy.getId();
        for (Long acctId : accountIds) {
            if (!isPolicyAttachedToAccount(policyId, acctId)) {
                attachAclPolicyToAccounts(policyId, Collections.singletonList(acctId));
            }
        }
    }

    @Override
    public void revokeEntityPermissioinFromAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds) {
        // there should already a policy with only this permission added to it, this call is mainly used
        AclPolicy policy = _iamSrv.getResourceGrantPolicy(entityType, entityId, accessType.toString(), action);
        if (policy == null) {
            s_logger.warn("Cannot find a policy associated with this entity permissioin to be revoked, just return");
            return;
        }
        // detach this policy from list of accounts if not detached already
        Long policyId = policy.getId();
        for (Long acctId : accountIds) {
            if (isPolicyAttachedToAccount(policyId, acctId)) {
                removeAclPolicyFromAccounts(policyId, Collections.singletonList(acctId));
            }
        }

    }

    private boolean isPolicyAttachedToAccount(Long policyId, Long accountId) {
        List<AclPolicy> pList = listAclPolicies(accountId);
        for (AclPolicy p : pList) {
            if (p.getId() == policyId.longValue()) {
                return true;
            }
        }
        return false;
    }

    private void resetTemplatePermission(Long templateId){
        // reset template will change template to private, so we need to remove its permission for domain admin and normal user group
        _iamSrv.removeAclPermissionFromAclPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), AclEntityType.VirtualMachineTemplate.toString(),
                PermissionScope.RESOURCE.toString(), templateId, "listTemplates");
        _iamSrv.removeAclPermissionFromAclPolicy(new Long(Account.ACCOUNT_TYPE_NORMAL + 1), AclEntityType.VirtualMachineTemplate.toString(),
                PermissionScope.RESOURCE.toString(), templateId, "listTemplates");
        // check if there is a policy with only UseEntry permission for this template added
        AclPolicy policy = _iamSrv.getResourceGrantPolicy(AclEntityType.VirtualMachineTemplate.toString(), templateId, AccessType.UseEntry.toString(), "listTemplates");
        if ( policy == null ){
            s_logger.info("No policy found for this template grant: " + templateId + ", no detach to be done");
            return;
        }
        // delete the policy, which should detach it from groups and accounts
        _iamSrv.deleteAclPolicy(policy.getId());

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
        cmdList.add(AttachAclPolicyToAccountCmd.class);
        cmdList.add(RemoveAclPolicyFromAccountCmd.class);
        return cmdList;
    }
}
