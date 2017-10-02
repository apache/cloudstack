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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.IPAddress;

import com.amazonaws.auth.policy.Condition;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.elasticache.model.Event;

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.command.iam.AddAccountToIAMGroupCmd;
import org.apache.cloudstack.api.command.iam.AddIAMPermissionToIAMPolicyCmd;
import org.apache.cloudstack.api.command.iam.AttachIAMPolicyToAccountCmd;
import org.apache.cloudstack.api.command.iam.AttachIAMPolicyToIAMGroupCmd;
import org.apache.cloudstack.api.command.iam.CreateIAMGroupCmd;
import org.apache.cloudstack.api.command.iam.CreateIAMPolicyCmd;
import org.apache.cloudstack.api.command.iam.DeleteIAMGroupCmd;
import org.apache.cloudstack.api.command.iam.DeleteIAMPolicyCmd;
import org.apache.cloudstack.api.command.iam.ListIAMGroupsCmd;
import org.apache.cloudstack.api.command.iam.ListIAMPoliciesCmd;
import org.apache.cloudstack.api.command.iam.RemoveAccountFromIAMGroupCmd;
import org.apache.cloudstack.api.command.iam.RemoveIAMPermissionFromIAMPolicyCmd;
import org.apache.cloudstack.api.command.iam.RemoveIAMPolicyFromAccountCmd;
import org.apache.cloudstack.api.command.iam.RemoveIAMPolicyFromIAMGroupCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.iam.IAMGroupResponse;
import org.apache.cloudstack.api.response.iam.IAMPermissionResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.iam.api.IAMGroup;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.api.ApiServerService;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.network.MonitoringService;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.UserIpv6Address;
import com.cloud.network.VpnUser;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.lb.SslCert;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.projects.ProjectInvitation;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.server.ResourceTag;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

public class IAMApiServiceImpl extends ManagerBase implements IAMApiService, Manager {

    public static final Logger s_logger = Logger.getLogger(IAMApiServiceImpl.class);
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

    @Inject
    EntityManager _entityMgr;

    private static final Map<String, Class<?>> s_typeMap = new HashMap<String, Class<?>>();
    static {
        s_typeMap.put(VirtualMachine.class.getSimpleName(), VirtualMachine.class);
        s_typeMap.put(Volume.class.getSimpleName(), Volume.class);
        s_typeMap.put(ResourceTag.class.getSimpleName(), ResourceTag.class);
        s_typeMap.put(Account.class.getSimpleName(), Account.class);
        s_typeMap.put(AffinityGroup.class.getSimpleName(), AffinityGroup.class);
        s_typeMap.put(AutoScalePolicy.class.getSimpleName(), AutoScalePolicy.class);
        s_typeMap.put(AutoScaleVmProfile.class.getSimpleName(), AutoScaleVmProfile.class);
        s_typeMap.put(AutoScaleVmGroup.class.getSimpleName(), AutoScaleVmGroup.class);
        s_typeMap.put(Condition.class.getSimpleName(), Condition.class);
        s_typeMap.put(Vpc.class.getSimpleName(), Vpc.class);
        s_typeMap.put(VpcGateway.class.getSimpleName(), VpcGateway.class);
        s_typeMap.put(VpnUser.class.getSimpleName(), VpnUser.class);
        s_typeMap.put(VMSnapshot.class.getSimpleName(), VMSnapshot.class);
        s_typeMap.put(VirtualMachineTemplate.class.getSimpleName(), VirtualMachineTemplate.class);
        s_typeMap.put(UserIpv6Address.class.getSimpleName(), UserIpv6Address.class);
        s_typeMap.put(StaticRoute.class.getSimpleName(), StaticRoute.class);
        s_typeMap.put(SSHKeyPair.class.getSimpleName(), SSHKeyPair.class);
        s_typeMap.put(Snapshot.class.getSimpleName(), Snapshot.class);
        s_typeMap.put(Site2SiteVpnGateway.class.getSimpleName(), Site2SiteVpnGateway.class);
        s_typeMap.put(Site2SiteCustomerGateway.class.getSimpleName(), Site2SiteCustomerGateway.class);
        s_typeMap.put(Site2SiteVpnConnection.class.getSimpleName(), Site2SiteVpnConnection.class);
        s_typeMap.put(SecurityGroup.class.getSimpleName(), SecurityGroup.class);
        s_typeMap.put(RemoteAccessVpn.class.getSimpleName(), RemoteAccessVpn.class);
        s_typeMap.put(ProjectInvitation.class.getSimpleName(), ProjectInvitation.class);
        s_typeMap.put(NicSecondaryIp.class.getSimpleName(), NicSecondaryIp.class);
        s_typeMap.put(NicIpAlias.class.getSimpleName(), NicIpAlias.class);
        s_typeMap.put(Network.class.getSimpleName(), Network.class);
        s_typeMap.put(IpAddress.class.getSimpleName(), IPAddress.class);
        s_typeMap.put(InstanceGroup.class.getSimpleName(), InstanceGroup.class);
        s_typeMap.put(GlobalLoadBalancerRule.class.getSimpleName(), GlobalLoadBalancerRule.class);
        s_typeMap.put(FirewallRule.class.getSimpleName(), FirewallRule.class);
        s_typeMap.put(PortForwardingRule.class.getSimpleName(), PortForwardingRule.class);
        s_typeMap.put(Event.class.getSimpleName(), Event.class);
        s_typeMap.put(AsyncJob.class.getSimpleName(), AsyncJob.class);
        s_typeMap.put(IAMGroup.class.getSimpleName(), IAMGroup.class);
        s_typeMap.put(IAMPolicy.class.getSimpleName(), IAMPolicy.class);
        s_typeMap.put(MonitoringService.class.getSimpleName(), MonitoringService.class);
        s_typeMap.put(SslCert.class.getSimpleName(), SslCert.class);
    }

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
                    addAccountToIAMGroup(accountId, groupId);
                    // add it to domain group too
                    AccountVO account = _accountDao.findById(accountId);
                    Domain domain = _domainDao.findById(account.getDomainId());
                    if (domain != null) {
                        List<IAMGroup> domainGroups = listDomainGroup(domain);

                        if (domainGroups != null) {
                            for (IAMGroup group : domainGroups) {
                                addAccountToIAMGroup(accountId, new Long(group.getId()));
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
                    removeAccountFromIAMGroups(accountId);
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
                    _iamSrv.createIAMGroup("DomainGrp-" + domain.getUuid(), "Domain group", domain.getPath());
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
                    List<IAMGroup> groups = listDomainGroup(domain);
                    for (IAMGroup group : groups) {
                        _iamSrv.deleteIAMGroup(group.getId());
                    }
                }
            }
        });

        _messageBus.subscribe(TemplateManager.MESSAGE_REGISTER_PUBLIC_TEMPLATE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Long templateId = (Long)obj;
                if (templateId != null) {
                    s_logger.debug("MessageBus message: new public template registered: " + templateId
                            + ", grant permission to default root admin, domain admin and normal user policies");
                    _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_ADMIN + 1), VirtualMachineTemplate.class.getSimpleName(),
                            PermissionScope.RESOURCE.toString(), templateId, "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
                    _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), VirtualMachineTemplate.class.getSimpleName(),
                            PermissionScope.RESOURCE.toString(), templateId, "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
                    _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_NORMAL + 1), VirtualMachineTemplate.class.getSimpleName(),
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
                Pair<Class<?>, Long> entity = (Pair<Class<?>, Long>)obj;
                if (entity != null) {
                    String entityType = entity.first().getSimpleName();
                    Long entityId = entity.second();
                    s_logger.debug("MessageBus message: delete an entity: (" + entityType + "," + entityId + "), remove its related permission");
                    _iamSrv.removeIAMPermissionForEntity(entityType, entityId);
                }
            }
        });


        _messageBus.subscribe(EntityManager.MESSAGE_GRANT_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Map<String, Object> permit = (Map<String, Object>)obj;
                if (permit != null) {
                    Class<?> entityType = (Class<?>)permit.get(ApiConstants.ENTITY_TYPE);
                    Long entityId = (Long)permit.get(ApiConstants.ENTITY_ID);
                    AccessType accessType = (AccessType)permit.get(ApiConstants.ACCESS_TYPE);
                    String action = (String)permit.get(ApiConstants.IAM_ACTION);
                    List<Long> acctIds = (List<Long>)permit.get(ApiConstants.ACCOUNTS);
                    s_logger.debug("MessageBus message: grant accounts permission to an entity: (" + entityType + "," + entityId + ")");
                    grantEntityPermissioinToAccounts(entityType.getSimpleName(), entityId, accessType, action, acctIds);
                }
            }
        });

        _messageBus.subscribe(EntityManager.MESSAGE_REVOKE_ENTITY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                Map<String, Object> permit = (Map<String, Object>)obj;
                if (permit != null) {
                    Class<?> entityType = (Class<?>)permit.get(ApiConstants.ENTITY_TYPE);
                    Long entityId = (Long)permit.get(ApiConstants.ENTITY_ID);
                    AccessType accessType = (AccessType)permit.get(ApiConstants.ACCESS_TYPE);
                    String action = (String)permit.get(ApiConstants.IAM_ACTION);
                    List<Long> acctIds = (List<Long>)permit.get(ApiConstants.ACCOUNTS);
                    s_logger.debug("MessageBus message: revoke from accounts permission to an entity: (" + entityType + "," + entityId + ")");
                    revokeEntityPermissioinFromAccounts(entityType.getSimpleName(), entityId, accessType, action, acctIds);
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

    @Override
    public boolean start() {
        s_logger.info("Populating IAM group and account association for default accounts...");

        // populate group <-> account association if not present for CS admin
        // and system accounts
        populateIAMGroupAdminAccountMap();

        return true;
    }

    private void populateIAMGroupAdminAccountMap() {
        List<Long> sysAccts = new ArrayList<Long>();
        sysAccts.add(Account.ACCOUNT_ID_SYSTEM);
        sysAccts.add(Account.ACCOUNT_ID_SYSTEM + 1);
        _iamSrv.addAccountsToGroup(sysAccts, new Long(Account.ACCOUNT_TYPE_ADMIN + 1));
    }

    private void addDomainWideResourceAccess(Map<String, Object> params) {

        Class<?> entityType = (Class<?>)params.get(ApiConstants.ENTITY_TYPE);
        Long entityId = (Long) params.get(ApiConstants.ENTITY_ID);
        Long domainId = (Long) params.get(ApiConstants.DOMAIN_ID);
        Boolean isRecursive = (Boolean) params.get(ApiConstants.SUBDOMAIN_ACCESS);

        if (entityType == Network.class) {
            createPolicyAndAddToDomainGroup("DomainWideNetwork-" + entityId, "domain wide network", entityType.toString(),
                    entityId, "listNetworks", AccessType.UseEntry, domainId, isRecursive);
        } else if (entityType == AffinityGroup.class) {
            createPolicyAndAddToDomainGroup("DomainWideNetwork-" + entityId, "domain wide affinityGroup", entityType.toString(),
                    entityId, "listAffinityGroups", AccessType.UseEntry, domainId, isRecursive);
        }

    }

    private void createPolicyAndAddToDomainGroup(String policyName, String description, String entityType,
            Long entityId, String action, AccessType accessType, Long domainId, Boolean recursive) {

       Domain domain = _domainDao.findById(domainId);
       if (domain != null) {
            IAMPolicy policy = _iamSrv.createIAMPolicy(policyName, description, null, domain.getPath());
            _iamSrv.addIAMPermissionToIAMPolicy(policy.getId(), entityType, PermissionScope.RESOURCE.toString(),
                    entityId, action, accessType.toString(), Permission.Allow, recursive);
            List<Long> policyList = new ArrayList<Long>();
            policyList.add(new Long(policy.getId()));

           List<IAMGroup> domainGroups = listDomainGroup(domain);
           if (domainGroups != null) {
               for (IAMGroup group : domainGroups) {
                   _iamSrv.attachIAMPoliciesToGroup(policyList, group.getId());
               }
           }
       }
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_CREATE, eventDescription = "Creating Acl Group", create = true)
    public IAMGroup createIAMGroup(Account caller, String iamGroupName, String description) {
        Long domainId = caller.getDomainId();
        Domain callerDomain = _domainDao.findById(domainId);
        if (callerDomain == null) {
            throw new InvalidParameterValueException("Caller does not have a domain");
        }
        return _iamSrv.createIAMGroup(iamGroupName, description, callerDomain.getPath());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_DELETE, eventDescription = "Deleting Acl Group")
    public boolean deleteIAMGroup(final Long iamGroupId) {
        return _iamSrv.deleteIAMGroup(iamGroupId);
    }

    @Override
    public List<IAMGroup> listIAMGroups(long accountId) {
        return _iamSrv.listIAMGroups(accountId);
    }


    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_UPDATE, eventDescription = "Adding accounts to acl group")
    public IAMGroup addAccountsToGroup(final List<Long> acctIds, final Long groupId) {
        return _iamSrv.addAccountsToGroup(acctIds, groupId);
    }


    private void removeAccountFromIAMGroups(long accountId) {
        List<IAMGroup> groups = listIAMGroups(accountId);
        List<Long> accts = new ArrayList<Long>();
        accts.add(accountId);
        if (groups != null) {
            for (IAMGroup grp : groups) {
                removeAccountsFromGroup(accts, grp.getId());
            }
        }
    }

    private void addAccountToIAMGroup(long accountId, long groupId) {
        List<Long> accts = new ArrayList<Long>();
        accts.add(accountId);
        addAccountsToGroup(accts, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_UPDATE, eventDescription = "Removing accounts from acl group")
    public IAMGroup removeAccountsFromGroup(final List<Long> acctIds, final Long groupId) {
        return _iamSrv.removeAccountsFromGroup(acctIds, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_POLICY_CREATE, eventDescription = "Creating IAM Policy", create = true)
    public IAMPolicy createIAMPolicy(Account caller, final String iamPolicyName, final String description, final Long parentPolicyId) {
        Long domainId = caller.getDomainId();
        Domain callerDomain = _domainDao.findById(domainId);
        if (callerDomain == null) {
            throw new InvalidParameterValueException("Caller does not have a domain");
        }
        return _iamSrv.createIAMPolicy(iamPolicyName, description, parentPolicyId, callerDomain.getPath());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_POLICY_DELETE, eventDescription = "Deleting IAM Policy")
    public boolean deleteIAMPolicy(final long iamPolicyId) {
        return _iamSrv.deleteIAMPolicy(iamPolicyId);
    }


    @Override
    public List<IAMPolicy> listIAMPolicies(long accountId) {
        return _iamSrv.listIAMPolicies(accountId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_UPDATE, eventDescription = "Attaching policy to acl group")
    public IAMGroup attachIAMPoliciesToGroup(final List<Long> policyIds, final Long groupId) {
        return _iamSrv.attachIAMPoliciesToGroup(policyIds, groupId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_GROUP_UPDATE, eventDescription = "Removing policies from acl group")
    public IAMGroup removeIAMPoliciesFromGroup(final List<Long> policyIds, final Long groupId) {
        return _iamSrv.removeIAMPoliciesFromGroup(policyIds, groupId);
    }


    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_ACCOUNT_POLICY_UPDATE, eventDescription = "Attaching policy to accounts")
    public void attachIAMPolicyToAccounts(final Long policyId, final List<Long> accountIds) {
        _iamSrv.attachIAMPolicyToAccounts(policyId, accountIds);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_ACCOUNT_POLICY_UPDATE, eventDescription = "Removing policy from accounts")
    public void removeIAMPolicyFromAccounts(final Long policyId, final List<Long> accountIds) {
        _iamSrv.removeIAMPolicyFromAccounts(policyId, accountIds);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_POLICY_GRANT, eventDescription = "Granting acl permission to IAM Policy")
    public IAMPolicy addIAMPermissionToIAMPolicy(long iamPolicyId, String entityType, PermissionScope scope,
            Long scopeId, String action, Permission perm, Boolean recursive, Boolean readOnly) {
        Class<?> cmdClass = _apiServer.getCmdClass(action);
        AccessType accessType = null;
        if (BaseListCmd.class.isAssignableFrom(cmdClass)) {
            if (readOnly) {
                accessType = AccessType.ListEntry;
            } else {
                accessType = AccessType.UseEntry;
            }
        } else {
            accessType = AccessType.OperateEntry;
        }
        String accessTypeStr = (accessType != null) ? accessType.toString() : null;
        return _iamSrv.addIAMPermissionToIAMPolicy(iamPolicyId, entityType, scope.toString(), scopeId, action,
                accessTypeStr, perm, recursive);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IAM_POLICY_REVOKE, eventDescription = "Revoking acl permission from IAM Policy")
    public IAMPolicy removeIAMPermissionFromIAMPolicy(long iamPolicyId, String entityType, PermissionScope scope, Long scopeId, String action) {
        return _iamSrv.removeIAMPermissionFromIAMPolicy(iamPolicyId, entityType, scope.toString(), scopeId, action);
    }

    @Override
    public IAMPolicyPermission getIAMPolicyPermission(long accountId, String entityType, String action) {
        List<IAMPolicy> policies = _iamSrv.listIAMPolicies(accountId);
        IAMPolicyPermission curPerm = null;
        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> perms = _iamSrv.listPolicyPermissionByActionAndEntity(policy.getId(), action,
                    entityType);
            if (perms == null || perms.size() == 0)
                continue;
            IAMPolicyPermission perm = perms.get(0); // just pick one
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
    public IAMPolicyResponse createIAMPolicyResponse(IAMPolicy policy) {
        IAMPolicyResponse response = new IAMPolicyResponse();
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
        List<IAMPolicyPermission> permissions = _iamSrv.listPolicyPermissions(policy.getId());
        if (permissions != null && permissions.size() > 0) {
            for (IAMPolicyPermission permission : permissions) {
                IAMPermissionResponse perm = new IAMPermissionResponse();
                perm.setAction(permission.getAction());
                if (permission.getEntityType() != null) {
                    perm.setEntityType(permission.getEntityType());
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
    public IAMGroupResponse createIAMGroupResponse(IAMGroup group) {
        IAMGroupResponse response = new IAMGroupResponse();
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
        List<IAMPolicy> policies = _iamSrv.listIAMPoliciesByGroup(group.getId());
        if (policies != null && policies.size() > 0) {
            for (IAMPolicy policy : policies) {
                response.addPolicy(policy.getName());
            }
        }

        response.setObjectName("aclgroup");
        return response;

    }

    public List<IAMGroup> listDomainGroup(Domain domain) {

        if (domain != null) {
            String domainPath = domain.getPath();
            // search for groups
            Pair<List<IAMGroup>, Integer> result = _iamSrv.listIAMGroups(null, "DomainGrp-" + domain.getUuid(),
                    domainPath, null, null);
            return result.first();
        }
        return new ArrayList<IAMGroup>();

    }

    @Override
    public ListResponse<IAMGroupResponse> listIAMGroups(Long iamGroupId, String iamGroupName, Long domainId, Long startIndex, Long pageSize) {
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
        Pair<List<IAMGroup>, Integer> result = _iamSrv.listIAMGroups(iamGroupId, iamGroupName, domainPath, startIndex, pageSize);
        // generate group response
        ListResponse<IAMGroupResponse> response = new ListResponse<IAMGroupResponse>();
        List<IAMGroupResponse> groupResponses = new ArrayList<IAMGroupResponse>();
        for (IAMGroup group : result.first()) {
            IAMGroupResponse resp = createIAMGroupResponse(group);
            groupResponses.add(resp);
        }
        response.setResponses(groupResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<IAMPolicyResponse> listIAMPolicies(Long iamPolicyId, String iamPolicyName, Long domainId, Long startIndex,
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
        Pair<List<IAMPolicy>, Integer> result = _iamSrv.listIAMPolicies(iamPolicyId, iamPolicyName, domainPath, startIndex, pageSize);
        // generate policy response
        ListResponse<IAMPolicyResponse> response = new ListResponse<IAMPolicyResponse>();
        List<IAMPolicyResponse> policyResponses = new ArrayList<IAMPolicyResponse>();
        for (IAMPolicy policy : result.first()) {
            IAMPolicyResponse resp = createIAMPolicyResponse(policy);
            policyResponses.add(resp);
        }
        response.setResponses(policyResponses, result.second());
        return response;
    }

    @Override
    public void grantEntityPermissioinToAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds) {
        // check if there is already a policy with only this permission added to it
        IAMPolicy policy = _iamSrv.getResourceGrantPolicy(entityType, entityId, accessType.toString(), action);
        if (policy == null) {
            // not found, just create a policy with resource grant permission
            Account caller = CallContext.current().getCallingAccount();
            String aclPolicyName = "policyGrant" + entityType + entityId;
            String description = "Policy to grant permission to " + entityType + entityId;
            policy = createIAMPolicy(caller, aclPolicyName, description, null);
            // add permission to this policy
            addIAMPermissionToIAMPolicy(policy.getId(), entityType, PermissionScope.RESOURCE, entityId, action,
                    Permission.Allow, false, false);
        }
        // attach this policy to list of accounts if not attached already
        Long policyId = policy.getId();
        for (Long acctId : accountIds) {
            if (!isPolicyAttachedToAccount(policyId, acctId)) {
                attachIAMPolicyToAccounts(policyId, Collections.singletonList(acctId));
            }
        }
    }

    @Override
    public void revokeEntityPermissioinFromAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds) {
        // there should already a policy with only this permission added to it, this call is mainly used
        IAMPolicy policy = _iamSrv.getResourceGrantPolicy(entityType, entityId, accessType.toString(), action);
        if (policy == null) {
            s_logger.warn("Cannot find a policy associated with this entity permissioin to be revoked, just return");
            return;
        }
        // detach this policy from list of accounts if not detached already
        Long policyId = policy.getId();
        for (Long acctId : accountIds) {
            if (isPolicyAttachedToAccount(policyId, acctId)) {
                removeIAMPolicyFromAccounts(policyId, Collections.singletonList(acctId));
            }
        }

    }

    private boolean isPolicyAttachedToAccount(Long policyId, Long accountId) {
        List<IAMPolicy> pList = listIAMPolicies(accountId);
        for (IAMPolicy p : pList) {
            if (p.getId() == policyId.longValue()) {
                return true;
            }
        }
        return false;
    }

    private void resetTemplatePermission(Long templateId){
        // reset template will change template to private, so we need to remove its permission for domain admin and normal user group
        _iamSrv.removeIAMPermissionFromIAMPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), VirtualMachineTemplate.class.getSimpleName(),
                PermissionScope.RESOURCE.toString(), templateId, "listTemplates");
        _iamSrv.removeIAMPermissionFromIAMPolicy(new Long(Account.ACCOUNT_TYPE_NORMAL + 1), VirtualMachineTemplate.class.getSimpleName(),
                PermissionScope.RESOURCE.toString(), templateId, "listTemplates");
        // check if there is a policy with only UseEntry permission for this template added
        IAMPolicy policy = _iamSrv.getResourceGrantPolicy(VirtualMachineTemplate.class.getSimpleName(), templateId, AccessType.UseEntry.toString(), "listTemplates");
        if ( policy == null ){
            s_logger.info("No policy found for this template grant: " + templateId + ", no detach to be done");
            return;
        }
        // delete the policy, which should detach it from groups and accounts
        _iamSrv.deleteIAMPolicy(policy.getId());

    }

    @Override
    public Long getPermissionScopeId(String scope, String entityType, String scopeId) {
        if (scopeId.equals("-1")) {
            return -1L;
        }
        PermissionScope permScope = PermissionScope.valueOf(scope);
        InternalIdentity entity = null;
        switch (permScope) {
        case DOMAIN:
            entity = _domainDao.findByUuid(scopeId);
            break;
        case ACCOUNT:
            entity = _accountDao.findByUuid(scopeId);
            break;
        case RESOURCE:
            Class<?> clazz = s_typeMap.get(entityType);
            entity = (InternalIdentity)_entityMgr.findByUuid(clazz, scopeId);
        }

        if (entity != null) {
            return entity.getId();
        }
        throw new InvalidParameterValueException("Unable to find scopeId " + scopeId + " with scope " + scope + " and type " + entityType);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIAMPolicyCmd.class);
        cmdList.add(DeleteIAMPolicyCmd.class);
        cmdList.add(ListIAMPoliciesCmd.class);
        cmdList.add(AddIAMPermissionToIAMPolicyCmd.class);
        cmdList.add(RemoveIAMPermissionFromIAMPolicyCmd.class);
        cmdList.add(AttachIAMPolicyToIAMGroupCmd.class);
        cmdList.add(RemoveIAMPolicyFromIAMGroupCmd.class);
        cmdList.add(CreateIAMGroupCmd.class);
        cmdList.add(DeleteIAMGroupCmd.class);
        cmdList.add(ListIAMGroupsCmd.class);
        cmdList.add(AddAccountToIAMGroupCmd.class);
        cmdList.add(RemoveAccountFromIAMGroupCmd.class);
        cmdList.add(AttachIAMPolicyToAccountCmd.class);
        cmdList.add(RemoveIAMPolicyFromAccountCmd.class);
        return cmdList;
    }
}
