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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.ProjectRoleService;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.UnavailableCommandException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.dao.VpcOfferingDetailsDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DomainChecker extends AdapterBase implements SecurityChecker {

    @Inject
    DomainDao _domainDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    LaunchPermissionDao _launchPermissionDao;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    ProjectAccountDao _projectAccountDao;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    @Inject
    AccountService _accountService;
    @Inject
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Inject
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    VpcOfferingDetailsDao vpcOfferingDetailsDao;
    @Inject
    private ProjectRoleService projectRoleService;
    @Inject
    private ProjectDao projectDao;
    @Inject
    private AccountService accountService;

    public static final Logger s_logger = Logger.getLogger(DomainChecker.class.getName());
    protected DomainChecker() {
        super();
    }

    @Override
    public boolean checkAccess(Account caller, Domain domain) throws PermissionDeniedException {
        if (caller.getState() != Account.State.enabled) {
            throw new PermissionDeniedException("Account " + caller.getAccountName() + " is disabled.");
        }

        if (domain == null) {
            throw new PermissionDeniedException(String.format("Provided domain is NULL, cannot check access for account [uuid=%s, name=%s]", caller.getUuid(), caller.getAccountName()));
        }

        long domainId = domain.getId();

        if (_accountService.isNormalUser(caller.getId())) {
            if (caller.getDomainId() != domainId) {
                throw new PermissionDeniedException("Account " + caller.getAccountName() + " does not have permission to operate within domain id=" + domain.getUuid());
            }
        } else if (!_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
            throw new PermissionDeniedException("Account " + caller.getAccountName() + " does not have permission to operate within domain id=" + domain.getUuid());
        }

        return true;
    }

    @Override
    public boolean checkAccess(User user, Domain domain) throws PermissionDeniedException {
        if (user.getRemoved() != null) {
            throw new PermissionDeniedException(user + " is no longer active.");
        }

        Account account = _accountDao.findById(user.getAccountId());
        return checkAccess(account, domain);
    }

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException {
        if (entity instanceof VirtualMachineTemplate) {
            VirtualMachineTemplate template = (VirtualMachineTemplate)entity;
            Account owner = _accountDao.findById(template.getAccountId());
            // validate that the template is usable by the account
            if (!template.isPublicTemplate()) {
                if (_accountService.isRootAdmin(caller.getId()) || (owner.getId() == caller.getId())) {
                    return true;
                }
                //special handling for the project case
                if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT && _projectMgr.canAccessProjectAccount(caller, owner.getId())) {
                    return true;
                }

                // since the current account is not the owner of the template, check the launch permissions table to see if the
                // account can launch a VM from this template
                LaunchPermissionVO permission = _launchPermissionDao.findByTemplateAndAccount(template.getId(), caller.getId());
                if (permission == null) {
                    throw new PermissionDeniedException("Account " + caller.getAccountName() +
                            " does not have permission to launch instances from template " + template.getName());
                }
            } else {
                // Domain admin and regular user can delete/modify only templates created by them
                if (accessType != null && accessType == AccessType.OperateEntry) {
                    if (!_accountService.isRootAdmin(caller.getId()) && owner.getId() != caller.getId()) {
                        // For projects check if the caller account can access the project account
                        if (owner.getType() != Account.ACCOUNT_TYPE_PROJECT || !(_projectMgr.canAccessProjectAccount(caller, owner.getId()))) {
                            throw new PermissionDeniedException("Domain Admin and regular users can modify only their own Public templates");
                        }
                    }
                }
            }

            return true;
        } else if (entity instanceof Network && accessType != null && accessType == AccessType.UseEntry) {
            _networkMgr.checkNetworkPermissions(caller, (Network)entity);
        } else if (entity instanceof AffinityGroup) {
            return false;
        } else {
            if (_accountService.isNormalUser(caller.getId())) {
                Account account = _accountDao.findById(entity.getAccountId());
                if (account != null && account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                    //only project owner can delete/modify the project
                    if (accessType != null && accessType == AccessType.ModifyProject) {
                        if (!_projectMgr.canModifyProjectAccount(caller, account.getId())) {
                            throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                        }
                    } else if (!_projectMgr.canAccessProjectAccount(caller, account.getId())) {
                        throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                    }
                    checkOperationPermitted(caller, entity);
                } else {
                    if (caller.getId() != entity.getAccountId()) {
                        throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                    }
                }
            }
        }
        return true;
    }

    private boolean checkOperationPermitted(Account caller, ControlledEntity entity) {
        User user = CallContext.current().getCallingUser();
        Project project = projectDao.findByProjectAccountId(entity.getAccountId());
        if (project == null) {
            throw new CloudRuntimeException("Unable to find project to which the entity belongs to");
        }
        ProjectAccount projectUser = _projectAccountDao.findByProjectIdUserId(project.getId(), user.getAccountId(), user.getId());
        String apiCommandName = CallContext.current().getApiName();

        if (accountService.isRootAdmin(caller.getId()) || accountService.isDomainAdmin(caller.getAccountId())) {
            return true;
        }

        if (projectUser != null) {
            if (projectUser.getAccountRole() == ProjectAccount.Role.Admin) {
                return true;
            } else {
                return isPermitted(project, projectUser, apiCommandName);
            }
        }

        ProjectAccount projectAccount = _projectAccountDao.findByProjectIdAccountId(project.getId(), caller.getAccountId());
        if (projectAccount != null) {
            if (projectAccount.getAccountRole() == ProjectAccount.Role.Admin) {
                return true;
            } else {
                return isPermitted(project, projectAccount, apiCommandName);
            }
        }
        throw new UnavailableCommandException("The given command '" + apiCommandName + "' either does not exist or is not available for the user");
    }

    private boolean isPermitted(Project project, ProjectAccount projectUser, String apiCommandName) {
        ProjectRole projectRole = null;
        if(projectUser.getProjectRoleId() != null) {
            projectRole = projectRoleService.findProjectRole(projectUser.getProjectRoleId(), project.getId());
        }

        if (projectRole == null) {
            return true;
        }

        for (ProjectRolePermission permission : projectRoleService.findAllProjectRolePermissions(project.getId(), projectRole.getId())) {
            if (permission.getRule().matches(apiCommandName)) {
                if (RolePermissionEntity.Permission.ALLOW.equals(permission.getPermission())) {
                    return true;
                } else {
                    throw new PermissionDeniedException("The given command '" + apiCommandName + "' either does not exist or is not available for the user");
                }
            }
        }
        return true;
    }
    @Override
    public boolean checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException {
        Account account = _accountDao.findById(user.getAccountId());
        return checkAccess(account, entity, null);
    }

    @Override
    public boolean checkAccess(Account account, DiskOffering dof, DataCenter zone) throws PermissionDeniedException {
        boolean hasAccess = false;
        // Check for domains
        if (account == null || dof == null) {
            hasAccess = true;
        } else {
            //admin has all permissions
            if (_accountService.isRootAdmin(account.getId())) {
                hasAccess = true;
            }
            //if account is normal user or domain admin
            //check if account's domain is a child of offering's domain (Note: This is made consistent with the list command for disk offering)
            else if (_accountService.isNormalUser(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN
                    || _accountService.isDomainAdmin(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                final List<Long> doDomainIds = diskOfferingDetailsDao.findDomainIds(dof.getId());
                if (doDomainIds.isEmpty()) {
                    hasAccess = true;
                } else {
                    for (Long domainId : doDomainIds) {
                        if (_domainDao.isChildDomain(domainId, account.getDomainId())) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
            }
        }
        // Check for zones
        if (hasAccess && dof != null && zone != null) {
            final List<Long> doZoneIds = diskOfferingDetailsDao.findZoneIds(dof.getId());
            hasAccess = doZoneIds.isEmpty() || doZoneIds.contains(zone.getId());
        }
        return hasAccess;
    }

    @Override
    public boolean checkAccess(Account account, ServiceOffering so, DataCenter zone) throws PermissionDeniedException {
        boolean hasAccess = false;
        // Check for domains
        if (account == null || so == null) {
            hasAccess = true;
        } else {
            //admin has all permissions
            if (_accountService.isRootAdmin(account.getId())) {
                hasAccess = true;
            }
            //if account is normal user or domain admin
            //check if account's domain is a child of offering's domain (Note: This is made consistent with the list command for service offering)
            else if (_accountService.isNormalUser(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN
                    || _accountService.isDomainAdmin(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                final List<Long> soDomainIds = serviceOfferingDetailsDao.findDomainIds(so.getId());
                if (soDomainIds.isEmpty()) {
                    hasAccess = true;
                } else {
                    for (Long domainId : soDomainIds) {
                        if (_domainDao.isChildDomain(domainId, account.getDomainId())) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
            }
        }
        // Check for zones
        if (hasAccess && so != null && zone != null) {
            final List<Long> soZoneIds = serviceOfferingDetailsDao.findZoneIds(so.getId());
            hasAccess = soZoneIds.isEmpty() || soZoneIds.contains(zone.getId());
        }
        return hasAccess;
    }

    @Override
    public boolean checkAccess(Account account, NetworkOffering nof, DataCenter zone) throws PermissionDeniedException {
        boolean hasAccess = false;
        // Check for domains
        if (account == null || nof == null) {
            hasAccess = true;
        } else {
            //admin has all permissions
            if (_accountService.isRootAdmin(account.getId())) {
                hasAccess = true;
            }
            //if account is normal user or domain admin
            //check if account's domain is a child of offering's domain (Note: This is made consistent with the list command for disk offering)
            else if (_accountService.isNormalUser(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN
                    || _accountService.isDomainAdmin(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                final List<Long> noDomainIds = networkOfferingDetailsDao.findDomainIds(nof.getId());
                if (noDomainIds.isEmpty()) {
                    hasAccess = true;
                } else {
                    for (Long domainId : noDomainIds) {
                        if (_domainDao.isChildDomain(domainId, account.getDomainId())) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
            }
        }
        // Check for zones
        if (hasAccess && nof != null && zone != null) {
            final List<Long> doZoneIds = networkOfferingDetailsDao.findZoneIds(nof.getId());
            hasAccess = doZoneIds.isEmpty() || doZoneIds.contains(zone.getId());
        }
        return hasAccess;
    }

    @Override
    public boolean checkAccess(Account account, VpcOffering vof, DataCenter zone) throws PermissionDeniedException {
        boolean hasAccess = false;
        // Check for domains
        if (account == null || vof == null) {
            hasAccess = true;
        } else {
            //admin has all permissions
            if (_accountService.isRootAdmin(account.getId())) {
                hasAccess = true;
            }
            //if account is normal user or domain admin
            //check if account's domain is a child of offering's domain (Note: This is made consistent with the list command for disk offering)
            else if (_accountService.isNormalUser(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN
                    || _accountService.isDomainAdmin(account.getId())
                    || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                final List<Long> voDomainIds = vpcOfferingDetailsDao.findDomainIds(vof.getId());
                if (voDomainIds.isEmpty()) {
                    hasAccess = true;
                } else {
                    for (Long domainId : voDomainIds) {
                        if (_domainDao.isChildDomain(domainId, account.getDomainId())) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
            }
        }
        // Check for zones
        if (hasAccess && vof != null && zone != null) {
            final List<Long> doZoneIds = vpcOfferingDetailsDao.findZoneIds(vof.getId());
            hasAccess = doZoneIds.isEmpty() || doZoneIds.contains(zone.getId());
        }
        return hasAccess;
    }

    @Override
    public boolean checkAccess(Account account, DataCenter zone) throws PermissionDeniedException {
        if (account == null || zone.getDomainId() == null) {//public zone
            return true;
        } else {
            //admin has all permissions
            if (_accountService.isRootAdmin(account.getId())) {
                return true;
            }
            //if account is normal user
            //check if account's domain is a child of zone's domain
            else if (_accountService.isNormalUser(account.getId()) || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                // if zone is dedicated to an account check that the accountId
                // matches.
                DedicatedResourceVO dedicatedZone = _dedicatedDao.findByZoneId(zone.getId());
                if (dedicatedZone != null) {
                    if (dedicatedZone.getAccountId() != null) {
                        if (dedicatedZone.getAccountId() == account.getId()) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                if (account.getDomainId() == zone.getDomainId()) {
                    return true; //zone and account at exact node
                } else {
                    Domain domainRecord = _domainDao.findById(account.getDomainId());
                    if (domainRecord != null) {
                        while (true) {
                            if (domainRecord.getId() == zone.getDomainId()) {
                                //found as a child
                                return true;
                            }
                            if (domainRecord.getParent() != null) {
                                domainRecord = _domainDao.findById(domainRecord.getParent());
                            } else {
                                break;
                            }
                        }
                    }
                }
                //not found
                return false;
            }
            //if account is domain admin
            //check if the account's domain is either child of zone's domain, or if zone's domain is child of account's domain
            else if (_accountService.isDomainAdmin(account.getId())) {
                if (account.getDomainId() == zone.getDomainId()) {
                    return true; //zone and account at exact node
                } else {
                    Domain zoneDomainRecord = _domainDao.findById(zone.getDomainId());
                    Domain accountDomainRecord = _domainDao.findById(account.getDomainId());
                    if (accountDomainRecord != null) {
                        Domain localRecord = accountDomainRecord;
                        while (true) {
                            if (localRecord.getId() == zone.getDomainId()) {
                                //found as a child
                                return true;
                            }
                            if (localRecord.getParent() != null) {
                                localRecord = _domainDao.findById(localRecord.getParent());
                            } else {
                                break;
                            }
                        }
                    }
                    //didn't find in upper tree
                    if (zoneDomainRecord != null &&
                            accountDomainRecord != null &&
                            zoneDomainRecord.getPath().contains(accountDomainRecord.getPath())) {
                        return true;
                    }
                }
                //not found
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType, String action)
            throws PermissionDeniedException {

        if (action != null && ("SystemCapability".equals(action))) {
            if (caller != null && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                return true;
            } else {
                return false;
            }
        } else if (action != null && ("DomainCapability".equals(action))) {
            if (caller != null && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                return true;
            } else {
                return false;
            }
        } else if (action != null && ("DomainResourceCapability".equals(action))) {
            if (caller != null && caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                return true;
            } else {
                return false;
            }
        }
        return checkAccess(caller, entity, accessType);
    }

    @Override
    public boolean checkAccess(Account caller, AccessType accessType, String action, ControlledEntity... entities)
            throws PermissionDeniedException {

        // returns true only if access to all entities is granted
        for (ControlledEntity entity : entities) {
            if (!checkAccess(caller, entity, accessType, action)) {
                return false;
            }
        }
        return true;
    }
}
