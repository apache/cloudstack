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
package com.cloud.projects;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.dao.ProjectRoleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project.State;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.projects.dao.ProjectInvitationDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

@Component
public class ProjectManagerImpl extends ManagerBase implements ProjectManager {
    public static final Logger s_logger = Logger.getLogger(ProjectManagerImpl.class);
    private EmailInvite _emailInvite;

    @Inject
    private DomainDao _domainDao;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private ProjectJoinDao _projectJoinDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    private ProjectAccountJoinDao _projectAccountJoinDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ProjectInvitationDao _projectInvitationDao;
    @Inject
    private ProjectInvitationJoinDao _projectInvitationJoinDao;
    @Inject
    protected ResourceTagDao _resourceTagDao;
    @Inject
    private ProjectRoleDao projectRoleDao;
    @Inject
    private UserDao userDao;

    protected boolean _invitationRequired = false;
    protected long _invitationTimeOut = 86400000;
    protected boolean _allowUserToCreateProject = true;
    protected ScheduledExecutorService _executor;
    protected int _projectCleanupExpInvInterval = 60; //Interval defining how often project invitation cleanup thread is running

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        Map<String, String> configs = _configDao.getConfiguration(params);
        _invitationRequired = Boolean.valueOf(configs.get(Config.ProjectInviteRequired.key()));

        String value = configs.get(Config.ProjectInvitationExpirationTime.key());
        _invitationTimeOut = Long.parseLong(value != null ? value : "86400") * 1000;
        _allowUserToCreateProject = Boolean.valueOf(configs.get(Config.AllowUserToCreateProject.key()));

        // set up the email system for project invitations

        String smtpHost = configs.get("project.smtp.host");
        int smtpPort = NumbersUtil.parseInt(configs.get("project.smtp.port"), 25);
        String useAuthStr = configs.get("project.smtp.useAuth");
        boolean useAuth = ((useAuthStr == null) ? false : Boolean.parseBoolean(useAuthStr));
        String smtpUsername = configs.get("project.smtp.username");
        String smtpPassword = configs.get("project.smtp.password");
        String emailSender = configs.get("project.email.sender");
        String smtpDebugStr = configs.get("project.smtp.debug");
        boolean smtpDebug = false;
        if (smtpDebugStr != null) {
            smtpDebug = Boolean.parseBoolean(smtpDebugStr);
        }

        _emailInvite = new EmailInvite(smtpHost, smtpPort, useAuth, smtpUsername, smtpPassword, emailSender, smtpDebug);
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Project-ExpireInvitations"));

        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new ExpiredInvitationsCleanup(), _projectCleanupExpInvInterval, _projectCleanupExpInvInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private User validateUser(Long userId, Long accountId, Long domainId) {
        User user = null;
        if (userId != null) {
            user = userDao.findById(userId);
            if (user == null ) {
                throw new InvalidParameterValueException("Invalid user ID provided");
            }
            if (user.getAccountId() != accountId || _accountDao.findById(user.getAccountId()).getDomainId() != domainId) {
                throw new InvalidParameterValueException("User doesn't belong to the specified account or domain");
            }
        }
        return user;
    }

    private User validateUser(Long userId, Long domainId) {
        User user = null;
        if (userId != null) {
            user = userDao.findById(userId);
            if (user == null) {
                throw new InvalidParameterValueException("Invalid user ID provided");
            }
            if (_accountDao.findById(user.getAccountId()).getDomainId() != domainId) {
                throw new InvalidParameterValueException("User doesn't belong to the specified account or domain");
            }
        }
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_CREATE, eventDescription = "creating project", create = true)
    @DB
    public Project createProject(final String name, final String displayText, String accountName, final Long domainId, final Long userId, final Long accountId) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = caller;

        //check if the user authorized to create the project
        if (_accountMgr.isNormalUser(caller.getId()) && !_allowUserToCreateProject) {
            throw new PermissionDeniedException("Regular user is not permitted to create a project");
        }

        //Verify request parameters
        if ((accountName != null && domainId == null) || (domainId != null && accountName == null)) {
            throw new InvalidParameterValueException("Account name and domain id must be specified together");
        }

        if (userId != null && (accountId == null && domainId == null)) {
            throw new InvalidParameterValueException("Domain ID and account ID must be provided with User ID");
        }

        if (accountName != null) {
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
        }

        //don't allow 2 projects with the same name inside the same domain
        if (_projectDao.findByNameAndDomain(name, owner.getDomainId()) != null) {
            throw new InvalidParameterValueException("Project with name " + name + " already exists in domain id=" + owner.getDomainId());
        }

        User user = validateUser(userId, accountId, domainId);
        if (user != null) {
            owner = _accountDao.findById(user.getAccountId());
        }

        //do resource limit check
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.project);

        final Account ownerFinal = owner;
        User finalUser = user;
        return Transaction.execute(new TransactionCallback<Project>() {
            @Override
            public Project doInTransaction(TransactionStatus status) {

                //Create an account associated with the project
                StringBuilder acctNm = new StringBuilder("PrjAcct-");
                acctNm.append(name).append("-").append(ownerFinal.getDomainId());

                Account projectAccount = _accountMgr.createAccount(acctNm.toString(), Account.ACCOUNT_TYPE_PROJECT, null, domainId, null, null, UUID.randomUUID().toString());

                Project project = _projectDao.persist(new ProjectVO(name, displayText, ownerFinal.getDomainId(), projectAccount.getId()));

                //assign owner to the project
                assignAccountToProject(project, ownerFinal.getId(), ProjectAccount.Role.Admin,
                        Optional.ofNullable(finalUser).map(User::getId).orElse(null),  null);

        if (project != null) {
            CallContext.current().setEventDetails("Project id=" + project.getId());
            CallContext.current().putContextParameter(Project.class, project.getUuid());
        }

        //Increment resource count
                _resourceLimitMgr.incrementResourceCount(ownerFinal.getId(), ResourceType.project);

        return project;
    }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_CREATE, eventDescription = "creating project", async = true)
    @DB
    public Project enableProject(long projectId) {
        Account caller = CallContext.current().getCallingAccount();

        ProjectVO project = getProject(projectId);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + projectId);
        }

        CallContext.current().setProject(project);
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        //at this point enabling project doesn't require anything, so just update the state
        project.setState(State.Active);
        _projectDao.update(projectId, project);

        return project;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_DELETE, eventDescription = "deleting project", async = true)
    public boolean deleteProject(long projectId) {
        CallContext ctx = CallContext.current();

        ProjectVO project = getProject(projectId);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + projectId);
        }

        CallContext.current().setProject(project);
        _accountMgr.checkAccess(ctx.getCallingAccount(), AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        return deleteProject(ctx.getCallingAccount(), ctx.getCallingUserId(), project);
    }

    @DB
    @Override
    public boolean deleteProject(Account caller, long callerUserId, final ProjectVO project) {
        //mark project as inactive first, so you can't add resources to it
        boolean updateResult = Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
        s_logger.debug("Marking project id=" + project.getId() + " with state " + State.Disabled + " as a part of project delete...");
        project.setState(State.Disabled);
        boolean updateResult = _projectDao.update(project.getId(), project);
        //owner can be already removed at this point, so adding the conditional check
        List<Long> projectOwners = getProjectOwners(project.getId());
        if (projectOwners != null) {
            for (Long projectOwner : projectOwners)
            _resourceLimitMgr.decrementResourceCount(projectOwner, ResourceType.project);
        }
                return updateResult;
            }
        });

        if (updateResult) {
            //pass system caller when clenaup projects account
            if (!cleanupProject(project, _accountDao.findById(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM)) {
                s_logger.warn("Failed to cleanup project's id=" + project.getId() + " resources, not removing the project yet");
                return false;
            } else {
                return _projectDao.remove(project.getId());
            }
        } else {
            s_logger.warn("Failed to mark the project id=" + project.getId() + " with state " + State.Disabled);
            return false;
        }
    }

    @DB
    private boolean cleanupProject(final Project project, AccountVO caller, Long callerUserId) {
        boolean result = true;
        //Delete project's account
        AccountVO account = _accountDao.findById(project.getProjectAccountId());
        s_logger.debug("Deleting projects " + project + " internal account id=" + account.getId() + " as a part of project cleanup...");

        result = result && _accountMgr.deleteAccount(account, callerUserId, caller);

        if (result) {
            //Unassign all users from the project
            result = Transaction.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    boolean result = true;
            s_logger.debug("Unassigning all accounts from project " + project + " as a part of project cleanup...");
            List<? extends ProjectAccount> projectAccounts = _projectAccountDao.listByProjectId(project.getId());
            for (ProjectAccount projectAccount : projectAccounts) {
                result = result && unassignAccountFromProject(projectAccount.getProjectId(), projectAccount.getAccountId());
            }

            s_logger.debug("Removing all invitations for the project " + project + " as a part of project cleanup...");
            _projectInvitationDao.cleanupInvitations(project.getId());
                    return result;
                }
            });
            if (result) {
                s_logger.debug("Accounts are unassign successfully from project " + project + " as a part of project cleanup...");
            }
        } else {
            s_logger.warn("Failed to cleanup project's internal account");
        }

        return result;
    }

    @Override
    public boolean unassignAccountFromProject(long projectId, long accountId) {
        ProjectAccountVO projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, accountId);
        if (projectAccount == null) {
            s_logger.debug("Account id=" + accountId + " is not assigned to project id=" + projectId + " so no need to unassign");
            return true;
        }

        if (_projectAccountDao.remove(projectAccount.getId())) {
            return true;
        } else {
            s_logger.warn("Failed to unassign account id=" + accountId + " from the project id=" + projectId);
            return false;
        }
    }

    @Override
    public ProjectVO getProject(long projectId) {
        return _projectDao.findById(projectId);
    }

    @Override
    public long getInvitationTimeout() {
        return _invitationTimeOut;
    }

    @Override
    public ProjectAccount assignAccountToProject(Project project, long accountId, ProjectAccount.Role accountRole, Long userId, Long projectRoleId) {
        ProjectAccountVO projectAccountVO = new ProjectAccountVO(project, accountId, accountRole, userId, projectRoleId);
        return _projectAccountDao.persist(projectAccountVO);
    }

    public ProjectAccount assignUserToProject(Project project, long userId, long accountId, Role userRole, Long projectRoleId) {
       return assignAccountToProject(project, accountId, userRole, userId, projectRoleId);
    }

    @Override
    @DB
    public boolean deleteAccountFromProject(final long projectId, final long accountId) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
        boolean success = true;

        //remove account
        ProjectAccountVO projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, accountId);
        success = _projectAccountDao.remove(projectAccount.getId());

        //remove all invitations for account
        if (success) {
            s_logger.debug("Removed account " + accountId + " from project " + projectId + " , cleaning up old invitations for account/project...");
            ProjectInvitation invite = _projectInvitationDao.findByAccountIdProjectId(accountId, projectId);
            if (invite != null) {
                success = success && _projectInvitationDao.remove(invite.getId());
            }
        }

        return success;
    }
        });
    }

    @Override
    public Account getProjectOwner(long projectId) {
        ProjectAccount prAcct = _projectAccountDao.getProjectOwner(projectId);
        if (prAcct != null) {
            return _accountMgr.getAccount(prAcct.getAccountId());
        }

        return null;
    }

    @Override
    public List<Long> getProjectOwners(long projectId) {
        List<? extends ProjectAccount> projectAccounts = _projectAccountDao.getProjectOwners(projectId);
        if (projectAccounts != null || !projectAccounts.isEmpty()) {
            return projectAccounts.stream().map(acc -> _accountMgr.getAccount(acc.getAccountId()).getId()).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public ProjectVO findByProjectAccountId(long projectAccountId) {
        return _projectDao.findByProjectAccountId(projectAccountId);
    }

    @Override
    public ProjectVO findByProjectAccountIdIncludingRemoved(long projectAccountId) {
        return _projectDao.findByProjectAccountIdIncludingRemoved(projectAccountId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_USER_ADD, eventDescription = "adding user to project", async = true)
    public boolean addUserToProject(Long projectId, String username, String email, Long projectRoleId, Role projectRole) {
        Account caller = CallContext.current().getCallingAccount();

        Project project = getProject(projectId);
        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        if (project.getState() != State.Active) {
            InvalidParameterValueException ex =
                    new InvalidParameterValueException("Can't add user to the specified project id in state=" + project.getState() + " as it isn't currently active");
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }


        User user = userDao.getUserByName(username, project.getDomainId());
        if (user == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Invalid user ID provided");
            ex.addProxyObject(String.valueOf(username), "userId");
            throw ex;
        }

        CallContext.current().setProject(project);
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        Account userAccount = _accountDao.findById(user.getAccountId());
        if (_projectAccountDao.findByProjectIdAccountId(projectId, userAccount.getAccountId()) != null) {
            throw new InvalidParameterValueException("User belongs to account " + userAccount.getAccountId() + " which is already part of the project");
        }

        ProjectAccount projectAccountUser = _projectAccountDao.findByProjectIdUserId(projectId, user.getAccountId(), user.getId());
        if (projectAccountUser != null) {
            s_logger.info("User with id: " + user.getId() + " is already added to the project with id: " + projectId);
            return true;
        }

        if (projectRoleId != null && projectRoleId < 1L) {
            throw new InvalidParameterValueException("Invalid project role id provided");
        }

        ProjectRole role = null;
        if (projectRoleId != null) {
            role = projectRoleDao.findById(projectRoleId);
            if (role == null || !role.getProjectId().equals(projectId)) {
                throw new InvalidParameterValueException("Invalid project role ID for the given project");
            }
        }

        if (_invitationRequired) {
            return inviteUserToProject(project, user, email, projectRole, role);
        } else {
            if (username == null) {
                throw new InvalidParameterValueException("User information (ID) is required to add user to the project");
            }
            if (assignUserToProject(project, user.getId(), user.getAccountId(), projectRole,
                    Optional.ofNullable(role).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            }
            s_logger.warn("Failed to add user to project with id: " + projectId);
            return false;
        }
    }

    @Override
    public Project findByNameAndDomainId(String name, long domainId) {
        return _projectDao.findByNameAndDomain(name, domainId);
    }

    @Override
    public boolean canAccessProjectAccount(Account caller, long accountId) {
        //ROOT admin always can access the project
        if (_accountMgr.isRootAdmin(caller.getId())) {
            return true;
        } else if (_accountMgr.isDomainAdmin(caller.getId())) {
            Account owner = _accountMgr.getAccount(accountId);
            _accountMgr.checkAccess(caller, _domainDao.findById(owner.getDomainId()));
            return true;
        }
        User user = CallContext.current().getCallingUser();
        ProjectVO project = _projectDao.findByProjectAccountId(accountId);
        if (project != null) {
            ProjectAccount userProjectAccount = _projectAccountDao.findByProjectIdUserId(project.getId(), user.getAccountId(), user.getId());
            if (userProjectAccount != null) {
                return _projectAccountDao.canUserAccessProjectAccount(user.getAccountId(), user.getId(), accountId);
            }
        }
        return _projectAccountDao.canAccessProjectAccount(caller.getId(), accountId);
    }

    @Override
    public boolean canModifyProjectAccount(Account caller, long accountId) {
        //ROOT admin always can access the project
        if (_accountMgr.isRootAdmin(caller.getId())) {
            return true;
        } else if (_accountMgr.isDomainAdmin(caller.getId())) {
            Account owner = _accountMgr.getAccount(accountId);
            _accountMgr.checkAccess(caller, _domainDao.findById(owner.getDomainId()));
            return true;
        }

        User user = CallContext.current().getCallingUser();
        Project project = CallContext.current().getProject();
        if (project != null) {
            ProjectAccountVO projectUser = _projectAccountDao.findByProjectIdUserId(project.getId(), caller.getAccountId(), user.getId());
            if (projectUser != null) {
                return _projectAccountDao.canUserModifyProject(project.getId(), caller.getAccountId(), user.getId());
            }
        }
        return _projectAccountDao.canModifyProjectAccount(caller.getId(), accountId);
    }

    private void updateProjectAccount(ProjectAccountVO futureOwner, Role newAccRole, Long accountId) throws ResourceAllocationException {
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(accountId), ResourceType.project);
        futureOwner.setAccountRole(newAccRole);
        _projectAccountDao.update(futureOwner.getId(), futureOwner);
        if (newAccRole != null && Role.Admin == newAccRole) {
            _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.project);
        } else {
            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.project);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_UPDATE, eventDescription = "updating project", async = true)
    public Project updateProject(final long projectId, final String displayText, final String newOwnerName) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        //check that the project exists
        final ProjectVO project = getProject(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }

        //verify permissions
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<ResourceAllocationException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws ResourceAllocationException {
                if (displayText != null) {
                    project.setDisplayText(displayText);
                    _projectDao.update(projectId, project);
                }

                if (newOwnerName != null) {
                    //check that the new owner exists
                    Account futureOwnerAccount = _accountMgr.getActiveAccountByName(newOwnerName, project.getDomainId());
                    if (futureOwnerAccount == null) {
                        throw new InvalidParameterValueException("Unable to find account name=" + newOwnerName + " in domain id=" + project.getDomainId());
                    }
                    Account currentOwnerAccount = getProjectOwner(projectId);
                    if (currentOwnerAccount == null) {
                        s_logger.error("Unable to find the current owner for the project id=" + projectId);
                        throw new InvalidParameterValueException("Unable to find the current owner for the project id=" + projectId);
                    }
                    if (currentOwnerAccount.getId() != futureOwnerAccount.getId()) {
                        ProjectAccountVO futureOwner = _projectAccountDao.findByProjectIdAccountId(projectId, futureOwnerAccount.getAccountId());
                        if (futureOwner == null) {
                            throw new InvalidParameterValueException("Account " + newOwnerName +
                                    " doesn't belong to the project. Add it to the project first and then change the project's ownership");
                        }

                        //do resource limit check
                        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(futureOwnerAccount.getId()), ResourceType.project);

                        //unset the role for the old owner
                        ProjectAccountVO currentOwner = _projectAccountDao.findByProjectIdAccountId(projectId, currentOwnerAccount.getId());
                        currentOwner.setAccountRole(Role.Regular);
                        _projectAccountDao.update(currentOwner.getId(), currentOwner);
                        _resourceLimitMgr.decrementResourceCount(currentOwnerAccount.getId(), ResourceType.project);

                        //set new owner
                        futureOwner.setAccountRole(Role.Admin);
                        _projectAccountDao.update(futureOwner.getId(), futureOwner);
                        _resourceLimitMgr.incrementResourceCount(futureOwnerAccount.getId(), ResourceType.project);

                    } else {
                        s_logger.trace("Future owner " + newOwnerName + "is already the owner of the project id=" + projectId);
                    }
                }
            }
        });

        return _projectDao.findById(projectId);

    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_UPDATE, eventDescription = "updating project", async = true)
    public Project updateProject(final long projectId, final String displayText, final String newOwnerName, Long userId,
                                 Role newRole) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        //check that the project exists
        final ProjectVO project = getProject(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }

        CallContext.current().setProject(project);
        //verify permissions
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        List<? extends  ProjectAccount> projectOwners = _projectAccountDao.getProjectOwners(projectId);

        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<ResourceAllocationException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws ResourceAllocationException {
                if (displayText != null) {
                    project.setDisplayText(displayText);
                    _projectDao.update(projectId, project);
                }
                if (newOwnerName != null) {
                    //check that the new owner exists
                    Account updatedAcc = _accountMgr.getActiveAccountByName(newOwnerName, project.getDomainId());
                    if (updatedAcc == null) {
                        throw new InvalidParameterValueException("Unable to find account name=" + newOwnerName + " in domain id=" + project.getDomainId());
                    }
                    ProjectAccountVO newProjectAcc = _projectAccountDao.findByProjectIdAccountId(projectId, updatedAcc.getAccountId());
                    if (newProjectAcc == null) {
                        throw new InvalidParameterValueException("Account " + newOwnerName +
                                " doesn't belong to the project. Add it to the project first and then change the project's ownership");
                    }

                    if (isTheOnlyProjectOwner(projectId, newProjectAcc, caller) && newRole != Role.Admin) {
                        throw new InvalidParameterValueException("Cannot demote the only admin of the project");
                    }
                    updateProjectAccount(newProjectAcc, newRole, updatedAcc.getId());
                } else if (userId != null) {
                    User user = validateUser(userId, project.getDomainId());
                    if (user == null) {
                        throw new InvalidParameterValueException("Unable to find user= " + user.getUsername() + " in domain id = " + project.getDomainId());
                    }
                    ProjectAccountVO newProjectUser = _projectAccountDao.findByProjectIdUserId(projectId, user.getAccountId(), userId);
                    if (newProjectUser == null) {
                        throw new InvalidParameterValueException("User " + userId +
                                " doesn't belong to the project. Add it to the project first and then change the project's ownership");
                    }

                    if (projectOwners.size() == 1 && newProjectUser.getUserId().equals(projectOwners.get(0).getUserId())
                            && newRole != Role.Admin ) {
                        throw new InvalidParameterValueException("Cannot demote the only admin of the project");
                    }

                    updateProjectAccount(newProjectUser, newRole, user.getAccountId());

                }
            }
        });
        return _projectDao.findById(projectId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ACCOUNT_ADD, eventDescription = "adding account to project", async = true)
    public boolean addAccountToProject(long projectId, String accountName, String email, Long projectRoleId, Role projectRoleType) {
        Account caller = CallContext.current().getCallingAccount();

        //check that the project exists
        Project project = getProject(projectId);

        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        //User can be added to Active project only
        if (project.getState() != Project.State.Active) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("Can't add account to the specified project id in state=" + project.getState() + " as it's no longer active");
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }

        //check that account-to-add exists
        Account account = null;
        if (accountName != null) {
            account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
            if (account == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find account name=" + accountName + " in specified domain id");
                DomainVO domain = ApiDBUtils.findDomainById(project.getDomainId());
                String domainUuid = String.valueOf(project.getDomainId());
                if (domain != null) {
                    domainUuid = domain.getUuid();
                }
                ex.addProxyObject(domainUuid, "domainId");
                throw ex;
            }

            CallContext.current().setProject(project);
            //verify permissions - only project owner can assign
            _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

            //Check if the account already added to the project
            ProjectAccount projectAccount =  _projectAccountDao.findByProjectIdAccountId(projectId, account.getId());
            if (projectAccount != null) {
                s_logger.debug("Account " + accountName + " already added to the project id=" + projectId);
                return true;
            }
        }

        if (projectRoleId != null && projectRoleId < 1L) {
            throw new InvalidParameterValueException("Invalid project role id provided");
        }

        ProjectRole projectRole = null;
        if (projectRoleId != null) {
            projectRole = projectRoleDao.findById(projectRoleId);
            if (projectRole == null || projectRole.getProjectId() != projectId) {
                throw new InvalidParameterValueException("Invalid project role ID for the given project");
            }
        }

        if (_invitationRequired) {
            return inviteAccountToProject(project, account, email, projectRoleType, projectRole);
        } else {
            if (account == null) {
                throw new InvalidParameterValueException("Account information is required for assigning account to the project");
            }
            if (assignAccountToProject(project, account.getId(), projectRoleType, null,
                    Optional.ofNullable(projectRole).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            } else {
                s_logger.warn("Failed to add account " + accountName + " to project id=" + projectId);
                return false;
            }
        }
    }

    private boolean inviteAccountToProject(Project project, Account account, String email, Role role,ProjectRole projectRole) {
        if (account != null) {
            if (createAccountInvitation(project, account.getId(), null, role,
                    Optional.ofNullable(projectRole).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            } else {
                s_logger.warn("Failed to generate invitation for account " + account.getAccountName() + " to project id=" + project);
                return false;
            }
        }

        if (email != null) {
            //generate the token
            String token = generateToken(10);
            if (generateTokenBasedInvitation(project, null, email, token, role,
                    Optional.ofNullable(projectRole).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            } else {
                s_logger.warn("Failed to generate invitation for email " + email + " to project id=" + project);
                return false;
            }
        }

        return false;
    }

    private boolean inviteUserToProject(Project project, User user, String email, Role role, ProjectRole projectRole) {
        if (email == null) {
            if (createAccountInvitation(project, user.getAccountId(), user.getId(), role,
                    Optional.ofNullable(projectRole).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            } else {
                s_logger.warn("Failed to generate invitation for account " + user.getUsername()  + " to project id=" + project);
                return false;
            }
        } else {
            //generate the token
            String token = generateToken(10);
            if (generateTokenBasedInvitation(project, user.getId(), email, token, role,
                    Optional.ofNullable(projectRole).map(ProjectRole::getId).orElse(null)) != null) {
                return true;
            } else {
                s_logger.warn("Failed to generate invitation for email " + email + " to project id=" + project);
                return false;
            }
        }
    }

    private boolean isTheOnlyProjectOwner(Long projectId, ProjectAccount projectAccount, Account caller) {
        List<? extends  ProjectAccount> projectOwners = _projectAccountDao.getProjectOwners(projectId);
        if ((projectOwners.size() == 1 && projectOwners.get(0).getAccountId() == projectAccount.getAccountId()
                && projectAccount.getAccountRole() == Role.Admin )) {
            return true;
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ACCOUNT_REMOVE, eventDescription = "removing account from project", async = true)
    public boolean deleteAccountFromProject(long projectId, String accountName) {
        Account caller = CallContext.current().getCallingAccount();

        //check that the project exists
        Project project = getProject(projectId);

        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        //check that account-to-remove exists
        Account account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
        if (account == null) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("Unable to find account name=" + accountName + " in domain id=" + project.getDomainId());
            DomainVO domain = ApiDBUtils.findDomainById(project.getDomainId());
            String domainUuid = String.valueOf(project.getDomainId());
            if (domain != null) {
                domainUuid = domain.getUuid();
            }
            ex.addProxyObject(domainUuid, "domainId");
            throw ex;
        }

        CallContext.current().setProject(project);
        //verify permissions
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        //Check if the account exists in the project
        ProjectAccount projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, account.getId());
        if (projectAccount == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Account " + accountName + " is not assigned to the project with specified id");
            // Use the projectVO object and not the projectAccount object to inject the projectId.
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }

        //can't remove the owner of the project
        if (isTheOnlyProjectOwner(projectId, projectAccount, caller)) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("Unable to delete account " + accountName +
                    " from the project with specified id as the account is the owner of the project");
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }

        return deleteAccountFromProject(projectId, account.getId());
    }

    @Override
    public boolean deleteUserFromProject(long projectId, long userId) {
        Account caller = CallContext.current().getCallingAccount();
        //check that the project exists
        Project project = getProject(projectId);

        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        User user = userDao.findById(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Invalid userId provided");
        }
        Account userAcc = _accountDao.findActiveAccountById(user.getAccountId(), project.getDomainId());
        if (userAcc == null) {
            InvalidParameterValueException ex =
                    new InvalidParameterValueException("Unable to find user "+ user.getUsername() + " in domain id=" + project.getDomainId());
            DomainVO domain = ApiDBUtils.findDomainById(project.getDomainId());
            String domainUuid = String.valueOf(project.getDomainId());
            if (domain != null) {
                domainUuid = domain.getUuid();
            }
            ex.addProxyObject(domainUuid, "domainId");
            throw ex;
        }

        CallContext.current().setProject(project);
        //verify permissions
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        //Check if the user exists in the project
        ProjectAccount projectUser =  _projectAccountDao.findByProjectIdUserId(projectId, user.getAccountId(), user.getId());
        if (projectUser == null) {
            deletePendingInvite(projectId, user);
            InvalidParameterValueException ex = new InvalidParameterValueException("User " + user.getUsername() + " is not assigned to the project with specified id");
            // Use the projectVO object and not the projectAccount object to inject the projectId.
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }
        return deleteUserFromProject(projectId, user);
    }

    private void deletePendingInvite(Long projectId, User user) {
        ProjectInvitation invite = _projectInvitationDao.findByUserIdProjectId(user.getId(), user.getAccountId(),  projectId);
        if (invite != null) {
            boolean success = _projectInvitationDao.remove(invite.getId());
            if (success){
                s_logger.info("Successfully deleted invite pending for the user : "+user.getUsername());
            } else {
                s_logger.info("Failed to delete project invite for user: "+ user.getUsername());
            }
        }
    }

    @DB
    private boolean deleteUserFromProject(Long projectId, User user) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = true;
                ProjectAccountVO projectAccount = _projectAccountDao.findByProjectIdUserId(projectId, user.getAccountId(), user.getId());
                success = _projectAccountDao.remove(projectAccount.getId());

                if (success) {
                    s_logger.debug("Removed user " + user.getId() + " from project. Removing any invite sent to the user");
                    ProjectInvitation invite = _projectInvitationDao.findByUserIdProjectId(user.getId(), user.getAccountId(),  projectId);
                    if (invite != null) {
                        success = success && _projectInvitationDao.remove(invite.getId());
                    }
                }
                return success;
            }
        });
    }

    public ProjectInvitation createAccountInvitation(Project project, Long accountId, Long userId, Role role, Long projectRoleId) {
        if (activeInviteExists(project, accountId, userId, null)) {
            throw new InvalidParameterValueException("There is already a pending invitation for account id=" + accountId + " to the project id=" + project);
        }

        ProjectInvitationVO invitationVO = new ProjectInvitationVO(project.getId(), accountId, project.getDomainId(), null, null);
        if (userId != null) {
            invitationVO.setForUserId(userId);
        }
        if (role != null) {
            invitationVO.setAccountRole(role);
        }
        if (projectRoleId != null) {
            invitationVO.setProjectRoleId(projectRoleId);
        }
        return  _projectInvitationDao.persist(invitationVO);
    }

    @DB
    public boolean activeInviteExists(final Project project, final Long accountId, Long userId, final String email) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
        //verify if the invitation was already generated
        ProjectInvitationVO invite = null;
        if (accountId != null) {
            invite = _projectInvitationDao.findByAccountIdProjectId(accountId, project.getId());
        } else if (userId != null) {
            invite = _projectInvitationDao.findByUserIdProjectId(userId, accountId, project.getId());
        } else if (email != null) {
            invite = _projectInvitationDao.findByEmailAndProjectId(email, project.getId());
        }

        if (invite != null) {
            if (invite.getState() == ProjectInvitation.State.Completed ||
                    (invite.getState() == ProjectInvitation.State.Pending && _projectInvitationDao.isActive(invite.getId(), _invitationTimeOut))) {
                return true;
            } else {
                if (invite.getState() == ProjectInvitation.State.Pending) {
                    expireInvitation(invite);
                }
                //remove the expired/declined invitation
                if (accountId != null) {
                    s_logger.debug("Removing invitation in state " + invite.getState() + " for account id=" + accountId + " to project " + project);
                } else if (userId != null) {
                    s_logger.debug("Removing invitation in state " + invite.getState() + " for user id=" + userId + " to project " + project);
                } else if (email != null) {
                    s_logger.debug("Removing invitation in state " + invite.getState() + " for email " + email + " to project " + project);
                }

                _projectInvitationDao.expunge(invite.getId());
            }
        }

        return false;
    }
        });
    }

    public ProjectInvitation generateTokenBasedInvitation(Project project, Long userId, String email, String token, Role role, Long projectRoleId) {
        //verify if the invitation was already generated
        if (activeInviteExists(project, null, null, email)) {
            throw new InvalidParameterValueException("There is already a pending invitation for email " + email + " to the project id=" + project);
        }

        ProjectInvitationVO projectInvitationVO = new ProjectInvitationVO(project.getId(), null, project.getDomainId(), email, token);
        if (userId != null) {
            projectInvitationVO.setForUserId(userId);
        }
        if (role != null) {
            projectInvitationVO.setAccountRole(role);
        }
        if (projectRoleId != null) {
            projectInvitationVO.setProjectRoleId(projectRoleId);
        }

        ProjectInvitation projectInvitation = _projectInvitationDao.persist(projectInvitationVO);
        try {
            _emailInvite.sendInvite(token, email, project.getId());
        } catch (Exception ex) {
            s_logger.warn("Failed to send project id=" + project + " invitation to the email " + email + "; removing the invitation record from the db", ex);
            _projectInvitationDao.remove(projectInvitation.getId());
            return null;
        }

        return projectInvitation;
    }

    private boolean expireInvitation(ProjectInvitationVO invite) {
        s_logger.debug("Expiring invitation id=" + invite.getId());
        invite.setState(ProjectInvitation.State.Expired);
        return _projectInvitationDao.update(invite.getId(), invite);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_INVITATION_UPDATE, eventDescription = "updating project invitation", async = true)
    public boolean updateInvitation(final long projectId, String accountName, Long userId, String token, final boolean accept) {
        Account caller = CallContext.current().getCallingAccount();
        Long accountId = null;
        User user = null;
        boolean result = true;

        //check that the project exists
        final Project project = getProject(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
        CallContext.current().setProject(project);
        if (accountName != null) {
            //check that account-to-remove exists
            Account account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account name=" + accountName + " in domain id=" + project.getDomainId());
            }

            //verify permissions
            _accountMgr.checkAccess(caller, null, true, account);
            accountId = account.getId();
        } else if (userId != null) {
            user = userDao.findById(userId);
            if (user == null) {
                throw new InvalidParameterValueException("Invalid user ID provided. Please provide a valid user ID or " +
                        "account name whose invitation is to be updated");
            }
            Account userAccount = _accountDao.findById(user.getAccountId());
            if (userAccount.getDomainId() != project.getDomainId()) {
                throw new InvalidParameterValueException("Unable to find user =" + userId + " in domain id=" + project.getDomainId());
            }
        } else {
            accountId = caller.getId();
            user = CallContext.current().getCallingUser();
        }

        //check that invitation exists
        ProjectInvitationVO invite = null;
        if (token == null) {
            if (accountName != null) {
                invite = _projectInvitationDao.findByAccountIdProjectId(accountId, projectId, ProjectInvitation.State.Pending);
            } else {
                invite = _projectInvitationDao.findByUserIdProjectId(user.getId(), user.getAccountId(), projectId, ProjectInvitation.State.Pending);
            }
        } else {
            invite = _projectInvitationDao.findPendingByTokenAndProjectId(token, projectId, ProjectInvitation.State.Pending);
        }

        if (invite != null) {
            if (!_projectInvitationDao.isActive(invite.getId(), _invitationTimeOut) && accept) {
                expireInvitation(invite);
                throw new InvalidParameterValueException("Invitation is expired for account id=" + accountName + " to the project id=" + projectId);
            } else {
                final ProjectInvitationVO inviteFinal = invite;
                final Long accountIdFinal = accountId;
                final String accountNameFinal = accountName;
                final User finalUser = user;
                ProjectInvitationVO finalInvite = invite;
                result = Transaction.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        boolean result = true;

                ProjectInvitation.State newState = accept ? ProjectInvitation.State.Completed : ProjectInvitation.State.Declined;

                //update invitation
                s_logger.debug("Marking invitation " + inviteFinal + " with state " + newState);
                inviteFinal.setState(newState);
                result = _projectInvitationDao.update(inviteFinal.getId(), inviteFinal);

                if (result && accept) {
                    //check if account already exists for the project (was added before invitation got accepted)
                    if (finalInvite.getForUserId() == -1) {
                        ProjectAccount projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, accountIdFinal);
                        if (projectAccount != null) {
                            s_logger.debug("Account " + accountNameFinal + " already added to the project id=" + projectId);
                        } else {
                            assignAccountToProject(project, accountIdFinal, finalInvite.getAccountRole(), null, finalInvite.getProjectRoleId());
                        }
                    } else {
                        ProjectAccount projectAccount = _projectAccountDao.findByProjectIdUserId(projectId, finalUser.getAccountId(), finalUser.getId());
                        if (projectAccount != null) {
                            s_logger.debug("User " + finalUser.getId() + "has already been added to the project id=" + projectId);
                        } else {
                            assignUserToProject(project, finalInvite.getForUserId(), finalUser.getAccountId(), finalInvite.getAccountRole(), finalInvite.getProjectRoleId());
                        }
                    }
                } else {
                    s_logger.warn("Failed to update project invitation " + inviteFinal + " with state " + newState);
                }
                return result;
             }});
            }
        } else {
            throw new InvalidParameterValueException("Unable to find invitation for account name=" + accountName + " to the project id=" + projectId);
        }

        return result;
    }

    @Override
    public List<Long> listPermittedProjectAccounts(long accountId) {
        return _projectAccountDao.listPermittedAccountIds(accountId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ACTIVATE, eventDescription = "activating project")
    @DB
    public Project activateProject(final long projectId) {
        Account caller = CallContext.current().getCallingAccount();

        //check that the project exists
        final ProjectVO project = getProject(projectId);

        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        CallContext.current().setProject(project);
        //verify permissions
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        //allow project activation only when it's in Suspended state
        Project.State currentState = project.getState();

        if (currentState == State.Active) {
            s_logger.debug("The project id=" + projectId + " is already active, no need to activate it again");
            return project;
        }

        if (currentState != State.Suspended) {
            throw new InvalidParameterValueException("Can't activate the project in " + currentState + " state");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
        project.setState(Project.State.Active);
        _projectDao.update(projectId, project);

        _accountMgr.enableAccount(project.getProjectAccountId());
            }
        });

        return _projectDao.findById(projectId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_SUSPEND, eventDescription = "suspending project", async = true)
    public Project suspendProject(long projectId) throws ConcurrentOperationException, ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();

        ProjectVO project = getProject(projectId);
        //verify input parameters
        if (project == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified id");
            ex.addProxyObject(String.valueOf(projectId), "projectId");
            throw ex;
        }

        CallContext.current().setProject(project);
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        if (suspendProject(project)) {
            s_logger.debug("Successfully suspended project id=" + projectId);
            return _projectDao.findById(projectId);
        } else {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to suspend project with specified id");
            ex.addProxyObject(project.getUuid(), "projectId");
            throw ex;
        }

    }

    private boolean suspendProject(ProjectVO project) throws ConcurrentOperationException, ResourceUnavailableException {

        s_logger.debug("Marking project " + project + " with state " + State.Suspended + " as a part of project suspend...");
        project.setState(State.Suspended);
        boolean updateResult = _projectDao.update(project.getId(), project);

        if (updateResult) {
            long projectAccountId = project.getProjectAccountId();
            if (!_accountMgr.disableAccount(projectAccountId)) {
                s_logger.warn("Failed to suspend all project's " + project + " resources; the resources will be suspended later by background thread");
            }
        } else {
            throw new CloudRuntimeException("Failed to mark the project " + project + " with state " + State.Suspended);
        }
        return true;
    }

    public static String generateToken(int length) {
        String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rand = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int pos = rand.nextInt(charset.length());
            sb.append(charset.charAt(pos));
        }
        return sb.toString();
    }
    class EmailInvite {
        private Session _smtpSession;
        private final String _smtpHost;
        private int _smtpPort = -1;
        private boolean _smtpUseAuth = false;
        private final String _smtpUsername;
        private final String _smtpPassword;
        private final String _emailSender;

        public EmailInvite(String smtpHost, int smtpPort, boolean smtpUseAuth, final String smtpUsername, final String smtpPassword, String emailSender, boolean smtpDebug) {
            _smtpHost = smtpHost;
            _smtpPort = smtpPort;
            _smtpUseAuth = smtpUseAuth;
            _smtpUsername = smtpUsername;
            _smtpPassword = smtpPassword;
            _emailSender = emailSender;

            if (_smtpHost != null) {
                Properties smtpProps = new Properties();
                smtpProps.put("mail.smtp.host", smtpHost);
                smtpProps.put("mail.smtp.port", smtpPort);
                smtpProps.put("mail.smtp.auth", "" + smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtp.user", smtpUsername);
                }

                smtpProps.put("mail.smtps.host", smtpHost);
                smtpProps.put("mail.smtps.port", smtpPort);
                smtpProps.put("mail.smtps.auth", "" + smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtps.user", smtpUsername);
                }

                if ((smtpUsername != null) && (smtpPassword != null)) {
                    _smtpSession = Session.getInstance(smtpProps, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(smtpUsername, smtpPassword);
                        }
                    });
                } else {
                    _smtpSession = Session.getInstance(smtpProps);
                }
                _smtpSession.setDebug(smtpDebug);
            } else {
                _smtpSession = null;
            }
        }

        public void sendInvite(String token, String email, long projectId) throws MessagingException, UnsupportedEncodingException {
            if (_smtpSession != null) {
                InternetAddress address = null;
                if (email != null) {
                    try {
                        address = new InternetAddress(email, email);
                    } catch (Exception ex) {
                        s_logger.error("Exception creating address for: " + email, ex);
                    }
                }

                String content = "You've been invited to join the CloudStack project id=" + projectId + ". Please use token " + token + " to complete registration";

                SMTPMessage msg = new SMTPMessage(_smtpSession);
                msg.setSender(new InternetAddress(_emailSender, _emailSender));
                msg.setFrom(new InternetAddress(_emailSender, _emailSender));
                msg.addRecipient(RecipientType.TO, address);
                msg.setSubject("You are invited to join the cloud stack project id=" + projectId);
                msg.setSentDate(new Date(DateUtil.currentGMTTime().getTime() >> 10));
                msg.setContent(content, "text/plain");
                msg.saveChanges();

                SMTPTransport smtpTrans = null;
                if (_smtpUseAuth) {
                    smtpTrans = new SMTPSSLTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                } else {
                    smtpTrans = new SMTPTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                }
                smtpTrans.connect();
                smtpTrans.sendMessage(msg, msg.getAllRecipients());
                smtpTrans.close();
            } else {
                throw new CloudRuntimeException("Unable to send email invitation; smtp ses");
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_INVITATION_REMOVE, eventDescription = "removing project invitation", async = true)
    public boolean deleteProjectInvitation(long id) {
        Account caller = CallContext.current().getCallingAccount();

        ProjectInvitation invitation = _projectInvitationDao.findById(id);
        if (invitation == null) {
            throw new InvalidParameterValueException("Unable to find project invitation by id " + id);
        }

        //check that the project exists
        Project project = getProject(invitation.getProjectId());

        CallContext.current().setProject(project);
        //check permissions - only project owner can remove the invitations
        _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, _accountMgr.getAccount(project.getProjectAccountId()));

        if (_projectInvitationDao.remove(id)) {
            s_logger.debug("Project Invitation id=" + id + " is removed");
            return true;
        } else {
            s_logger.debug("Failed to remove project invitation id=" + id);
            return false;
        }
    }

    public class ExpiredInvitationsCleanup extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                TimeZone.getDefault();
                List<ProjectInvitationVO> invitationsToExpire = _projectInvitationDao.listInvitationsToExpire(_invitationTimeOut);
                if (!invitationsToExpire.isEmpty()) {
                    s_logger.debug("Found " + invitationsToExpire.size() + " projects to expire");
                    for (ProjectInvitationVO invitationToExpire : invitationsToExpire) {
                        invitationToExpire.setState(ProjectInvitation.State.Expired);
                        _projectInvitationDao.update(invitationToExpire.getId(), invitationToExpire);
                        s_logger.trace("Expired project invitation id=" + invitationToExpire.getId());
                    }
                }
            } catch (Exception ex) {
                s_logger.warn("Exception while running expired invitations cleanup", ex);
            }
        }
    }

    @Override
    public boolean projectInviteRequired() {
        return _invitationRequired;
    }

    @Override
    public boolean allowUserToCreateProject() {
        return _allowUserToCreateProject;
    }

}
